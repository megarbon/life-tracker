package com.example.lifet.core.database.nutrition

/**
 * Garantiza que la base de alimentos incluida en la app está en Room. La semilla forma parte de la app (no es dato
 * de demo): existe también con el escenario EMPTY y tras borrar todos los datos.
 */
public interface NutritionFoodSeeder {
    /** Idempotente: repetirlo no duplica alimentos ni pisa favoritos o cambios del usuario. */
    public suspend fun ensureSeeded()
}
