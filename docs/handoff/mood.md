# Handoff: mood (Ánimo)

Estado: listo para integrar

## Qué hay

- **Persistencia**: tabla `mood_entry` (Room). Varias entradas por día; `Instant` UTC + `ZoneId` del usuario; emociones y
  factores como claves separadas por comas (las predefinidas se traducen en la UI, las propias se muestran tal cual).
- **Pantallas** (`feature/mood`, API pública intacta: `moodGraph()` / `MoodDestination`):
  - Resumen: héroe (ánimo medio de hoy + media de 7 días), check-in rápido de 5 niveles (1 toque), tarjeta de apoyo
    (tras >= 5 días bajos en la última semana, con tono neutro y botón Ocultar), selector de rango y 3 gráficas.
  - Diario: timeline agrupado por día con la media diaria.
  - Calendario: "year in pixels" por meses con `LTCalendarHeatmap` y navegación por año.
  - FAB "Registrar" -> `LTQuickAddSheet` con nivel arriba y detalle desplegable (emociones, energía, estrés,
    factores, nota, gratitud; etiquetas propias).
  - Detalle de entrada (hoja) con Editar y Borrar (confirmación + snackbar "Deshacer").
- **Gráficas** (`LTChartCard`): ánimo con media de 7 días (LINE), emociones más frecuentes (COLUMNS) e impacto de
  factores (COLUMNS, diferencia de ánimo medio con/sin cada factor; sólo con >= 3 días a cada lado y con aviso de que
  no demuestra causa y efecto).
- **Demo**: `MoodDemoDataGenerator` (`@IntoSet` en `MoodDemoModule`). REGULAR ~6 meses: lunes más bajos, mejor con
  deporte, ola estacional suave y una racha baja de 5 días que acaba anteayer. EDGE_CASES: medianoche, cambio de hora,
  otra zona horaria, textos de 2.000 caracteres, emojis, 12 check-ins en un día y un hueco sin datos.

## Entities y DAOs (ya registrados en LifeTrackerDatabase)

- `MoodEntryEntity` (`core/database/.../mood/`) y `MoodDao` (`public`, porque lo consume `core:data`).
- `@Provides` del DAO en `core/database/.../mood/MoodDatabaseModule.kt`.
- Conflictos de merge esperables: la lista `entities = [...]` y el `abstract fun moodDao()` en `LifeTrackerDatabase.kt`
  (conservar las líneas de ambas partes). `schemas/.../1.json` se regenera: quedarse con la salida de
  `./gradlew :core:database:kspDebugKotlin` tras fusionar.

## Interfaces para otras áreas

| Pieza | Dónde | Uso |
|---|---|---|
| `ObserveMoodTodaySummary()` -> `Flow<MoodTodaySummary>` | `core:domain/.../mood/` | Tarjeta de Hoy |
| `ObserveMoodSeries(range: DateRange)` -> `Flow<MoodSeries>` | `core:domain/.../mood/` | Insights |
| `MoodStats` (funciones puras) | `core:model/.../mood/` | Correlaciones (ánimo vs deporte, etc.) |

`MoodTodaySummary`: `averageToday: Double?`, `checkInsToday`, `weekAverage: Double?`, `lastLevel`, `lowStreakDays`,
`missingCheckIn` (calculado) y `showSupport` (calculado).

`MoodSeries`: `days` (media diaria + media móvil de 7 días), `average`, `entryCount`, `recentDelta`, `topEmotions`,
`factorImpacts`. `MoodStats.dailyAverages(entries)` da el ánimo medio por día para cruzarlo con otras áreas
(los factores `sport` y `sleep` están como etiquetas en las entradas, pero la correlación real con Deporte debe hacerla
Insights con los datos de Deporte, no con la etiqueta).

## Qué debe cablear el integrador en Hoy

- Tarjeta `LTMetricCard` con `Area.MOOD`: valor `"4,5 de 5"` si `averageToday != null`; si `missingCheckIn`, texto
  "Sin registrar" y subtítulo invitando a un toque. Progreso = `averageToday / 5`.
- Strings reutilizables ya traducidas en `feature/mood` (no accesibles desde otra feature): el integrador debe
  duplicar las que necesite en `app`/`feature:dashboard` (p. ej. `mood_hero_title`, `mood_hero_missing_value`).
- `summary.showSupport == true` -> opcional: enlace discreto a la pantalla de Ánimo (NO mostrar la tarjeta de apoyo en
  Hoy; vive sólo en Ánimo).

## Acciones rápidas para Hoy

- "Ánimo": navegar a `MoodDestination`. El check-in de 1 toque está en la pestaña Resumen. Si se quiere un check-in
  directo desde Hoy sin salir de ella, haría falta un composable compartido (ver "Componentes que echo en falta").
- Recordatorio diario: **no implementado** en esta iteración (necesita WorkManager + permiso de notificaciones y
  `Settings`; sugiero que lo haga el integrador junto a los demás recordatorios usando `missingCheckIn`).

## Componentes LT* que faltan (stub local mientras tanto)

- Selector de nivel de 5 opciones con iconos (`MoodLevelPicker`, local en `feature/mood/ui`); podría subir a
  `core:designsystem` como `LTRatingPicker` si otra área lo necesita.
- Escala 1-5 opcional (`ScalePicker`, local).
- `LTCalendarHeatmap` no admite pulsar un día ni etiquetar los meses; el año se compone mes a mes con un título propio.
  Un `onDayClick` permitiría abrir el diario en ese día.
- `LTChartCard` no admite valores negativos con eje en cero bien marcado ni etiquetas largas rotadas (el impacto de
  factores usa deltas, que pueden ser negativos).

## Dependencias nuevas para el catálogo

Ninguna. Cambios en builds propios: `core:domain` ahora depende de `core:data` (api); `core:testing` expone
`core:data` (api) para el `FakeMoodRepository`; `core:demo` y `core:data` añaden dependencias de test.

## Pendiente / no verificado

- No se ha ejecutado en dispositivo ni emulador. Sin Roborazzi ni Compose UI tests (no hay infraestructura aún):
  la cobertura es de lógica (modelo, DAO, repositorio, casos de uso, ViewModel, generador de demo).
- Recordatorio diario (ver arriba) y editar/borrar categorías de emociones y factores predefinidos: las propias se
  crean al escribirlas y se sugieren después, pero no hay pantalla para renombrarlas o eliminarlas.
- Tarjeta de apoyo: el texto menciona el 024 y el 112 (España). Revisar con el criterio del usuario antes de publicar.
- Previews con datos propios del módulo (la feature no puede depender de `core:demo`, que es sólo debug).
- Migraciones: sigue VERSION = 1; al congelar el esquema, `mood_entry` debe quedar incluida con su test.
