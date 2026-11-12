---
name: life-tracker-domain
description: Modelo de dominio y reglas de negocio de Life Tracker (finanzas, deporte, comida, sueño, objetivos, insights) e integración con Health Connect. Úsala al diseñar entidades, tablas Room, cálculos, objetivos, métricas o cualquier lógica específica de un área.
---

# Dominio de Life Tracker

## Conceptos transversales

- **Entry**: todo registro tiene `id: String (UUID)`, `createdAt: Instant`, `updatedAt: Instant`, `source: Source` (`MANUAL`, `HEALTH_CONNECT`, `IMPORT`), `note: String?` y, si viene de HC, `externalId` para deduplicar.
- **Día lógico**: un día va de 00:00 a 23:59 en la zona del usuario, **excepto sueño**, que se asigna al día en que te despiertas.
- **Goal**: `area`, `metric`, `target`, `period` (DAILY/WEEKLY/MONTHLY), `direction` (AT_LEAST / AT_MOST). Progreso = función pura testeable.
- **Streak**: días consecutivos cumpliendo un objetivo; un día sin datos rompe la racha sólo cuando termina.
- **Unidades**: guardar siempre en SI (metros, gramos, kcal, segundos, ml); convertir sólo en UI según preferencias.
- Borrado lógico (`deletedAt`) para permitir "Deshacer" en la snackbar.

## Finanzas

- `Money(amountMinor: Long, currency: CurrencyCode)` — nunca `Double`. Operaciones sólo entre la misma divisa.
- `Account` (efectivo, banco, tarjeta…), `Category` (jerárquica, con icono y color, tipo INCOME/EXPENSE), `Transaction` (importe con signo según tipo, fecha, cuenta, categoría, nota, etiquetas), `Transfer` (entre cuentas, no cuenta como gasto), `Budget` (categoría + periodo + límite), `RecurringRule` (suscripciones, nómina).
- Métricas: balance, gasto por categoría, presupuesto restante, tasa de ahorro, comparación con meses anteriores, previsión de fin de mes.
- Edge cases: reembolsos, transacciones en otra divisa, cambio de mes, presupuesto sin gasto, categorías borradas con transacciones.
- Futuro: import CSV del banco; nunca pedir credenciales bancarias.

## Deporte / actividad

- `Workout(type, start, end, durationSec, distanceM?, energyKcal?, avgHr?, notes)`, `DailyActivity(steps, distanceM, activeKcal, activeMinutes)`, `BodyMeasurement(weightKg, bodyFatPct?, …)`.
- Opcional: `StrengthSet(exercise, reps, weightKg, rpe?)` agrupado en `Workout`.
- Fuente principal: Health Connect (`StepsRecord`, `ExerciseSessionRecord`, `DistanceRecord`, `ActiveCaloriesBurnedRecord`, `WeightRecord`, `HeartRateRecord`). Registro manual siempre disponible.
- Métricas: pasos vs objetivo, minutos activos semanales (referencia OMS: 150 min moderados), volumen de entrenamiento, récords personales.

## Comida / nutrición

- `FoodItem(name, brand?, per100g: Nutrients, servingSizes)`, `Meal(type: BREAKFAST/LUNCH/DINNER/SNACK, time, items: List<MealItem(food, grams)>)`, `Nutrients(kcal, proteinG, carbsG, fatG, fiberG?, sugarG?, sodiumMg?)`, `WaterIntake(ml)`.
- Alta rápida: recientes, favoritos, "repetir comida de ayer", escaneo de código de barras (ML Kit) + Open Food Facts como fuente opcional.
- Métricas: kcal y macros vs objetivo, distribución por comida, agua diaria.
- Sincronizar con HC `NutritionRecord` y `HydrationRecord` (opt-in).
- Sin lenguaje de dieta restrictiva; los objetivos los define el usuario.

## Sueño

- `SleepSession(start, end, stages: List<SleepStage(type, start, end)>?, quality: 1..5?, note)`; tipos de fase: AWAKE, LIGHT, DEEP, REM, UNKNOWN.
- Fuente: HC `SleepSessionRecord`; registro manual con hora de acostarse/levantarse.
- Métricas: duración total, eficiencia (dormido / en cama), regularidad (desviación de la hora de acostarse), deuda de sueño vs objetivo, media 7 días.
- Edge cases: sesiones que cruzan medianoche, siestas (sesiones múltiples en un día), solapamientos entre fuentes (priorizar la de mayor detalle y deduplicar), cambio de horario de verano.

## Insights (correlaciones)

Funciones puras en `core:domain` que crucen áreas: sueño vs pasos del día siguiente, gasto en comida fuera vs kcal, entrenos vs calidad de sueño. Mostrar sólo con datos suficientes (≥ 14 días) y en lenguaje prudente ("parece que…"), nunca como consejo médico o financiero.

## Health Connect

- Comprobar disponibilidad (`HealthConnectClient.getSdkStatus`) y ofrecer instalar/actualizar si falta.
- Pedir permisos por área sólo al activarla, con pantalla de explicación propia; declarar la activity de política de privacidad requerida.
- Sync incremental con *changes tokens*, en `WorkManager` periódico + al abrir la app. Room sigue siendo la fuente de verdad de la UI.
- Escribir en HC sólo lo que el usuario registre manualmente y si ha dado permiso de escritura.

## Privacidad y datos

- Todo local; backup/export a JSON/CSV cifrado elegido por el usuario. Considerar SQLCipher o cifrado de backup.
- `android:allowBackup` configurado con reglas explícitas (`dataExtractionRules`).
- Bloqueo opcional con biometría (`BiometricPrompt`).
- Nada de datos de salud o finanzas en logs ni en crash reports.
