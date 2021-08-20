package com.example.trucklogger.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity(tableName = "tblTruckLogs")
data class TruckLog (
    var timestamp: Long = 0L,
    var lat:Float = 0f,
    var long:Float = 0f,
    var accel:Float = 0f,
        ) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}