package com.example.trucklogger.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.trucklogger.R
import com.example.trucklogger.db.TruckLogDAO
import com.example.trucklogger.other.Constants.ACTION_SHOW_UI
import com.example.trucklogger.other.Constants.ACTION_START_SERVICE
import com.example.trucklogger.other.Constants.ACTION_STOP_SERVICE
import com.example.trucklogger.other.Constants.PREFERENCES_FILE
import com.example.trucklogger.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.trucklogger.other.ServerRequest
import com.example.trucklogger.other.ServerRequestCode
import com.example.trucklogger.other.ServerResponseCode
import com.example.trucklogger.other.TrackingUtility
import com.example.trucklogger.services.ServerConnector
import com.example.trucklogger.services.TrackingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.net.ssl.SSLSocketFactory

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, AdapterView.OnItemClickListener,
    AdapterView.OnItemSelectedListener {
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

        btnUpdateId.setOnClickListener { showDialog() }

        sharedPreferences = this.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)

        val switchStatus : Switch = findViewById(R.id.switchStatus)
        val viewStatus : TextView = findViewById(R.id.ViewStatus)
        val viewSpeed : TextView = findViewById(R.id.textSpeed)
        val viewLat : TextView = findViewById(R.id.textLat)
        val viewLng : TextView = findViewById(R.id.textLng)
        val viewLogsStashed : TextView = findViewById(R.id.textLogsStashed)

        TrackingService.isRunning.observe(this, {
            if (it) {
                switchStatus.isChecked = true
                viewStatus.text = "RUNNING"
            } else {
                switchStatus.isChecked = false
                viewStatus.text = "IDLE"
            }
        })

        TrackingService.speed.observe(this, {
           viewSpeed.text = String.format("%.1f", it)
        })

        TrackingService.lat.observe(this, {
            viewLat.text = String.format("%.4f", it)
        })

        TrackingService.lng.observe(this, {
            viewLng.text = String.format("%.4f", it)
        })

        TrackingService.logsStashed.observe(this, {
            viewLogsStashed.text = "$it"
        })

        if (intent.action == ACTION_SHOW_UI) {
            Timber.d("coming from notification")
            //switchStatus.isChecked = true
        }

        switchStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                sendCommandToService(ACTION_START_SERVICE)
                viewStatus.text = "LOGGING"
            }
            else{
                sendCommandToService(ACTION_STOP_SERVICE)
                viewStatus.text = "IDLE"
            }
        }
        updateUI()

        val spinner: Spinner = findViewById(R.id.spinner)
        spinner.onItemSelectedListener = this
        ArrayAdapter.createFromResource(
            this,
            R.array.upload_schedule,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
            spinner.setSelection(sharedPreferences.getInt("UPLOAD_FREQUENCY", 0))
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_SHOW_UI) {
            Timber.d("coming from notification")
            //switchStatus.isChecked = true
        }
        updateUI()
    }

    private fun showDialog() {
        val inflater: LayoutInflater = getSystemService(android.app.Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.edit_dialog, null)
        val alertDialog = AlertDialog.Builder(this).create()
        val etComments:EditText = view.findViewById(R.id.etComments)
        with (alertDialog) {
            setTitle("Update ID")
            setCancelable(true)
            setMessage("Enter your trucker ID.")
            setButton(AlertDialog.BUTTON_POSITIVE ,"OK") { _, _ ->
                val input = etComments.text.toString()
                updateTruckerID(input)
            }
            setButton(AlertDialog.BUTTON_NEGATIVE ,"Cancel") { _, _ ->
                alertDialog.dismiss()
            }
        }
        alertDialog.setView(view)
        alertDialog.show()
    }

    fun updateTruckerID(truckerID:String) {
        TRUCKER_ID = truckerID.toInt()
        TRUCKER_UUID = UUID.randomUUID().toString()
        val request = ServerRequest(TRUCKER_ID, TRUCKER_UUID, ServerRequestCode.REQUEST_UPDATE_ID.value, null)
        var responseCode: ServerResponseCode

        GlobalScope.launch(Dispatchers.IO) {
            val result = serverConnector.sendMessage(request)
            responseCode = ServerResponseCode.fromInt(result.res)
            var toastText:String
            when (responseCode){
                ServerResponseCode.RESPONSE_OK -> {
                    with(sharedPreferences.edit()){
                        putInt("ID", TRUCKER_ID)
                        putString("UUID", TRUCKER_UUID)
                        putString("TRUCKER", result.trucker)
                        putString("VEHICLE", result.veh)
                        putString("MANAGER", result.manager)
                        putBoolean("VERIFIED", true)
                        apply()
                    }
                    toastText = "Updated Identity."
                }
                ServerResponseCode.RESPONSE_FAIL -> {
                    toastText = "Can't update ID. Ask your manager to reset your ID."
                }
                ServerResponseCode.RESPONSE_TIMEOUT -> {
                    toastText = "No server connection"
                }
                ServerResponseCode.RESPONSE_DB_CONN_FAIL,ServerResponseCode.RESPONSE_PARSE_FAIL, -> {
                    toastText = "Server Error"
                }
                else -> {
                    toastText = "Failed!"
                }
            }

            withContext(Dispatchers.Main) {
                updateUI()
                Toast.makeText(applicationContext,  toastText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(){
        val textTrucker: TextView = findViewById(R.id.textTrucker)
        textTrucker.text = sharedPreferences.getString("TRUCKER", "Unknown").toString()

        val textVehicle: TextView = findViewById(R.id.textVehicleNumber)
        textVehicle.text = sharedPreferences.getString("VEHICLE", "Unknown").toString()

        val textManager: TextView = findViewById(R.id.textManager)
        textManager.text = sharedPreferences.getString("MANAGER", "Unknown").toString()

        val textId: TextView = findViewById(R.id.textId)
        val truckerID = sharedPreferences.getInt("ID", 0).toString()
        val truckerVerified = sharedPreferences.getBoolean("VERIFIED", false)
        if (truckerVerified) {
            textId.text = "$truckerID \u2713"
        } else {
            textId.text = "$truckerID \u274c"
        }
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

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        with(sharedPreferences.edit()){
            putInt("UPLOAD_FREQUENCY", pos)
            apply()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Timber.d("spinner : nothing selected")
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Timber.d("spinner click")
    }
}