package com.example.trucklogger.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TruckLogDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTruckLog(truckLog: TruckLog)

    @Delete
    suspend fun deleteTruckLog(truckLog: TruckLog)

    @Query("SELECT * FROM tblTruckLogs ORDER BY tim ASC LIMIT 100")
    fun getAllTruckLogs(): List<TruckLog>

    @Query("SELECT COUNT(*) FROM tblTruckLogs")
    fun getTruckLogsCount(): Int
}