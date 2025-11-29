package com.snapswipe.app.data

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
import com.snapswipe.app.data.InteractionMode
import com.snapswipe.app.data.ThemeMode

private const val PREFERENCES_NAME = "snap_swipe_prefs"
private val Context.dataStore by preferencesDataStore(name = PREFERENCES_NAME)

class SortOrderPreferences(private val context: Context) {

    private val sortOrderKey = stringPreferencesKey("photo_sort_order")
    private val deleteModeKey = stringPreferencesKey("delete_mode")
    private val instructionsSeenKey = stringPreferencesKey("instructions_seen")
    private val interactionModeKey = stringPreferencesKey("interaction_mode")
    private val lastSeenVersionKey = stringPreferencesKey("last_seen_version")
    private val themeModeKey = stringPreferencesKey("theme_mode")

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

    val deleteModeFlow: Flow<DeleteMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.w(TAG, "Delete mode preferences read failed; using defaults", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[deleteModeKey]?.let { stored ->
                runCatching { DeleteMode.valueOf(stored) }.getOrNull()
            } ?: DeleteMode.IMMEDIATE
        }

    val instructionsSeenFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.w(TAG, "Instructions flag read failed; using default", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[instructionsSeenKey]?.toBooleanStrictOrNull() ?: false
        }

    val interactionModeFlow: Flow<InteractionMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.w(TAG, "Interaction mode read failed; using default", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[interactionModeKey]?.let { stored ->
                runCatching { InteractionMode.valueOf(stored) }.getOrNull()
            } ?: InteractionMode.SWIPE_TO_CHOOSE
        }

    val lastSeenVersionFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.w(TAG, "Last seen version read failed; using default", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[lastSeenVersionKey] }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.w(TAG, "Theme mode read failed; using default", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[themeModeKey]?.let { stored ->
                runCatching { ThemeMode.valueOf(stored) }.getOrNull()
            } ?: ThemeMode.SYSTEM
        }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { prefs ->
            prefs[sortOrderKey] = order.name
        }
    }

    suspend fun setDeleteMode(mode: DeleteMode) {
        context.dataStore.edit { prefs ->
            prefs[deleteModeKey] = mode.name
        }
    }

    suspend fun setInstructionsSeen(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[instructionsSeenKey] = seen.toString()
        }
    }

    suspend fun setInteractionMode(mode: InteractionMode) {
        context.dataStore.edit { prefs ->
            prefs[interactionModeKey] = mode.name
        }
    }

    suspend fun setLastSeenVersion(version: String) {
        context.dataStore.edit { prefs ->
            prefs[lastSeenVersionKey] = version
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    private companion object {
        private const val TAG = "SortOrderPreferences"
    }
}
