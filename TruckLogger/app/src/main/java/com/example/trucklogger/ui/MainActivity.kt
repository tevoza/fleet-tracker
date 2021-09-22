package com.example.trucklogger.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cardiomood.android.controls.gauge.SpeedometerGauge
import com.example.trucklogger.R
import com.example.trucklogger.db.TruckLogDAO
import com.example.trucklogger.other.*
import com.example.trucklogger.other.Constants.ACTION_SHOW_UI
import com.example.trucklogger.other.Constants.ACTION_START_SERVICE
import com.example.trucklogger.other.Constants.ACTION_STOP_SERVICE
import com.example.trucklogger.other.Constants.PREFERENCES_FILE
import com.example.trucklogger.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.trucklogger.services.ServerConnector
import com.example.trucklogger.services.TrackingService
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.net.ssl.SSLSocketFactory

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    @Inject
    lateinit var truckerLogDao: TruckLogDAO

    @Inject
    lateinit var sslSocketFactory: SSLSocketFactory
    lateinit var serverConnector : ServerConnector
    lateinit var sharedPreferences: SharedPreferences

    var TRUCKER_ID: Int = 0
    var TRUCKER_UUID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions()
        GlobalScope.launch(Dispatchers.IO) { serverConnector = ServerConnector(sslSocketFactory) }

        btnUpdateId.setOnClickListener { updateTruckerID() }

        sharedPreferences = this.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)

        val switchStatus : Switch = findViewById(R.id.switchStatus)
        val viewStatus : TextView = findViewById(R.id.ViewStatus)
        TrackingService.isRunning.observe(this, androidx.lifecycle.Observer {
            if (it) {
                switchStatus.isChecked = true
                viewStatus.text = "RUNNING"
            } else {
                switchStatus.isChecked = false
                viewStatus.text = "IDLE"
            }
        })

        if (intent.action == ACTION_SHOW_UI) {
            Timber.d("coming from notification")
            //switchStatus.isChecked = true
        }
        switchStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                sendCommandToService(ACTION_START_SERVICE)
                viewStatus.text = "RUNNING"
            }
            else{
                sendCommandToService(ACTION_STOP_SERVICE)
                viewStatus.text = "IDLE"
            }
        }
        updateUI()
        setSpeedo()
        TrackingService.speed.observe(this, androidx.lifecycle.Observer {
            val speedo :SpeedometerGauge = findViewById(R.id.speedometer)
            speedo.speed = it.toDouble()
        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_SHOW_UI) {
            Timber.d("coming from notification")
            //switchStatus.isChecked = true
        }
        updateUI()
    }

    fun updateTruckerID() {
        TRUCKER_ID = editTextId.text.toString().toInt()
        TRUCKER_UUID = UUID.randomUUID().toString()
        val request = ServerRequest(TRUCKER_ID, TRUCKER_UUID, ServerRequestCode.REQUEST_UPDATE_ID.value, null)
        var responseCode: ServerResponseCode

        GlobalScope.launch(Dispatchers.IO) {
            val result = serverConnector.sendMessage(request)
            responseCode = ServerResponseCode.fromInt(result.res)
            when (responseCode){
                ServerResponseCode.RESPONSE_OK -> {
                    with(sharedPreferences.edit()){
                        putInt("ID", TRUCKER_ID)
                        putString("UUID", TRUCKER_UUID)
                        putString("TRUCKER", result.trucker)
                        putBoolean("VERIFIED", true)
                        apply()
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext,  "Updated Identity.", Toast.LENGTH_SHORT).show()
                    }
                }

                ServerResponseCode.RESPONSE_FAIL -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext,
                            "Can't update ID. Ask your manager to reset your ID.", Toast.LENGTH_SHORT).show()
                    }
                }

                ServerResponseCode.RESPONSE_TIMEOUT -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext,  "No server connection", Toast.LENGTH_SHORT).show()
                    }
                }

                ServerResponseCode.RESPONSE_DB_CONN_FAIL,ServerResponseCode.RESPONSE_PARSE_FAIL, -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext,  "Server Error", Toast.LENGTH_SHORT).show()
                    }
                }

                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext,  "Failed!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        updateUI()
    }

    private fun updateUI(){
        val textTrucker: TextView = findViewById(R.id.textTrucker)
        textTrucker.text = sharedPreferences.getString("TRUCKER", "Unknown").toString()
        val textId: TextView = findViewById(R.id.textId)
        val truckerID = sharedPreferences.getInt("ID", 0).toString()
        val truckerVerified = sharedPreferences.getBoolean("VERIFIED", false)
        if (truckerVerified) {
            textId.text = "$truckerID \u2713"
        } else {
            textId.text = "$truckerID \u274c"
        }
    }

    private fun setSpeedo() {
        val speedo :SpeedometerGauge = findViewById(R.id.speedometer)

        // configure value range and ticks
        speedo.maxSpeed = 200.0;
        speedo.majorTickStep = 30.0;
        speedo.minorTicks = 2;

        // Configure value range colors
        speedo.addColoredRange(0.0, 60.0, Color.GREEN);
        speedo.addColoredRange(60.0, 100.0, Color.YELLOW);
        speedo.addColoredRange(100.0, 200.0, Color.RED);

        speedo.speed = 100.0

    }

    private fun sendCommandToService(action: String) =
        Intent(this, TrackingService::class.java).also {
            it.action = action
            this.startService(it)
        }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        if (TrackingUtility.hasLocationPermissions(this)) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "This application requires location permissions to function.",
                REQUEST_CODE_LOCATION_PERMISSION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "This application requires location permissions to function.",
                REQUEST_CODE_LOCATION_PERMISSION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }
}