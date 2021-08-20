package com.example.trucklogger.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TruckLog::class],
    version = 1
)

abstract class TruckLoggingDatabase : RoomDatabase (){
    abstract fun getTruckerLogDAO() : TruckLogDAO
}