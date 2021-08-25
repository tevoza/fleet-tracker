package com.example.trucklogger.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import com.example.trucklogger.R
import com.example.trucklogger.db.TruckLog
import com.example.trucklogger.db.TruckLogDAO
import com.example.trucklogger.other.Constants.ACTION_SHOW_UI
import com.example.trucklogger.other.Constants.ACTION_START_SERVICE
import com.example.trucklogger.other.Constants.ACTION_STOP_SERVICE
import com.example.trucklogger.other.Constants.FASTEST_LOCATION_UPDATE_INTERVAL
import com.example.trucklogger.other.Constants.KEYSTORE_PASS
import com.example.trucklogger.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.trucklogger.other.Constants.MPS_TO_KMH
import com.example.trucklogger.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.trucklogger.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.trucklogger.other.Constants.NOTIFICATION_ID
import com.example.trucklogger.other.Constants.SERVER_IP
import com.example.trucklogger.other.Constants.SERVER_PORT
import com.example.trucklogger.other.ServerRequest
import com.example.trucklogger.other.TrackingUtility
import com.example.trucklogger.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.gson.Gson
import com.google.gson.JsonArray
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.io.PrintWriter
import java.security.*
import java.util.*
import javax.inject.Inject
import javax.net.ssl.*

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    var started = true

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
    var TRUCKERID : Int = 0;

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        currNotificationBuilder = baseNotificationBuilder
        GlobalScope.launch(Dispatchers.IO) {
            serverConnector = ServerConnector(sslSocketFactory)
        }
    }

   override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_SERVICE -> {
                    TRUCKERID = it.getIntExtra("TRUCKER_ID", 0)
                    started = false
                    isTracking = true
                    startForegroundService()
                    updateTracking()
                    Timber.d("Started Tracking service, $TRUCKERID")
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped Tracking service")
                    isTracking = false
                    updateTracking()
                    stopForeground(true)
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
            super.onLocationResult(result)
            result?.locations?.let { locations ->
                for (location in locations) {
                    GlobalScope.launch(Dispatchers.IO) { processLocation(location) }
                }
            }
        }
    }

    private suspend fun processLocation(location : Location) {
        Timber.d("${location.longitude.toFloat()}")
        val truckLog = TruckLog(
            location.time/1000,
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.speed * MPS_TO_KMH,
            0f
        )

        truckLogDao.insertTruckLog(truckLog)

        val logs = truckLogDao.getAllTruckLogs()
        val serverRequest = ServerRequest(TRUCKERID, "UPDATE_LOGS", logs)
        val gson = Gson()
        val json = gson.toJson(serverRequest)

        val result = serverConnector.sendMessage(json)
        Timber.d(result)
        var notif:String = ""
        if (result == "OK"){
            for (log in logs) {
                truckLogDao.deleteTruckLog(log)
            }
            notif = "up to date"
        }
        else{
            val count = truckLogDao.getTruckLogsCount()
            notif = "$count log/s stashed"
        }

        //update notification
        val notification = currNotificationBuilder
            .setContentText(notif)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }
}
