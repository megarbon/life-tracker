package com.example.lifet.core.database.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DebugSchemaRecoveryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun database(vararg suffixes: String): File {
        val db = folder.newFile("life-tracker.db").also { it.writeText("datos reales") }
        suffixes.forEach { File(db.path + it).writeText("aux$it") }
        return db
    }

    @Test
    fun `the database and its side files are copied before being deleted`() {
        val db = database("-wal", "-shm")

        val copies = DebugSchemaRecovery.backupAndDelete(db, stamp = 42L)

        assertEquals(3, copies.size)
        assertEquals("datos reales", File(db.path + ".bak-42").readText())
        assertEquals("aux-wal", File(db.path + "-wal.bak-42").readText())
        assertFalse(db.exists())
        assertFalse(File(db.path + "-wal").exists())
    }

    @Test
    fun `nothing is created when there is no database to save`() {
        val missing = File(folder.root, "life-tracker.db")

        val copies = DebugSchemaRecovery.backupAndDelete(missing, stamp = 1L)

        assertTrue(copies.isEmpty())
        assertTrue(
            folder.root
                .listFiles()
                .orEmpty()
                .isEmpty(),
        )
    }

    @Test
    fun `an earlier backup is never overwritten by a later one with another stamp`() {
        val db = database()
        DebugSchemaRecovery.backupAndDelete(db, stamp = 1L)
        db.writeText("segunda base")

        DebugSchemaRecovery.backupAndDelete(db, stamp = 2L)

        assertEquals("datos reales", File(db.path + ".bak-1").readText())
        assertEquals("segunda base", File(db.path + ".bak-2").readText())
    }
}
