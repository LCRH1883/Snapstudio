package com.example.snapswipe.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val PREFERENCES_NAME = "snap_swipe_prefs"
private val Context.dataStore by preferencesDataStore(name = PREFERENCES_NAME)

class SortOrderPreferences(private val context: Context) {

    private val sortOrderKey = stringPreferencesKey("photo_sort_order")

    val sortOrderFlow: Flow<SortOrder> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.w(TAG, "Sort order preferences read failed; using defaults", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[sortOrderKey]?.let { stored ->
                runCatching { SortOrder.valueOf(stored) }.getOrNull()
            } ?: SortOrder.NEWEST_FIRST
        }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { prefs ->
            prefs[sortOrderKey] = order.name
        }
    }

    private companion object {
        private const val TAG = "SortOrderPreferences"
    }
}
