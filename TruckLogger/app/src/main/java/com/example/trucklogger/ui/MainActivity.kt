package com.example.trucklogger.ui

import android.annotation.SuppressLint
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
import com.example.trucklogger.other.Constants.ACTION_UPLOAD_FAIL
import com.example.trucklogger.other.Constants.ACTION_UPLOAD_LOGS
import com.example.trucklogger.other.Constants.ACTION_UPLOAD_SUCCESS
import com.example.trucklogger.other.Constants.KEY_TRUCKER_ID
import com.example.trucklogger.other.Constants.KEY_TRUCKER_MANAGER
import com.example.trucklogger.other.Constants.KEY_TRUCKER_NAME
import com.example.trucklogger.other.Constants.KEY_TRUCKER_UUID
import com.example.trucklogger.other.Constants.KEY_TRUCKER_VEHICLE_NUMBER
import com.example.trucklogger.other.Constants.KEY_TRUCKER_VERIFIED
import com.example.trucklogger.other.Constants.KEY_UPLOAD_FREQUENCY
import com.example.trucklogger.other.Constants.PREFERENCES_FILE
import com.example.trucklogger.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.trucklogger.other.ServerRequest
import com.example.trucklogger.other.ServerRequestCode
import com.example.trucklogger.other.ServerResponseCode
import com.example.trucklogger.other.TrackingUtility
import com.example.trucklogger.repositories.MainRepository
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
    lateinit var mainRepository: MainRepository

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    lateinit var switchStatus : Switch
    lateinit var viewStatus : TextView
    lateinit var viewSpeed : TextView
    lateinit var viewLat : TextView
    lateinit var viewLng : TextView
    lateinit var viewLogsStashed : TextView
    lateinit var spinner: Spinner
    lateinit var textVehicle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions()

        switchStatus        = findViewById(R.id.switchStatus)
        viewStatus          = findViewById(R.id.ViewStatus)
        viewSpeed           = findViewById(R.id.textSpeed)
        viewLat             = findViewById(R.id.textLat)
        viewLng             = findViewById(R.id.textLng)
        viewLogsStashed     = findViewById(R.id.textLogsStashed)
        spinner             = findViewById(R.id.spinner)
        textVehicle         = findViewById(R.id.textVehicleNumber)

        btnUpdateId.setOnClickListener { showDialog() }
        subscribeToObservers()

        if (intent.action == ACTION_SHOW_UI) {
            Timber.d("coming from notification")
        }

        switchStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                sendCommandToService(ACTION_START_SERVICE)
            }
            else{
                sendCommandToService(ACTION_STOP_SERVICE)
            }
        }

        btnUpdateId.setOnClickListener {
            showDialog()
        }

        btnUploadLogs.setOnClickListener {
            Toast.makeText(this, "Uploading logs...", Toast.LENGTH_SHORT).show()
            GlobalScope.launch(Dispatchers.IO) { uploadLogs() }
        }

        updateUI()
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
            spinner.setSelection(mainRepository.appSettings.UPLOAD_FREQUENCY)
        }
    }

    private fun subscribeToObservers(){
        TrackingService.isRunning.observe(this, {
            if (it) {
                switchStatus.isChecked = true
                viewStatus.text = "RUNNING"
            } else {
                switchStatus.isChecked = false
                viewStatus.text = "IDLE"
            }
        })

        TrackingService.log.observe(this, {
            viewLat.text = String.format("%.4f", it.lat)
            viewLng.text = String.format("%.4f", it.lon)
            viewSpeed.text = String.format("%.1f", it.spd)
            textAccel.text = String.format("%.2f", it.acc)
            textAltitude.text = String.format("%.1f", it.alt)
        })

        TrackingService.logsStashed.observe(this, {
            viewLogsStashed.text = "$it"
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
                Toast.makeText(this@MainActivity, "Updating ID..." ,Toast.LENGTH_SHORT, ).show()
                val input = etComments.text.toString()
                GlobalScope.launch(Dispatchers.IO) { updateTruckerID(input.toInt()) }
            }
            setButton(AlertDialog.BUTTON_NEGATIVE ,"Cancel") { _, _ ->
                alertDialog.dismiss()
            }
        }
        alertDialog.setView(view)
        alertDialog.show()
    }

    suspend fun updateTruckerID(newID : Int) {
        val toastText = when(mainRepository.updateID(newID)) {
            ServerResponseCode.RESPONSE_OK -> {
                "Successfully updated ID."
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
            else -> {
                "Error."
            }
        }
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, toastText, Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    private suspend fun uploadLogs() {
        val toastText = when(mainRepository.uploadLogs()) {
            ServerResponseCode.RESPONSE_OK -> {
                "Successfully uploaded logs."
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
            else -> {
                "Error."
            }
        }
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, toastText, Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    private fun updateUI(){
        mainRepository.fetchSettings()

        with (mainRepository.appSettings) {
            textTrucker.text = TRUCKER_NAME
            textVehicle.text = TRUCKER_VEHICLE_NUMBER
            textManager.text = TRUCKER_MANAGER
            val truckerID = TRUCKER_ID
            val truckerVerified = TRUCKER_VERIFIED
            if (truckerVerified) {
                textId.text = "$truckerID \u2713"
            } else {
                textId.text = "$truckerID \u274c"
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            textLogsStashed.text = mainRepository.getTruckLogsCount().toString()
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(this, TrackingService::class.java).also {
            it.action = action
            this.startService(it)
        }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        with(mainRepository.sharedPreferences.edit()){
            putInt(KEY_UPLOAD_FREQUENCY, pos)
            apply()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Timber.d("spinner : nothing selected")
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Timber.d("spinner click")
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