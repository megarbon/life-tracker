package com.example.lifet.core.database.nutrition

import android.content.Context
import com.example.lifet.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carga la semilla desde `assets/nutrition/foods_es.csv` con `INSERT OR IGNORE`. Recuerda en memoria que ya lo
 * hizo para no releer el asset en cada pantalla; tras reiniciar el proceso vuelve a intentarlo, sin efecto.
 */
@Singleton
internal class AssetNutritionFoodSeeder
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val dao: NutritionFoodDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : NutritionFoodSeeder {
        private val mutex = Mutex()
        private var seeded = false

        override suspend fun ensureSeeded() {
            mutex.withLock {
                if (!seeded) {
                    val csv =
                        withContext(ioDispatcher) {
                            context.assets
                                .open(ASSET_PATH)
                                .bufferedReader()
                                .use { it.readText() }
                        }
                    dao.insertSeedIgnoring(parseFoodSeedCsv(csv))
                    seeded = true
                }
            }
        }

        private companion object {
            const val ASSET_PATH = "nutrition/foods_es.csv"
        }
    }
