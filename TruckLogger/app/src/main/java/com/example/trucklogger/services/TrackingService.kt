package com.example.trucklogger.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.trucklogger.db.TruckLog
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
import com.example.trucklogger.repositories.MainRepository
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
import kotlin.math.pow
import kotlin.math.sqrt

@AndroidEntryPoint
class TrackingService : LifecycleService(), SensorEventListener {
    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder
    lateinit var currNotificationBuilder: NotificationCompat.Builder
    lateinit var notificationManager : NotificationManager

    var started = false
    lateinit var sensorManager:SensorManager
    var sensor: Sensor?  = null
    var accX:Float = 0.0f
    var accY:Float = 0.0f
    var accZ:Float = 0.0f

    var isTracking = false
    companion object {
        val isRunning = MutableLiveData<Boolean> ()
        val log = MutableLiveData<TruckLog> ()
        val logsStashed = MutableLiveData<Int> ()
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        currNotificationBuilder = baseNotificationBuilder

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        mainRepository.fetchSettings()
        log.postValue(TruckLog())
        logsStashed.postValue(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.postValue(false)
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
                sensorManager.registerListener(this, sensor,1000000)
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            sensorManager.unregisterListener(this)
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
        mainRepository.fetchSettings()
        val truckLog = TruckLog(
            location.time/1000,
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.speed * MPS_TO_KMH,
            sqrt(accX.pow(2) + accY.pow(2) + accZ.pow(2)),
            location.altitude.toFloat()
        )
        log.postValue(truckLog)

        mainRepository.insertTruckingLog(truckLog)

        var count = mainRepository.getTruckLogsCount()
        var statusUpload = ""

        with (mainRepository.appSettings) {
            if (TRUCKER_VERIFIED) {
                if ((UPLOAD_FREQUENCY == UPLOAD_CONTINUOUSLY) || (UPLOAD_FREQUENCY == UPLOAD_HOURLY && count > 10)) {
                    statusUpload = when(mainRepository.uploadLogs()) {
                        ServerResponseCode.RESPONSE_OK -> {
                            "Up to date."
                        }
                        ServerResponseCode.RESPONSE_PARSE_FAIL -> {
                            "Server parsing error."
                        }
                        ServerResponseCode.RESPONSE_DB_CONN_FAIL -> {
                            "Server database error."
                        }
                        ServerResponseCode.RESPONSE_FAIL -> {
                            "Server error."
                        }
                        ServerResponseCode.RESPONSE_TIMEOUT -> {
                            "No network connection."
                        }
                        ServerResponseCode.RESPONSE_INVALID_CREDENTIALS -> {
                            "Unverified ID."
                        }
                        else -> {
                            "Error."
                        }
                    }
                }
            } else {
                statusUpload = "Unverified ID."
            }
        }

        count = mainRepository.getTruckLogsCount()
        logsStashed.postValue(count)

        //update notification
        val notif = "${String.format("%.0f",truckLog.spd)} km/h. $count log/s stashed. $statusUpload"
        val notification = currNotificationBuilder
            .setContentText(notif)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            with (event) {
                accX = values[0]
                accY = values[1]
                accY = values[2]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        TODO("Not yet implemented")
    }
}
