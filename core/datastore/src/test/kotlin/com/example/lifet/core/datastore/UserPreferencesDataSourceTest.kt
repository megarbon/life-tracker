package com.example.lifet.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.cash.turbine.test
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.ThemeMode
import com.example.lifet.core.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UserPreferencesDataSourceTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun newStore() =
        PreferenceDataStoreFactory.create(scope = scope, produceFile = {
            File(tmp.root, "prefs.preferences_pb")
        })

    @Test
    fun `defaults are used when nothing is stored`() =
        runTest {
            val source = UserPreferencesDataSource(newStore())

            source.preferences.test {
                assertEquals(UserPreferences(), awaitItem())
            }
        }

    @Test
    fun `written values are read back`() =
        runTest {
            val source = UserPreferencesDataSource(newStore())

            source.setCurrency(CurrencyCode("USD"))
            source.setThemeMode(ThemeMode.DARK)
            source.setEnabledAreas(setOf(Area.FINANCE, Area.MOOD))
            source.setOnboardingCompleted(true)

            source.preferences.test {
                val prefs = awaitItem()
                assertEquals(CurrencyCode("USD"), prefs.currency)
                assertEquals(ThemeMode.DARK, prefs.themeMode)
                assertEquals(setOf(Area.FINANCE, Area.MOOD), prefs.enabledAreas)
                assertTrue(prefs.onboardingCompleted)
            }
        }

    @Test
    fun `corrupt stored values fall back to defaults instead of crashing`() =
        runTest {
            val store = newStore()
            store.edit {
                it[stringPreferencesKey("currency")] = "not-a-currency"
                it[stringPreferencesKey("theme_mode")] = "NEON"
            }
            val source = UserPreferencesDataSource(store)

            source.preferences.test {
                val prefs = awaitItem()
                assertEquals(CurrencyCode.EUR, prefs.currency)
                assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
            }
        }

    @Test
    fun `unknown area names are ignored`() =
        runTest {
            val store = newStore()
            store.edit { it[stringSetPreferencesKey("enabled_areas")] = setOf("FINANCE", "SLEEP") }
            val source = UserPreferencesDataSource(store)

            source.preferences.test {
                assertEquals(setOf(Area.FINANCE), awaitItem().enabledAreas)
            }
        }
}
