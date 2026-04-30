package com.fantok.app.di

import android.content.Context
import com.fantok.app.data.local.AppDatabase
import com.fantok.app.data.local.VideoCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideVideoCacheDao(database: AppDatabase): VideoCacheDao {
        return database.videoCacheDao()
    }
}
