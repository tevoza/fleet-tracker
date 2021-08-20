package com.example.trucklogger.repositories

import com.example.trucklogger.db.TruckLogDAO
import com.example.trucklogger.db.TruckLog
import javax.inject.Inject

class MainRepository @Inject constructor(
    val truckLogDAO: TruckLogDAO
){
    suspend fun insertTruckingLog(truckLog : TruckLog) = truckLogDAO.insertTruckLog(truckLog)
    suspend fun deleteTruckingLog(truckLog : TruckLog) = truckLogDAO.deleteTruckLog(truckLog)
    fun getAllTruckLogs(truckLog : TruckLog) = truckLogDAO.getAllTruckLogs()
}