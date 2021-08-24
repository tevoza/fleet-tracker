package com.example.trucklogger.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.JsonAdapter
import java.sql.Timestamp

@Entity(tableName = "tblTruckLogs")
data class TruckLog (
    var tim : Long  = 0L,
    var lat : Float = 0f,
    var lon : Float = 0f,
    var spd : Float = 0f,
    var acc : Float = 0f,
        ) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}