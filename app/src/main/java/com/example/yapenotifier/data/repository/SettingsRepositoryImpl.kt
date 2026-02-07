package com.example.yapenotifier.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.yapenotifier.data.datastore.YapeDataStoreKeys
import com.example.yapenotifier.domain.model.SettingsSnapshot
import com.example.yapenotifier.domain.model.SmsContact
import com.example.yapenotifier.domain.model.WatchedPackage
import com.example.yapenotifier.domain.repository.SettingsRepository
import com.example.yapenotifier.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    // ── Contacts ──

    override fun contactsFlow(): Flow<List<SmsContact>> {
        return dataStore.data.map { prefs ->
            parseContacts(prefs[YapeDataStoreKeys.KEY_CONTACTS_JSON].orEmpty())
        }
    }

    override suspend fun addContact(contact: SmsContact) {
        if (contact.number.isBlank()) return
        dataStore.edit { prefs ->
            val current = parseContacts(prefs[YapeDataStoreKeys.KEY_CONTACTS_JSON].orEmpty()).toMutableList()
            if (current.none { it.number == contact.number }) {
                current.add(contact)
            }
            prefs[YapeDataStoreKeys.KEY_CONTACTS_JSON] = encodeContacts(current)
        }
    }

    override suspend fun removeContact(number: String) {
        dataStore.edit { prefs ->
            val current = parseContacts(prefs[YapeDataStoreKeys.KEY_CONTACTS_JSON].orEmpty()).toMutableList()
            current.removeAll { it.number == number }
            prefs[YapeDataStoreKeys.KEY_CONTACTS_JSON] = encodeContacts(current)
        }
    }

    override suspend fun getNumbersOnce(): Set<String> {
        val contacts = parseContacts(
            dataStore.data.first()[YapeDataStoreKeys.KEY_CONTACTS_JSON].orEmpty()
        )
        return contacts.map { it.number }.toSet()
    }

    // ── Watched Packages ──

    override fun watchedPackagesFlow(): Flow<List<WatchedPackage>> {
        return dataStore.data.map { prefs ->
            resolvePackages(prefs)
        }
    }

    override suspend fun addWatchedPackage(pkg: WatchedPackage) {
        if (pkg.packageName.isBlank()) return
        dataStore.edit { prefs ->
            val current = resolvePackages(prefs).toMutableList()
            if (current.none { it.packageName == pkg.packageName }) {
                current.add(pkg)
            }
            prefs[YapeDataStoreKeys.KEY_PACKAGES_JSON] = encodePackages(current)
        }
    }

    override suspend fun removeWatchedPackage(packageName: String) {
        dataStore.edit { prefs ->
            val current = resolvePackages(prefs).toMutableList()
            current.removeAll { it.packageName == packageName }
            prefs[YapeDataStoreKeys.KEY_PACKAGES_JSON] = encodePackages(current)
        }
    }

    override suspend fun updateWatchedPackage(oldPackageName: String, updated: WatchedPackage) {
        if (updated.packageName.isBlank()) return
        dataStore.edit { prefs ->
            val current = resolvePackages(prefs).toMutableList()
            val index = current.indexOfFirst { it.packageName == oldPackageName }
            if (index >= 0) {
                current[index] = updated
            }
            prefs[YapeDataStoreKeys.KEY_PACKAGES_JSON] = encodePackages(current)
        }
    }

    override suspend fun getPackageNamesOnce(): Set<String> {
        val prefs = dataStore.data.first()
        return resolvePackages(prefs).map { it.packageName }.toSet()
    }

    // ── Settings ──

    override fun captureAllFlow(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[YapeDataStoreKeys.KEY_CAPTURE_ALL] ?: false
        }
    }

    override fun lastSeenPackageFlow(): Flow<String> {
        return dataStore.data.map { prefs ->
            prefs[YapeDataStoreKeys.KEY_LAST_SEEN_PACKAGE].orEmpty()
        }
    }

    override fun lastSeenTextFlow(): Flow<String> {
        return dataStore.data.map { prefs ->
            prefs[YapeDataStoreKeys.KEY_LAST_SEEN_TEXT].orEmpty()
        }
    }

    override suspend fun setCaptureAll(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[YapeDataStoreKeys.KEY_CAPTURE_ALL] = enabled
        }
    }

    override suspend fun setLastSeen(packageName: String, text: String) {
        dataStore.edit { prefs ->
            prefs[YapeDataStoreKeys.KEY_LAST_SEEN_PACKAGE] = packageName
            prefs[YapeDataStoreKeys.KEY_LAST_SEEN_TEXT] = text
        }
    }

    override suspend fun getSettingsSnapshot(): SettingsSnapshot {
        val prefs = dataStore.data.first()
        val packages = resolvePackages(prefs).map { it.packageName }.toSet()
        val captureAll = prefs[YapeDataStoreKeys.KEY_CAPTURE_ALL] ?: false
        val contacts = parseContacts(prefs[YapeDataStoreKeys.KEY_CONTACTS_JSON].orEmpty())
        return SettingsSnapshot(
            packages = packages,
            captureAll = captureAll,
            numbers = contacts.map { it.number }.toSet()
        )
    }

    override suspend fun setServiceEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[YapeDataStoreKeys.KEY_SERVICE_ENABLED] = enabled
        }
    }

    override suspend fun isServiceEnabledOnce(): Boolean {
        return dataStore.data.first()[YapeDataStoreKeys.KEY_SERVICE_ENABLED] ?: false
    }

    // ── JSON helpers ──

    /**
     * Resolves the current package list from prefs, migrating from legacy Set<String>
     * format if needed. Returns default Yape package if nothing is configured.
     */
    private fun resolvePackages(prefs: Preferences): List<WatchedPackage> {
        val json = prefs[YapeDataStoreKeys.KEY_PACKAGES_JSON].orEmpty()
        if (json.isNotBlank()) return parsePackages(json)

        // Migrate from legacy Set<String> format
        val legacy = prefs[YapeDataStoreKeys.KEY_PACKAGES_LEGACY].orEmpty()
        if (legacy.isNotEmpty()) {
            return legacy.map { WatchedPackage(name = "", packageName = it) }
        }

        return listOf(WatchedPackage(name = "Yape", packageName = Constants.DEFAULT_YAPE_PACKAGE))
    }

    private fun parsePackages(raw: String): List<WatchedPackage> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = ArrayList<WatchedPackage>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    WatchedPackage(
                        name = obj.optString("name", ""),
                        packageName = obj.optString("packageName", "")
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.w("SettingsRepo", "Failed to parse packages JSON", e)
            emptyList()
        }
    }

    private fun encodePackages(packages: List<WatchedPackage>): String {
        val array = JSONArray()
        packages.forEach { pkg ->
            val obj = JSONObject()
            obj.put("name", pkg.name)
            obj.put("packageName", pkg.packageName)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseContacts(raw: String): List<SmsContact> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = ArrayList<SmsContact>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    SmsContact(
                        name = obj.optString("name", ""),
                        number = obj.optString("number", "")
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.w("SettingsRepo", "Failed to parse contacts JSON", e)
            emptyList()
        }
    }

    private fun encodeContacts(contacts: List<SmsContact>): String {
        val array = JSONArray()
        contacts.forEach { contact ->
            val obj = JSONObject()
            obj.put("name", contact.name)
            obj.put("number", contact.number)
            array.put(obj)
        }
        return array.toString()
    }
}
