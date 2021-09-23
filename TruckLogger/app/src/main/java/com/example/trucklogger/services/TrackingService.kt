package com.example.trucklogger.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.Looper
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.trucklogger.R
import com.example.trucklogger.db.TruckLog
import com.example.trucklogger.db.TruckLogDAO
import com.example.trucklogger.other.*
import com.example.trucklogger.other.Constants.ACTION_START_SERVICE
import com.example.trucklogger.other.Constants.ACTION_STOP_SERVICE
import com.example.trucklogger.other.Constants.FASTEST_LOCATION_UPDATE_INTERVAL
import com.example.trucklogger.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.trucklogger.other.Constants.MPS_TO_KMH
import com.example.trucklogger.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.trucklogger.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.trucklogger.other.Constants.NOTIFICATION_ID
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.net.ssl.*

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
    var TRUCKER_ID : Int = 0;
    var TRUCKER_UUID : String = "";
    var TRUCKER_VERIFIED  : Boolean = false;

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        currNotificationBuilder = baseNotificationBuilder
        GlobalScope.launch(Dispatchers.IO) {
            serverConnector = ServerConnector(sslSocketFactory)
        }
        sharedPreferences = this.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        getSettings()
        speed.postValue(0.0f)
        lat.postValue(0.0f)
        lng.postValue(0.0f)
        logsStashed.postValue(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.postValue(false)
    }

    private fun getSettings(){
        with (sharedPreferences) {
            TRUCKER_ID = getInt("ID", 0)
            TRUCKER_UUID = getString("UUID", "NONE").toString()
            TRUCKER_VERIFIED = getBoolean("VERIFIED", false)
        }
    }

   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
       intent?.let {
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
        getSettings()
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

        val logs = truckLogDao.getAllTruckLogs()
        val serverRequest = ServerRequest(TRUCKER_ID,TRUCKER_UUID, ServerRequestCode.REQUEST_UPDATE_LOGS.value, logs)

        val result = serverConnector.sendMessage(serverRequest)
        Timber.d("$result")

        var notif:String = ""
        var count:Int = 0;
        when (ServerResponseCode.fromInt(result.res)){
            ServerResponseCode.RESPONSE_OK -> {
                for (log in logs) {
                    truckLogDao.deleteTruckLog(log)
                }
                count = truckLogDao.getTruckLogsCount()
                notif = "${String.format("%.0f",truckLog.spd)} km/h Updated, $count logs stashed"
            }

            ServerResponseCode.RESPONSE_TIMEOUT -> {
                count = truckLogDao.getTruckLogsCount()
                notif = "${String.format("%.0f",truckLog.spd)} km/h $count log/s stashed. No server Connection"
            }

            ServerResponseCode.RESPONSE_INVALID_CREDENTIALS -> {
                count = truckLogDao.getTruckLogsCount()
                notif = "${String.format("%.0f",truckLog.spd)} km/h $count log/s stashed. Unverified ID"
                with(sharedPreferences.edit()){
                    putBoolean("VERIFIED", false)
                    apply()
                }
            }

            ServerResponseCode.RESPONSE_PARSE_FAIL, ServerResponseCode.RESPONSE_DB_CONN_FAIL -> {
                count = truckLogDao.getTruckLogsCount()
                notif = "${String.format("%.0f",truckLog.spd)} km/h $count log/s stashed. Server Error"
            }

            else -> {
                count = truckLogDao.getTruckLogsCount()
                notif = "${String.format("%.0f",truckLog.spd)} km/h $count log/s stashed"
            }
        }

        logsStashed.postValue(truckLogDao.getTruckLogsCount())
        //update notification
        val notification = currNotificationBuilder
            .setContentText(notif)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }
}
