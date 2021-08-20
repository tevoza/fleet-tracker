package com.example.trucklogger.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.trucklogger.db.TruckLoggingDatabase
import com.example.trucklogger.other.Constants.TRUCKLOGGING_DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.DefineComponent
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideTruckerLogDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        TruckLoggingDatabase::class.java,
        TRUCKLOGGING_DATABASE_NAME
    ).build()

    @Singleton
    @Provides
    fun provideTruckerLogDAO(db: TruckLoggingDatabase) = db.getTruckerLogDAO()
}