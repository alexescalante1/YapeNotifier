package com.example.yapenotifier.domain.repository

import com.example.yapenotifier.domain.model.SettingsSnapshot
import com.example.yapenotifier.domain.model.SmsContact
import com.example.yapenotifier.domain.model.WatchedPackage
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun contactsFlow(): Flow<List<SmsContact>>
    fun watchedPackagesFlow(): Flow<List<WatchedPackage>>
    fun captureAllFlow(): Flow<Boolean>
    fun lastSeenPackageFlow(): Flow<String>
    fun lastSeenTextFlow(): Flow<String>

    suspend fun addContact(contact: SmsContact)
    suspend fun removeContact(number: String)
    suspend fun getNumbersOnce(): Set<String>

    suspend fun addWatchedPackage(pkg: WatchedPackage)
    suspend fun removeWatchedPackage(packageName: String)
    suspend fun updateWatchedPackage(oldPackageName: String, updated: WatchedPackage)
    suspend fun getPackageNamesOnce(): Set<String>

    suspend fun setCaptureAll(enabled: Boolean)
    suspend fun setLastSeen(packageName: String, text: String)

    suspend fun getSettingsSnapshot(): SettingsSnapshot
    suspend fun setServiceEnabled(enabled: Boolean)
    suspend fun isServiceEnabledOnce(): Boolean
}
