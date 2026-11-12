# Handoff: fitness (Deporte)

Estado: listo para integrar

Rama `feat/fitness-complete`. Todo el área funciona de extremo a extremo (Room, repositorio, casos de uso, ViewModel, UI,
gráficas, demo y tests). La navegación existente (`fitnessGraph()` / `FitnessDestination`) no cambia.

## Pantalla (boceto)

```
┌ Deporte ────────────────────────────┐
│ Resumen│Historial│Ejercicios│Cuerpo │
├─────────────────────────────────────┤
│ Minutos activos esta semana         │  héroe (LTMetricCard) con progreso
│  96 min                             │
│  de 150 min recomendados por la OMS │
│  Te faltan 54 min para el objetivo. │
│ [Peso 80,1 kg][Pasos 6.420][Récords]│  LTStatTile
│ ┌ Minutos activos por semana ─────┐ │  LTChartCard COLUMNS (12 semanas)
│ ┌ Actividad de las últimas semanas┐ │  LTCalendarHeatmap (3 meses)
│ Repetir un entreno reciente         │  chips: recientes + plantillas
│ Récords recientes                   │
└─────────────────────────────────────┘      [ + Registrar ]  FAB -> LTQuickAddSheet
```

- **Historial**: entrenos agrupados por semana (lunes a domingo) con los minutos de cada una; tocar una fila edita;
  borrar muestra "Deshacer". Los que baten un récord llevan una copa y la palabra "Récord".
- **Ejercicios**: lista de ejercicios de fuerza (último primero). Tocar uno abre su detalle: gráfica de progresión
  (1RM estimado en kg, o repeticiones si nunca se usó carga), últimas sesiones y récords batidos.
- **Cuerpo**: gráfica de peso con media móvil de 7 días (`LTChartCard` LINE), lista de pesos y pasos manuales. Un peso y
  unos pasos por día; guardar otro el mismo día lo sustituye.
- **Registrar entreno** (`LTQuickAddSheet`): arriba, los entrenos recientes como chips (un toque rellena todo) y las
  plantillas; debajo tipo, título, duración, distancia, kcal, ejercicios con series `reps x kg` (kg vacío = peso
  corporal), fecha y hora, nota y "Guardar como plantilla". Un entreno nuevo sale "acabando ahora": la hora de inicio se
  recalcula sola al teclear la duración hasta que se toca a mano.

## Entities y DAOs (YA registrados en LifeTrackerDatabase)

Registrado con líneas mínimas (nombres totalmente cualificados) en `entities = [...]` y `abstract fun fitnessDao()`.
No requiere acción: sólo conservar ambas listas al resolver conflictos de merge.

- `fitness_workouts`, `fitness_sets` (FK en cascada al entreno), `fitness_weights` (PK = fecha), `fitness_steps`
  (PK = fecha), `fitness_templates` y `fitness_template_exercises` (FK en cascada a la plantilla).
- Todo en SI y enteros: segundos, metros, gramos. Nada de `Double` en un dato guardado. Instante UTC + `ZoneId` por entreno.
- `FitnessDao` (público) y `FitnessDatabaseModule` (`core/database/.../fitness/di/`) con su `@Provides`.
- `schemas/.../1.json` se regenera con el esquema; si hay conflicto con otras áreas, regenerarlo tras el merge
  (`./gradlew :core:database:kspDebugKotlin`).

## Hoy: qué cablear

`ObserveFitnessTodaySummary` (core:domain, `com.example.lifet.core.domain.fitness`) emite `FitnessTodaySummary`:

| Campo | Uso sugerido en `LTMetricCard` |
|---|---|
| `weekActiveMinutes` | valor héroe ("96 min") |
| `weeklyGoalMinutes` | contexto ("de 150 min recomendados") |
| `weekProgress` (0 a 1) | progreso de la tarjeta |
| `goalReached` | tono del subtítulo (nunca rojo: sólo se felicita) |
| `workedOutToday` + `todayActiveMinutes` | subtítulo ("Hoy has entrenado 40 min" / "Hoy aún no has entrenado") |
| `latestWeightGrams` + `latestWeightDate` | dato secundario (`null` si no hay) |
| `stepsToday` | dato secundario (`null` si no se anotaron) |
| `hasData` | `false` -> estado vacío que invita a registrar el primer entreno |

Icono y color: `Area.FITNESS.icon` y `areaColors.fitness`. Los gramos se pasan a kg dividiendo con `BigDecimal`, no con
`Double` (hay un formateador de ejemplo en `feature/fitness/FitnessFormatters.kt`).

## Insights

`ObserveFitnessSeries(range: DateRange)` emite `FitnessSeries` con un `FitnessDayPoint` por día hasta hoy
(`activeMinutes`, `workouts`, `steps?`, `weightGrams?`). Los días sin actividad van con cero, no se omiten. Un entreno
que cruza la medianoche reparte sus minutos entre los días que toca (con tiempo real, así que un cambio de hora no lo
desajusta). `FitnessSeries.weights` lleva el peso con su media móvil de 7 días.

Para cruzar con Ánimo (ánimo vs deporte) usar `FitnessDayPoint.activeMinutes > 0` por día; la etiqueta `sport` de Ánimo no
sustituye a esto.

## Acciones rápidas para Hoy

- "Entreno": abrir la ruta de Deporte (`FitnessDestination`); el FAB abre la hoja de alta. Abrir la hoja directamente
  necesitaría un argumento de navegación (`FitnessDestination(openAdd = true)`): no lo añadí para no cambiar la API
  pública de `feature/fitness` que usa `app`.

