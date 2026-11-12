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
 * Precarga la base de alimentos desde `assets/nutrition/foods_es.csv`. Es idempotente: usa `INSERT OR IGNORE`,
 * así que repetirlo no duplica nada ni pisa favoritos o cambios del usuario. La semilla forma parte de la app
 * (no es dato de demo) y existe también con el escenario EMPTY.
 */
@Singleton
public class NutritionFoodSeeder
    @Inject
    internal constructor(
        @param:ApplicationContext private val context: Context,
        private val dao: NutritionFoodDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        private val mutex = Mutex()
        private var seeded = false

        /** Garantiza que la semilla está en Room; las llamadas posteriores en el mismo proceso no hacen nada. */
        public suspend fun ensureSeeded() {
            mutex.withLock {
                if (!seeded) {
                    val foods =
                        withContext(ioDispatcher) {
                            context.assets
                                .open(ASSET_PATH)
                                .bufferedReader()
                                .use { it.readText() }
                        }.let(::parseFoodSeedCsv)
                    dao.insertSeedIgnoring(foods)
                    seeded = true
                }
            }
        }

        private companion object {
            const val ASSET_PATH = "nutrition/foods_es.csv"
        }
    }
