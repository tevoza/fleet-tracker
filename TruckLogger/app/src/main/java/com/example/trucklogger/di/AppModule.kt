package com.example.trucklogger.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.trucklogger.R
import com.example.trucklogger.db.TruckLoggingDatabase
import com.example.trucklogger.other.Constants
import com.example.trucklogger.other.Constants.TRUCKLOGGING_DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.DefineComponent
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.security.KeyStore
import javax.inject.Singleton
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

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

    @Singleton
    @Provides
    fun provideSSLSocketFactory(
        @ApplicationContext app: Context
    ): SSLSocketFactory {
        val trusted = KeyStore.getInstance(KeyStore.getDefaultType())
        val inputStream = app.resources.openRawResource(R.raw.keystore)
        trusted.load(inputStream, Constants.KEYSTORE_PASS)

        val kmf = KeyManagerFactory
            .getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(trusted, Constants.KEYSTORE_PASS)

        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(kmf.keyManagers, null, null)
        return sslContext.socketFactory
    }
}