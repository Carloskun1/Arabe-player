package com.dcm.music.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "dcm_settings")

class Prefs(private val context: Context) {

    private val sortKey = stringPreferencesKey("sort_by")

    val sort: Flow<SortBy> = context.dataStore.data.map { prefs ->
        runCatching { SortBy.valueOf(prefs[sortKey] ?: SortBy.NAME.name) }.getOrDefault(SortBy.NAME)
    }

    suspend fun setSort(value: SortBy) {
        context.dataStore.edit { it[sortKey] = value.name }
    }

    private val premiumKey = booleanPreferencesKey("premium")

    val premium: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[premiumKey] ?: false }

    suspend fun setPremium(value: Boolean) {
        context.dataStore.edit { it[premiumKey] = value }
    }
}
