package com.example.trucklogger.repositories

import android.content.SharedPreferences
import com.example.trucklogger.db.TruckLog
import com.example.trucklogger.db.TruckLogDAO
import com.example.trucklogger.other.Constants.KEY_TRUCKER_ID
import com.example.trucklogger.other.Constants.KEY_TRUCKER_MANAGER
import com.example.trucklogger.other.Constants.KEY_TRUCKER_NAME
import com.example.trucklogger.other.Constants.KEY_TRUCKER_UUID
import com.example.trucklogger.other.Constants.KEY_TRUCKER_VEHICLE_NUMBER
import com.example.trucklogger.other.Constants.KEY_TRUCKER_VERIFIED
import com.example.trucklogger.other.Constants.KEY_UPLOAD_FREQUENCY
import com.example.trucklogger.other.Constants.UPLOAD_CONTINUOUSLY
import com.example.trucklogger.other.ServerRequest
import com.example.trucklogger.other.ServerRequestCode
import com.example.trucklogger.other.ServerResponseCode
import com.example.trucklogger.services.ServerConnector
import java.util.*
import javax.inject.Inject
import javax.net.ssl.SSLSocketFactory

class MainRepository @Inject constructor(
    private val truckLogDAO: TruckLogDAO,
    sslSocketFactory: SSLSocketFactory,
    val sharedPreferences: SharedPreferences
){
    var serverConnector =  ServerConnector(sslSocketFactory)
    var appSettings : Settings = Settings(0, "", "", "", "", false, 0)

    suspend fun insertTruckingLog(truckLog : TruckLog) = truckLogDAO.insertTruckLog(truckLog)
    suspend fun deleteTruckingLog(truckLog : TruckLog) = truckLogDAO.deleteTruckLog(truckLog)
    fun getAllTruckLogs() = truckLogDAO.getAllTruckLogs()
    fun getTruckLogsCount() = truckLogDAO.getTruckLogsCount()

    fun fetchSettings()
    {
        with (sharedPreferences) {
            appSettings.TRUCKER_ID = getInt(KEY_TRUCKER_ID,0)
            appSettings.TRUCKER_NAME = getString(KEY_TRUCKER_NAME, "Unknown").toString()
            appSettings.TRUCKER_UUID = getString(KEY_TRUCKER_UUID, "").toString()
            appSettings.TRUCKER_VEHICLE_NUMBER = getString(KEY_TRUCKER_VEHICLE_NUMBER, "").toString()
            appSettings.TRUCKER_MANAGER = getString(KEY_TRUCKER_MANAGER, "Unknown").toString()
            appSettings.TRUCKER_VERIFIED = getBoolean(KEY_TRUCKER_VERIFIED, false)
            appSettings.UPLOAD_FREQUENCY = getInt(KEY_UPLOAD_FREQUENCY, UPLOAD_CONTINUOUSLY)
        }
    }

    fun writeSettings(settings: Settings) {
        with (sharedPreferences.edit()) {
            putInt(KEY_TRUCKER_ID, settings.TRUCKER_ID)
            putString(KEY_TRUCKER_UUID, settings.TRUCKER_UUID)
            putString(KEY_TRUCKER_NAME, settings.TRUCKER_NAME)
            putString(KEY_TRUCKER_VEHICLE_NUMBER, settings.TRUCKER_VEHICLE_NUMBER)
            putString(KEY_TRUCKER_MANAGER, settings.TRUCKER_MANAGER)
            putBoolean(KEY_TRUCKER_VERIFIED, settings.TRUCKER_VERIFIED)
            putInt(KEY_UPLOAD_FREQUENCY, settings.UPLOAD_FREQUENCY)
            apply()
        }
    }

    suspend fun uploadLogs() : ServerResponseCode
    {
        fetchSettings()
        var count: Int
        var resultCode: ServerResponseCode
        //send all logs until none left
        do {
            var logs = getAllTruckLogs()
            var serverRequest = ServerRequest(
                appSettings.TRUCKER_ID,
                appSettings.TRUCKER_UUID,
                ServerRequestCode.REQUEST_UPDATE_LOGS.value,
                logs
            )
            var result = serverConnector.sendMessage(serverRequest)
            resultCode = ServerResponseCode.fromInt(result.res)
            if (resultCode == ServerResponseCode.RESPONSE_OK) {
                for (log in logs) {
                    deleteTruckingLog(log)
                }
            }
            count = getTruckLogsCount()
        } while ((count > 0) && (resultCode == ServerResponseCode.RESPONSE_OK))

        if (resultCode == ServerResponseCode.RESPONSE_INVALID_CREDENTIALS )
        {
            with(sharedPreferences.edit()) {
                putBoolean(KEY_TRUCKER_VERIFIED, false)
                apply()
            }
        }
        fetchSettings()
        return resultCode
    }

    suspend fun updateID(id:Int) : ServerResponseCode
    {
        fetchSettings()
        val uuid = UUID.randomUUID().toString()
        val request = ServerRequest(id, uuid, ServerRequestCode.REQUEST_UPDATE_ID.value, null)
        val result = serverConnector.sendMessage(request)
        val response = ServerResponseCode.fromInt(result.res)

        if (response == ServerResponseCode.RESPONSE_OK) {
            with (sharedPreferences.edit()) {
                putString(KEY_TRUCKER_NAME, result.trucker)
                putString(KEY_TRUCKER_VEHICLE_NUMBER, result.veh)
                putInt(KEY_TRUCKER_ID, id)
                putString(KEY_TRUCKER_UUID, uuid)
                putBoolean(KEY_TRUCKER_VERIFIED, true)
                apply()
            }
        }
        fetchSettings()
        return response
    }
}