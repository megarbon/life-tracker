package com.example.lifet.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.ThemeMode
import com.example.lifet.core.model.UnitSystem
import com.example.lifet.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/** Lee y escribe las preferencias globales. Los valores corruptos o desconocidos caen al valor por defecto. */
public class UserPreferencesDataSource
    @Inject
    internal constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        public val preferences: Flow<UserPreferences> =
            dataStore.data
                .catch { error ->
                    // Un fichero ilegible no debe tumbar la app: se usan los valores por defecto.
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map { it.toUserPreferences() }

        public suspend fun setCurrency(currency: CurrencyCode) {
            dataStore.edit { it[CURRENCY] = currency.value }
        }

        public suspend fun setUnitSystem(unitSystem: UnitSystem) {
            dataStore.edit { it[UNIT_SYSTEM] = unitSystem.name }
        }

        public suspend fun setThemeMode(themeMode: ThemeMode) {
            dataStore.edit { it[THEME_MODE] = themeMode.name }
        }

        public suspend fun setDynamicColor(enabled: Boolean) {
            dataStore.edit { it[DYNAMIC_COLOR] = enabled }
        }

        public suspend fun setEnabledAreas(areas: Set<Area>) {
            dataStore.edit { it[ENABLED_AREAS] = areas.map(Area::name).toSet() }
        }

        public suspend fun setOnboardingCompleted(completed: Boolean) {
            dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
        }

        private fun Preferences.toUserPreferences(): UserPreferences {
            val defaults = UserPreferences()
            return UserPreferences(
                currency = this[CURRENCY]?.let { runCatching { CurrencyCode(it) }.getOrNull() } ?: defaults.currency,
                unitSystem = enumOrDefault(this[UNIT_SYSTEM], defaults.unitSystem),
                themeMode = enumOrDefault(this[THEME_MODE], defaults.themeMode),
                dynamicColor = this[DYNAMIC_COLOR] ?: defaults.dynamicColor,
                enabledAreas =
                    this[ENABLED_AREAS]
                        ?.mapNotNull { name -> Area.entries.firstOrNull { it.name == name } }
                        ?.toSet()
                        ?: defaults.enabledAreas,
                onboardingCompleted = this[ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            )
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(
            name: String?,
            default: T,
        ): T = enumValues<T>().firstOrNull { it.name == name } ?: default

        private companion object {
            val CURRENCY = stringPreferencesKey("currency")
            val UNIT_SYSTEM = stringPreferencesKey("unit_system")
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
            val ENABLED_AREAS = stringSetPreferencesKey("enabled_areas")
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        }
    }
