package com.example.lifet.core.database.di

import java.io.File

private val SIDE_FILES = listOf("", "-wal", "-shm", "-journal")

/**
 * Recuperación de la base en builds de depuración cuando Room rechaza el esquema (mismo `VERSION`, otro hash).
 *
 * Nunca borra sin antes copiar: el archivo de la base y sus auxiliares se guardan como `<nombre>.bak-<marca>`
 * junto a la original, para poder recuperar los datos a mano con adb. Solo se llama para fallos de esquema,
 * no para cualquier error de apertura (un disco lleno o un bloqueo transitorio no deben vaciar la base).
 */
internal object DebugSchemaRecovery {
    /**
     * Devuelve las copias creadas; vacío si no había base que guardar. [log] recibe un aviso con la ruta de la copia
     * (en producción es `Log.w`; se inyecta para que la lógica se pueda probar sin Android).
     */
    fun backupAndDelete(
        databaseFile: File,
        stamp: Long,
        log: (String) -> Unit = {},
    ): List<File> {
        val copies = mutableListOf<File>()
        SIDE_FILES.forEach { suffix ->
            val source = File(databaseFile.path + suffix)
            if (source.exists()) {
                val copy = File("${source.path}.bak-$stamp")
                source.copyTo(copy, overwrite = true)
                copies += copy
            }
        }
        // Solo se borra tras copiar: si una copia falla, la excepción corta antes de eliminar nada.
        SIDE_FILES.forEach { suffix -> File(databaseFile.path + suffix).delete() }
        if (copies.isNotEmpty()) log("Esquema distinto: base guardada en ${copies.first().path}")
        return copies
    }
}
