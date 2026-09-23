package com.ravaa.drive.di

import android.content.Context
import androidx.room.Room
import com.ravaa.drive.data.db.AppDatabase
import com.ravaa.drive.data.sync.ConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "ravaa-drive.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideConnectivity(@ApplicationContext ctx: Context): ConnectivityObserver =
        ConnectivityObserver(ctx)
}
