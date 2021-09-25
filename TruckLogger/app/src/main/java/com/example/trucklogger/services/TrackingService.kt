package com.example.trucklogger.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.trucklogger.db.TruckLog
import com.example.trucklogger.db.TruckLogDAO
import com.example.trucklogger.other.*
import com.example.trucklogger.other.Constants.ACTION_START_SERVICE
import com.example.trucklogger.other.Constants.ACTION_STOP_SERVICE
import com.example.trucklogger.other.Constants.ACTION_UPLOAD_FAIL
import com.example.trucklogger.other.Constants.ACTION_UPLOAD_LOGS
import com.example.trucklogger.other.Constants.ACTION_UPLOAD_SUCCESS
import com.example.trucklogger.other.Constants.FASTEST_LOCATION_UPDATE_INTERVAL
import com.example.trucklogger.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.trucklogger.other.Constants.MPS_TO_KMH
import com.example.trucklogger.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.trucklogger.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.trucklogger.other.Constants.NOTIFICATION_ID
import com.example.trucklogger.other.Constants.UPLOAD_CONTINUOUSLY
import com.example.trucklogger.other.Constants.UPLOAD_HOURLY
import com.example.trucklogger.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.net.ssl.SSLSocketFactory

@AndroidEntryPoint
class TrackingService : LifecycleService() {
    var started = false
    lateinit var sharedPreferences: SharedPreferences
    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var truckLogDao: TruckLogDAO

    @Inject
    lateinit var sslSocketFactory: SSLSocketFactory
    lateinit var serverConnector : ServerConnector

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder
    lateinit var currNotificationBuilder: NotificationCompat.Builder
    lateinit var notificationManager : NotificationManager

    var isTracking = false
    companion object {
        val isRunning = MutableLiveData<Boolean> ()
        val speed = MutableLiveData<Float> ()
        val lat = MutableLiveData<Float> ()
        val lng = MutableLiveData<Float> ()
        val logsStashed = MutableLiveData<Int> ()
    }

    //settings
    private var TRUCKER_ID : Int = 0;
    private var TRUCKER_UUID : String = "";
    private var TRUCKER_VERIFIED  : Boolean = false;
    private var UPLOAD_FREQUENCY  : Int = 0;

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        currNotificationBuilder = baseNotificationBuilder
        GlobalScope.launch(Dispatchers.IO) {
            serverConnector = ServerConnector(sslSocketFactory)
        }
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        updateSettings()
        speed.postValue(0.0f)
        lat.postValue(0.0f)
        lng.postValue(0.0f)
        logsStashed.postValue(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.postValue(false)
    }

    private fun updateSettings(){
        with (sharedPreferences) {
            TRUCKER_ID = getInt("ID", 0)
            TRUCKER_UUID = getString("UUID", "NONE").toString()
            TRUCKER_VERIFIED = getBoolean("VERIFIED", false)
            UPLOAD_FREQUENCY = getInt("UPLOAD_FREQUENCY", UPLOAD_CONTINUOUSLY)
        }
    }

   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
       intent?.let { it ->
           when (it.action) {
               ACTION_START_SERVICE -> {
                   if (!started) {
                       started = true
                       isTracking = true
                       isRunning.postValue(true)
                       startForegroundService()
                       updateTracking()
                   }
               }
               ACTION_STOP_SERVICE -> {
                   Timber.d("Stopped Tracking service")
                   isTracking = false
                   updateTracking()
                   stopForeground(true)
                   isRunning.postValue(false)
                   stopSelf()
               }
               ACTION_UPLOAD_LOGS -> {
                   if (isTracking) {
                       GlobalScope.launch(Dispatchers.IO) {
                           val action = if (uploadLogs() == "Up-to-date.") {
                               ACTION_UPLOAD_SUCCESS
                           } else {
                               ACTION_UPLOAD_FAIL
                           }

                           val i = Intent(this@TrackingService, MainActivity::class.java)
                           i.action = action
                           i.flags = FLAG_ACTIVITY_NEW_TASK
                           startActivity(i)
                       }
                   }
               }
           }
       }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService(){
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        Timber.d("starting notification")
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun updateTracking() {
        if (isTracking){
            if (TrackingUtility.hasLocationPermissions(this)) {
                val request = LocationRequest().apply {
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval  = FASTEST_LOCATION_UPDATE_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.let { locations ->
                for (location in locations) {
                    GlobalScope.launch(Dispatchers.IO) { processLocation(location) }
                }
            }
        }
    }

    private suspend fun processLocation(location : Location) {
        updateSettings()
        val truckLog = TruckLog(
            location.time/1000,
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.speed * MPS_TO_KMH,
            0f
        )
        speed.postValue(truckLog.spd)
        lat.postValue(truckLog.lat)
        lng.postValue(truckLog.lon)
        truckLogDao.insertTruckLog(truckLog)

        var count = truckLogDao.getTruckLogsCount()
        var statusUpload = ""

        if (TRUCKER_VERIFIED)
        {
            if ((UPLOAD_FREQUENCY == UPLOAD_CONTINUOUSLY) || (UPLOAD_FREQUENCY == UPLOAD_HOURLY && count > 10))
            {
                statusUpload = uploadLogs()
            }
        } else {
            statusUpload = "Unverified ID."
        }

        count = truckLogDao.getTruckLogsCount()
        logsStashed.postValue(count)

        //update notification
        val notif = "${String.format("%.0f",truckLog.spd)} km/h. $count log/s stashed. $statusUpload"
        val notification = currNotificationBuilder
            .setContentText(notif)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    private suspend fun uploadLogs() : String {
        var statusUpload: String
        var count: Int
        var resultCode: ServerResponseCode
        //send all logs until none left
        do {
            var logs = truckLogDao.getAllTruckLogs()
            var serverRequest = ServerRequest(TRUCKER_ID,TRUCKER_UUID, ServerRequestCode.REQUEST_UPDATE_LOGS.value, logs)
            var result = serverConnector.sendMessage(serverRequest)
            resultCode = ServerResponseCode.fromInt(result.res)
            if (resultCode == ServerResponseCode.RESPONSE_OK) {
                for (log in logs) {
                    truckLogDao.deleteTruckLog(log)
                }
            }
            count = truckLogDao.getTruckLogsCount()
        } while ((count > 0) && (resultCode == ServerResponseCode.RESPONSE_OK))

        when (resultCode) {
            ServerResponseCode.RESPONSE_INVALID_CREDENTIALS -> {
                with(sharedPreferences.edit()) {
                    putBoolean("VERIFIED", false)
                    apply()
                }
                statusUpload = "Unverified ID."
            }

            ServerResponseCode.RESPONSE_OK -> {
                statusUpload = "Up-to-date."
            }

            ServerResponseCode.RESPONSE_TIMEOUT -> {
                statusUpload = "No server connection."
            }

            ServerResponseCode.RESPONSE_FAIL -> {
                statusUpload = "Invalid Request."
            }

            ServerResponseCode.RESPONSE_DB_CONN_FAIL, ServerResponseCode.RESPONSE_PARSE_FAIL -> {
                statusUpload = "Serverside Error."
            }

            else -> {
                statusUpload = "Unknown Issue."
            }
        }
        return statusUpload
    }
}
