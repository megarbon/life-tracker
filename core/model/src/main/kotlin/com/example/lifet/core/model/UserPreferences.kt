package com.example.lifet.core.model

/** Tema visual elegido por el usuario. */
public enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Sistema de unidades para peso y distancia. Los datos siempre se guardan en SI. */
public enum class UnitSystem { METRIC, IMPERIAL }

/**
 * Preferencias globales del usuario.
 *
 * [enabledAreas] decide qué tarjetas y pestañas se muestran; [onboardingCompleted] evita repetir la bienvenida.
 */
public data class UserPreferences(
    val currency: CurrencyCode = CurrencyCode.EUR,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val enabledAreas: Set<Area> = Area.entries.toSet(),
    val onboardingCompleted: Boolean = false,
)
