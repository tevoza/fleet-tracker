package com.example.trucklogger.ui

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.trucklogger.R
import com.example.trucklogger.db.TruckLogDAO
import com.example.trucklogger.other.Constants.ACTION_SHOW_UI
import com.example.trucklogger.other.Constants.ACTION_START_SERVICE
import com.example.trucklogger.other.Constants.ACTION_STOP_SERVICE
import com.example.trucklogger.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.trucklogger.other.ServerRequest
import com.example.trucklogger.other.ServerRequestCode
import com.example.trucklogger.other.TrackingUtility
import com.example.trucklogger.services.ServerConnector
import com.example.trucklogger.services.TrackingService
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

    var TRUCKER_ID: Int = 0
    var TRUCKER_UUID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions()
        GlobalScope.launch(Dispatchers.IO) { serverConnector = ServerConnector(sslSocketFactory) }

        btnUpdateId.setOnClickListener { updateTruckerID() }

        if (intent.action == ACTION_SHOW_UI) {
            Timber.d("coming from notification")
            switchStatus.isChecked = true
        }
        switchStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) sendCommandToService(ACTION_START_SERVICE, TRUCKER_ID)
            else sendCommandToService(ACTION_STOP_SERVICE, TRUCKER_ID)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_SHOW_UI) {
            Timber.d("coming from notification")
            switchStatus.isChecked = true
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

    fun updateTruckerID() {
        TRUCKER_ID = editTextId.text.toString().toInt()
        TRUCKER_UUID = UUID.randomUUID().toString()
        val request = ServerRequest(TRUCKER_ID, TRUCKER_UUID, ServerRequestCode.REQUEST_UPDATE_ID.value, null)
        val gson = Gson()
        val json = gson.toJson(request)

        GlobalScope.launch(Dispatchers.IO) {
            val result = serverConnector.sendMessage(json)
            Timber.d(result)
        }
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

    private fun sendCommandToService(action: String, truckerID: Int) =
        Intent(this, TrackingService::class.java).also {
            it.action = action
            it.putExtra("TRUCKER_ID", truckerID)
            this.startService(it)
        }
}