## Health Connect (fuera de esta iteración, interfaz lista)

`ActivitySource` (core:data, `com.example.lifet.core.data.fitness`) es la costura: `status()`, `workouts(from, to)` y
`steps(from, to)` devuelven `AppResult`. Hoy está enlazada a `UnavailableActivitySource` (siempre `UNAVAILABLE`, sin
datos) en `FitnessDataModule`. Una implementación real sustituye ese `@Binds` y sincroniza hacia `FitnessRepository`
marcando `RecordSource.HEALTH_CONNECT` y `externalId` (la entity ya tiene índice en `externalId` para deduplicar).
Los registros manuales conservan `RecordSource.MANUAL` y editarlos no cambia su origen.

## Demo

`FitnessDemoDataGenerator` registrado con `@IntoSet` en `core/demo/.../fitness/di/FitnessDemoModule`.

- **EMPTY**: sin datos. **NEW_USER**: paseo, primer día de fuerza y primera carrera hoy, dos pesos y unos pasos.
- **REGULAR**: 26 semanas con empuje / tirón / pierna (3 sesiones) y un cardio de fin de semana la mayoría de las semanas
  (3 o 4 entrenos por semana), cargas que suben con una semana de descarga cada ocho, una semana de vacaciones sin
  actividad (con la fuerza algo resentida al volver), peso con tendencia suave a la baja (de ~82,2 a ~79,9 kg), pasos
  manuales y 4 plantillas. Hay semanas que cumplen los 150 min y otras que no; ~70 récords batidos.
- **EDGE_CASES**: entrenos que cruzan la medianoche y el cambio de hora, otra zona (Tokio), títulos y notas en su longitud
  máxima con emojis, 30 ejercicios y uno con 50 series, cargas y repeticiones en el límite, ocho entrenos en un día, uno de
  24 horas, el mismo ejercicio escrito de dos maneras (se une), pesos extremos y un hueco de doce días sin datos.

## Componentes LT* que faltan (stub local mientras tanto)

- `LTCalendarHeatmap` sólo dibuja un mes y no admite pulsar un día ni etiquetar el mes: el calendario de actividad se
  compone mes a mes con un título propio (3 meses). Un `onDayClick` abriría el entreno de ese día.
- `LTChartCard` sólo admite una serie: no se puede pintar el peso diario junto a su media móvil (se dibuja sólo la media,
  un punto por semana, para que el eje X no se amontone), ni marcar el objetivo de 150 min como línea de referencia en la
  gráfica semanal.
- Campos de formulario (`NumberField`, `TextInput`, `DateField`, `TimeField`) locales en `feature/fitness/ui/FormFields.kt`.
  Candidatos a `LTNumberField` / `LTDateField` / `LTTimeField` (Finanzas tiene sus propios `DateField` y `NameField`).
- Fila de lista con icono, subtítulo y dato a la derecha: local (`WorkoutListItem`); Finanzas tiene `TransactionListItem`.

## Dependencias nuevas para el catálogo

Ninguna. `core:testing` expone `InMemoryFitnessRepository` (ya dependía de `core:data`).

## Decisiones

- **Minutos activos** = minutos de cualquier entreno (fuerza o cardio). Un día cuenta como activo si tiene algún minuto.
- **Récords automáticos**: se compara el mejor 1RM estimado de cada sesión (fórmula de Epley, `carga x (1 + reps / 30)`) con
  la mejor marca anterior del mismo ejercicio; con peso corporal se compara la mejor serie en repeticiones. La primera vez
  que se hace un ejercicio no cuenta como récord (no hay marca previa). "Press banca" y "press  banca " son el mismo.
- **Unidades**: la UI muestra kg y km. `UnitSystem` existe en `core:model` pero sólo `app` depende de `core:datastore`, así
  que la UI no lo lee todavía: el integrador debe cablearlo (los datos ya están en SI, sólo falta formatear).
- **Peso corporal / cargas sin decimales flotantes**: se teclea en kg (hasta 3 decimales) y se guarda en gramos con
  `BigDecimal`; un valor con más decimales de los admitidos se rechaza, nunca se redondea en silencio.
- **Hora de inicio**: el formulario coloca el entreno "acabando ahora" (inicio = ahora - duración) mientras no se toque la
  hora, que es lo natural al registrar un entreno recién terminado.
- Borrar un entreno borra sus series (cascada). "Deshacer" lo vuelve a guardar entero.

## Pendiente / no verificado

- No se ha ejecutado en dispositivo ni emulador: sólo compilación, lint y tests JVM.
- Sin tests de UI Compose ni Roborazzi (no hay infraestructura todavía); sí `@LTPreviews` en las pantallas y hojas
  (resumen, historial, ejercicios, cuerpo, vacío, cargando, error, hoja de registro y detalle de ejercicio), pero no se han
  revisado visualmente.
- Sin test de migración: el esquema está en VERSION = 1 (la base se recrea en debug).
- Los **objetivos** (150 min, 8.000 pasos) son constantes de `FitnessRules`; falta una pantalla de Ajustes para cambiarlos.
- No hay editor de plantillas: se crean con "Guardar como plantilla" al registrar y se borran con una acción pendiente en la
  UI (el borrado y su "Deshacer" ya existen en el writer/ViewModel, falta el punto de entrada).
- `AppMetaDaoTest > unknown key emits null` (del esqueleto, no de Deporte) falló una vez con un timeout de 3 s de Turbine al
  arrancar Robolectric en frío con otro Gradle compilando; aislado y en la segunda ejecución completa pasa.
