# Handoff: nutrition (Alimentación)

Estado: listo para integrar

Qué hay: Diario del día, Buscar/añadir alimento, Detalle de alimento con selector de ración, alimento propio,
Objetivos y Tendencias; todo persistido en Room, con ~178 alimentos de semilla y datos de demo.

## Boceto del Diario

```
┌──────────────────────────────┐
│ Comida                  ↗  ⚑ │  LTTopAppBar (Tendencias, Objetivos)
├──────────────────────────────┤
│ <   Hoy                   >  │  navegación entre días (">" desactivado en hoy)
│                              │
│        ┌─────────┐           │
│        │  1.540  │           │  LTProgressRing de kcal
│        │de 2.000 │           │
│        │  kcal   │           │
│        └─────────┘           │
│       Te quedan 460 kcal     │
│ Proteínas ▓▓▓▓▓░░  82 de 110 │  barras de macros
│ Hidratos  ▓▓▓▓▓▓░ 190 de 240 │
│ Grasas    ▓▓▓▓░░░  48 de 65  │
│ Desayuno            420 kcal +│  una tarjeta por comida
│  Pan blanco · 80 g   212 kcal│  (tocar = editar, papelera = borrar)
│ Comida / Snack / Cena  …     │
│ Agua  ▓▓▓▓░░░  1.000 de 2.000│  [ +250 ml ]  [ - ]
│ [Repetir ayer]               │
│ Últimos 7 días (LTChartCard) │  columnas de kcal
└──────────────────────────────┘
                 [+ Añadir comida]   FAB -> LTQuickAddSheet (recientes, comida, buscar, repetir ayer, agua)
```

## Entities y DAOs (para registrar en LifeTrackerDatabase)

Ya registrados en `core/database/.../LifeTrackerDatabase.kt` (una línea por entity y un `abstract fun` por DAO):

- Entities: `NutritionFoodEntity`, `NutritionMealEntity`, `NutritionWaterEntity`, `NutritionGoalsEntity`.
- DAOs: `nutritionFoodDao()`, `nutritionMealDao()`, `nutritionWaterDao()`, `nutritionGoalsDao()`.
- Hilt: `core/database/.../nutrition/di/NutritionDatabaseModule.kt` (DAOs + `NutritionFoodSeeder`) y
  `core/data/.../nutrition/di/NutritionDataModule.kt` (repositorios) y
  `core/demo/.../nutrition/di/NutritionDemoModule.kt` (`@IntoSet`).
- Semilla: `core/database/src/main/assets/nutrition/foods_es.csv` (178 alimentos, separador `;`). Se carga con
  `INSERT OR IGNORE` la primera vez que se observa la base de alimentos; existe también con el escenario EMPTY.
- `schemas/1.json` se ha regenerado (VERSION sigue en 1). Al fusionar con otras áreas, regenerar con
  `./gradlew :core:database:kspDebugKotlin` si hay conflicto en ese JSON.

## Componentes LT* que faltan (stub local mientras tanto)

- Gráfica de macros en barras: `MacroSplitCard` es un stub local (tres `LinearProgressIndicator`). Si se quiere una
  barra apilada, haría falta un `LTStackedBar` en el design system.
- `LTChartCard` no dibuja una línea de objetivo: el objetivo va en el titular y en el resumen. Ayudaría un parámetro
  `referenceLine: Double?`.
- `LTChipGroup.label` no es `@Composable`, así que las etiquetas se resuelven fuera con `associateWith`.
- `LTChartCard` no admite series con huecos: los días sin registrar se dibujan como 0 en las columnas.

## Navegación (rutas nuevas o acciones rápidas para Hoy)

- API pública sin cambios: `NutritionDestination` y `NavGraphBuilder.nutritionGraph()`. Dentro hay un `NavHost`
  anidado (Diario, Buscar, Detalle, Alimento propio, Objetivos, Tendencias), así la app no tiene que cambiar.
- Acciones rápidas sugeridas para Hoy (hoy requieren navegar a `NutritionDestination`; no hay deep link todavía):
  1. "+250 ml de agua": llamar a `AddWater` (core:domain) directamente desde Hoy y mostrar "Deshacer" con
     `ManageDiaryEntries.deleteWater`.
  2. "Añadir comida": navegar a `NutritionDestination`.
  3. "Repetir ayer": `RepeatYesterdayMeals(date)`.

## Dependencias nuevas para el catálogo

Ninguna. `core:domain` ahora depende de `core:data` (para las interfaces de repositorio) y `core:testing` depende de
`core:data` (para los fakes de Alimentación en `core/testing/.../nutrition/`).

## Interfaces para otras áreas (Observe<Area>TodaySummary, Observe<Area>Series)

- `ObserveNutritionTodaySummary()`: `Flow<NutritionTodaySummary>` con `consumed: Nutrients`, `goals`, `waterMl`,
  `mealCount`, y los derivados `kcalProgress`, `remainingKcal`, `waterProgress`. "Hoy" se fija al empezar a
  recolectar: la pantalla Hoy debe volver a recolectar al cambiar de día.
- `ObserveNutritionSeries(range: DateRange)` y `ObserveNutritionSeries(start, endExclusive)`:
  `Flow<NutritionSeries>` con un `DayTotals` por día (los días sin registrar tienen `hasMeals = false`),
  `averageKcal` (sólo días registrados), `averageMacroSplit`, `averageWaterMl` y `rollingAverageKcal(window)`.
  Para Insights: no mezclar "día sin registrar" con "0 kcal".
- Otros casos de uso públicos: `ObserveNutritionDay`, `ObserveNutritionDiary`, `LogFood`, `AddWater`,
  `RepeatYesterdayMeals`, `ManageDiaryEntries`.

## Demo

- `NutritionDemoDataGenerator` (`@IntoSet`): EMPTY (diario vacío, semilla intacta), NEW_USER (3 días), REGULAR
  (180 días, ~6 % de días sin registrar, desayunos repetitivos, fines de semana ~+40 % de ración, agua irregular,
  objetivos propios, 2 alimentos propios y 4 favoritos) y EDGE_CASES (cena 23:59 y snack 00:01, el día del último
  cambio de hora de la zona, ración de 5 kg y de 1 g, nombres larguísimos, emojis, 0 kcal, 9,9 L de agua, un día
  sin datos entre días llenos y objetivos en el extremo del rango).
- Verificado con tests: media de REGULAR ~1.920 kcal/día frente a un objetivo de 2.000.

## Pendiente / no verificado

- No se ha ejecutado la app en un dispositivo ni se han lanzado los tests instrumentados.
- Sin tests de Compose UI ni Roborazzi (el brief indica omitirlos); hay `@LTPreviews` de todas las pantallas
  (Diario: normal, vacío, límite, cargando, error).
- Sin test de migración: el esquema sigue en VERSION 1 y se recrea en debug, según las reglas de la iteración.
- Health Connect (`NutritionRecord`, `HydrationRecord`), código de barras y comidas guardadas (plantillas) quedan
  fuera de esta entrega.
- Las etiquetas del eje X de la tendencia ("d/M") pueden solaparse con fuente al 200 %; se ve mejor la tabla
  accesible ("Ver como tabla").
- `docs/handoff/nutrition.md`: boceto ASCII orientativo.
