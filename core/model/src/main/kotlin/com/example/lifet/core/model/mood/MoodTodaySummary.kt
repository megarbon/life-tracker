package com.example.lifet.core.model.mood

/**
 * Lo que la pantalla "Hoy" necesita del ánimo.
 *
 * @property averageToday media de los check-ins de hoy (1 a 5) o `null` si aún no hay ninguno.
 * @property checkInsToday número de check-ins de hoy.
 * @property weekAverage media de los últimos 7 días con registro (incluido hoy) o `null`.
 * @property lastLevel nivel del último check-in de hoy.
 * @property lowStreakDays racha más larga de días bajos seguidos en la última semana (0 si no hay).
 */
public data class MoodTodaySummary(
    public val averageToday: Double?,
    public val checkInsToday: Int,
    public val weekAverage: Double?,
    public val lastLevel: MoodLevel?,
    public val lowStreakDays: Int,
) {
    /** `true` si todavía no se ha registrado nada hoy. */
    public val missingCheckIn: Boolean get() = checkInsToday == 0

    /** `true` si procede mostrar, con delicadeza, recursos de apoyo. */
    public val showSupport: Boolean get() = lowStreakDays >= MoodRules.LOW_STREAK_DAYS
}
