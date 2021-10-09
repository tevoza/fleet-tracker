package com.example.trucklogger.di

import android.content.SharedPreferences
import com.example.trucklogger.db.TruckLogDAO
import com.example.trucklogger.repositories.MainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.net.ssl.SSLSocketFactory

@InstallIn(SingletonComponent::class)
@Module
object RepositoryModule {
    @Singleton
    @Provides
    fun provideMainRepository(
        truckLogDAO: TruckLogDAO,
        sslSocketFactory: SSLSocketFactory,
        sharedPreferences: SharedPreferences
    ) : MainRepository {
        return MainRepository(truckLogDAO, sslSocketFactory, sharedPreferences)
    }
}