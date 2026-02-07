package com.example.yapenotifier.di

import com.example.yapenotifier.domain.repository.EventRepository
import com.example.yapenotifier.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceEntryPoint {
    fun eventRepository(): EventRepository
    fun settingsRepository(): SettingsRepository
}
