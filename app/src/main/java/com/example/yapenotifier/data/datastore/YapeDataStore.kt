package com.example.yapenotifier.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "yape_datastore"
)

object YapeDataStoreKeys {
    val KEY_CONTACTS_JSON = stringPreferencesKey("sms_contacts_json")
    val KEY_PACKAGES_LEGACY = stringSetPreferencesKey("watch_packages")
    val KEY_PACKAGES_JSON = stringPreferencesKey("watch_packages_json")
    val KEY_LAST_SEEN_PACKAGE = stringPreferencesKey("last_seen_package")
    val KEY_LAST_SEEN_TEXT = stringPreferencesKey("last_seen_text")
    val KEY_CAPTURE_ALL = booleanPreferencesKey("capture_all_v2")
    val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
}
