package com.dcm.music.data

import android.content.Context
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
}
