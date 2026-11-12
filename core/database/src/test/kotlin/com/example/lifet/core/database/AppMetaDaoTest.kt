package com.example.lifet.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppMetaDaoTest {
    private lateinit var database: LifeTrackerDatabase
    private lateinit var dao: AppMetaDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, LifeTrackerDatabase::class.java).allowMainThreadQueries().build()
        dao = database.appMetaDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `unknown key emits null`() =
        runTest {
            dao.observe("missing").test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun `upsert inserts and then overwrites the value`() =
        runTest {
            dao.observe("onboarding").test {
                assertNull(awaitItem())
                dao.upsert(AppMetaEntity("onboarding", "pending"))
                assertEquals("pending", awaitItem())
                dao.upsert(AppMetaEntity("onboarding", "done"))
                assertEquals("done", awaitItem())
            }
        }
}
