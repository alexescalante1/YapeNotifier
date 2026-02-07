package com.example.yapenotifier.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.example.yapenotifier.data.database.EventDao
import com.example.yapenotifier.data.database.YapeDatabase
import com.example.yapenotifier.data.datastore.dataStore
import com.example.yapenotifier.data.repository.EventRepositoryImpl
import com.example.yapenotifier.data.repository.SettingsRepositoryImpl
import com.example.yapenotifier.domain.repository.EventRepository
import com.example.yapenotifier.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): YapeDatabase {
        return Room.databaseBuilder(
            context,
            YapeDatabase::class.java,
            "yape_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideEventDao(database: YapeDatabase): EventDao {
        return database.eventDao()
    }

    @Provides
    @Singleton
    fun provideEventRepository(eventDao: EventDao): EventRepository {
        return EventRepositoryImpl(eventDao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository {
        return SettingsRepositoryImpl(dataStore)
    }
}
