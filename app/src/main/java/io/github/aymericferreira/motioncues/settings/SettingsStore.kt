package io.github.aymericferreira.motioncues.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single DataStore instance for the process. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Typed wrapper over Preferences DataStore. Exposes a single [settings] flow of the immutable
 * [Settings] snapshot plus per-field suspend writers, so the UI and the running [Settings]-driven
 * overlay observe the same source of truth.
 */
class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun setDotColor(value: Int) = put(Keys.DOT_COLOR, value)
    suspend fun setDotCount(value: Int) =
        put(Keys.DOT_COUNT, value.coerceIn(Settings.DOT_COUNT_RANGE))
    suspend fun setDotSizeDp(value: Float) =
        put(Keys.DOT_SIZE_DP, value.coerceIn(Settings.DOT_SIZE_DP_RANGE))
    suspend fun setSensitivity(value: Float) =
        put(Keys.SENSITIVITY, value.coerceIn(Settings.SENSITIVITY_RANGE))
    suspend fun setBackgroundDim(value: Float) =
        put(Keys.BACKGROUND_DIM, value.coerceIn(Settings.BACKGROUND_DIM_RANGE))
    suspend fun setPatternStyle(value: PatternStyle) = put(Keys.PATTERN, value.name)
    suspend fun setAutoDetect(value: Boolean) = put(Keys.AUTO_DETECT, value)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    private fun Preferences.toSettings(): Settings = Settings(
        dotColor = this[Keys.DOT_COLOR] ?: Settings.DEFAULT_DOT_COLOR,
        dotCount = this[Keys.DOT_COUNT] ?: Settings.DEFAULT_DOT_COUNT,
        dotSizeDp = this[Keys.DOT_SIZE_DP] ?: Settings.DEFAULT_DOT_SIZE_DP,
        sensitivity = this[Keys.SENSITIVITY] ?: Settings.DEFAULT_SENSITIVITY,
        backgroundDim = this[Keys.BACKGROUND_DIM] ?: Settings.DEFAULT_BACKGROUND_DIM,
        patternStyle = PatternStyle.fromName(this[Keys.PATTERN]),
        autoDetect = this[Keys.AUTO_DETECT] ?: Settings.DEFAULT_AUTO_DETECT,
    )

    private object Keys {
        val DOT_COLOR = intPreferencesKey("dot_color")
        val DOT_COUNT = intPreferencesKey("dot_count")
        val DOT_SIZE_DP = floatPreferencesKey("dot_size_dp")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val BACKGROUND_DIM = floatPreferencesKey("background_dim")
        val PATTERN = stringPreferencesKey("pattern_style")
        val AUTO_DETECT = booleanPreferencesKey("auto_detect")
    }
}
