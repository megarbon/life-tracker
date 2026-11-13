# Handoff: work (Trabajo)

Estado: listo para integrar

Rama `feat/work-complete`. El área funciona de extremo a extremo (Room, repositorio, casos de uso, ViewModel, UI,
gráficas, demo y tests). La navegación existente (`workGraph()` / `WorkDestination`) no cambia.

## Pantalla (boceto)

```
┌ Trabajo ──────────────────────── ⚙ ┐
│ Hoy │ Foco │ Semana │ Proyectos    │
├─────────────────────────────────────┤
│ ▣ Jornada en curso                  │  héroe
│   Desde las 08:45                   │
│   03:25:10                          │  cronómetro en vivo (sólo su texto se recompone)
│   Hoy llevas 3 h 25 min             │
│   Esta semana: 19 h de 40 h         │
│   Saldo de la semana: +1 h 10 min   │
│   [ Pausa ]        [ Salida ]       │  botones grandes (Entrada / Pausa|Reanudar / Salida)
│ Falta la salida (jornada olvidada)  │  tarjeta si hay jornadas abiertas de días anteriores
│ JORNADAS DE HOY            Añadir   │
│ ● 08:45 – En curso · 3 h 25 min     │
│ Nota del día                        │
│ ┌ Horas por día ──────────────────┐ │  LTChartCard COLUMNS
│ ┌ Saldo de horas acumulado ───────┐ │  LTChartCard LINE
└─────────────────────────────────────┘      [ + Añadir jornada ]  FAB
```

- **Hoy**: héroe con cronómetro, botones Entrada / Pausa (Reanudar) / Salida, aviso de jornadas sin cerrar, jornadas de hoy
  (incluida una nocturna que empezó ayer y sigue abierta), nota del día y las dos gráficas.
- **Foco**: temporizador tipo pomodoro con `LTProgressRing`. Elegir tipo (foco, descanso corto o largo), minutos,
  título y proyecto. Sugiere el siguiente intervalo (descanso largo cada cuatro focos). Muestra lo enfocado hoy y las
  sesiones recientes.
- **Semana**: navegación entre semanas (no deja ir al futuro), total frente al objetivo, saldo, las horas de cada día
  (tocar un día abre una jornada **de ese día**) y tres gráficas: horas por día, saldo acumulado y reparto por proyecto.
- **Proyectos**: lista con color, horas de la semana, totales y foco; alta, edición y borrado con "Deshacer". El color
  es de una paleta cerrada de diez y siempre va acompañado de su nombre.
- **Ajustes** (icono en la barra superior): objetivo semanal en horas (admite un decimal: "37,5") y días laborables.
- Cerrar a posteriori una jornada olvidada: tarjeta "Falta la salida" -> hoja con día y hora de salida.

## Entities y DAOs (YA registrados en LifeTrackerDatabase)

Registrado con líneas mínimas (nombres totalmente cualificados) en `entities = [...]` y `abstract fun workDao()`. No
requiere acción: sólo conservar ambas listas al resolver conflictos de merge.

- `work_shifts`, `work_breaks` (FK en cascada a la jornada), `work_projects`, `work_focus_sessions`, `work_day_notes`
  (PK = fecha) y `work_settings` (una única fila, `id = 1`).
- Todo el tiempo es `Instant` + `ZoneId`; las duraciones, segundos enteros. Nada de `Double` en un dato guardado.
- `work_shifts.projectId` y `work_focus_sessions.projectId` **no llevan clave foránea a propósito**: borrar un proyecto
  conserva sus jornadas (pasan a "Sin proyecto") y "Deshacer" lo recupera con todas sus horas.
- `WorkDao` (público) y `WorkDatabaseModule` (`core/database/.../work/di/`) con su `@Provides`.
- `schemas/.../1.json` se regenera con el esquema; si hay conflicto con otras áreas, regenerarlo tras el merge
  (`./gradlew :core:database:kspDebugKotlin`).

## Hoy: qué cablear

`ObserveWorkTodaySummary` (core:domain, `com.example.lifet.core.domain.work`) emite `WorkTodaySummary`:

| Campo | Uso sugerido en `LTMetricCard` |
|---|---|
| `isClockedIn` + `shiftStart` | héroe: "Jornada en curso desde las 08:45" / "Sin fichar" |
| `activeShiftSeconds` | cifra héroe (cronómetro de la jornada) |
| `todaySeconds` | "Hoy llevas 3 h 25 min" |
| `weekSeconds` / `weeklyTargetSeconds` / `weekProgress` | contexto y progreso ("19 h de 40 h") |
| `weekBalanceSeconds` | saldo ("+1 h 10 min"; el signo va en el texto, nunca sólo en el color) |
| `totalBalanceSeconds` | saldo acumulado de todo el histórico |
| `onBreak` | "En pausa" (el cronómetro no avanza) |
| `needsClosing` | aviso amable: "Falta la salida de una jornada" |
| `hasData` | `false` -> estado vacío que invita a fichar la primera entrada |

**Cronómetro vivo sin recomponer Hoy**: el resumen no vuelve a emitir cada segundo. Lleva su instante `asOf` y expone
`shiftSecondsAt(now)`, `todaySecondsAt(now)`, `weekSecondsAt(now)` y `weekBalanceSecondsAt(now)`; en la tarjeta de Hoy
basta una hoja que lea un "ahora" que avance cada segundo y llame a esas funciones (ver `LiveClock.kt` y
`TodayHero.LiveStopwatch` en `feature/work` como ejemplo; `rememberLiveNow(enabled)` se detiene con la app en segundo
plano y no corre si no hay jornada en marcha). Si la tarjeta no necesita segundos, basta mostrar `todaySeconds` y se
refresca con cada escritura.

Icono y color: `Area.WORK.icon` y `areaColors.work`.

## Insights

`ObserveWorkSeries(range: DateRange)` emite `WorkSeries` con un `WorkDayPoint` por día hasta hoy (`workedSeconds`,
`targetSeconds`, `balanceSeconds`, `shifts`, `focusSeconds`). Los días sin jornada van con cero, no se omiten. Una
jornada que cruza la medianoche cuenta **entera en el día en que empezó**; todo se mide con instantes reales, así que un
cambio de hora no la desajusta.

Para "ánimo vs. horas de trabajo": usar `WorkDayPoint.workedSeconds` por día. El día en curso aparece con lo trabajado
hasta ahora; filtrar el día de hoy si se quiere comparar sólo con días completos.

También existen `ObserveWorkOverview` y `ObserveWorkWeek(week)` (los usan las pantallas de Trabajo).

## Acciones rápidas para Hoy

`ManageWorkClock` (core:domain) permite fichar sin abrir la pantalla de Trabajo: `clockIn(projectId)`, `startBreak()`,
`endBreak()` y `clockOut()`. Devuelven `AppResult` y rechazan con `AppError.Validation` (por ejemplo, `clockIn` con una
jornada ya en curso). Es un **singleton**: las acciones de fichaje de Hoy y las de Trabajo comparten un único cerrojo, así
que un doble toque desde dos sitios no puede crear dos jornadas.

- "Fichar": llamar a `clockIn()` / `clockOut()` según `isClockedIn`. Para abrir la pantalla: `WorkDestination`.
- Abrir la hoja de jornada o el foco directamente necesitaría un argumento de navegación (`WorkDestination(tab = ...)`):
  no lo añadí para no cambiar la API pública de `feature/work` que usa `app`.

## Foco: servicio foreground y notificación (preparado, NO implementado)

Fuera de esta iteración, pero la costura está lista: `FocusTimerAlarm` (core:data, `com.example.lifet.core.data.work`)
con `schedule(session)` y `cancel(sessionId)`, enlazada a `NoOpFocusTimerAlarm` en `WorkDataModule`. Una implementación
real (servicio foreground o alarma exacta + notificación) sustituye ese `@Binds`, programa el aviso para
`session.plannedEnd` y llama a `ManageFocusTimer.finishIfDue()` al vencer.

**El temporizador no depende de ella**: se guarda la hora de inicio (`FocusSession.start`, un `Instant`) y lo que queda se
calcula siempre con `plannedEnd - ahora`. Sobrevive a una rotación y a que el proceso muera. Lo único que falta con la app
cerrada es el *aviso*; al volver a abrirla, `finishIfDue()` cierra el intervalo en su hora prevista (no en la actual) y lo
marca como completado. La pantalla de Foco lo dice en una línea ("Deja la pantalla abierta: aún no hay aviso con la app
cerrada").

## Demo

`WorkDemoDataGenerator` registrado con `@IntoSet` en `core/demo/.../work/di/WorkDemoModule`. Las fechas salen del `Clock`
inyectado y nada queda por delante de "ahora".

- **EMPTY**: sin datos. **NEW_USER**: dos jornadas completas con un proyecto, una sesión de foco, una nota y la jornada de
  hoy en curso.
- **REGULAR**: 26 semanas de lunes a viernes con entrada entre las 8:00 y las 9:30 y de 7 h 30 min a 9 h 30 min de
  duración, pausa para comer, una semana de vacaciones sin jornadas, **exactamente dos olvidos de fichar la salida**
  (jornadas abiertas de hace 14 y 5 semanas, para probar el cierre a posteriori), **una jornada nocturna que cruza la
  medianoche** (un sábado de 22:00 a 03:30), algún sábado suelto de horas extra, **tres proyectos**, sesiones de foco y
  notas del día.
- **EDGE_CASES**: las dos últimas noches de cambio de hora (primavera: 7 h reales; otoño: 9 h reales) calculadas con las
  reglas de la zona, una jornada de **24 horas exactas**, una con **veinte pausas** (el máximo), otra registrada en Tokio,
  notas y proyecto con **textos de longitud máxima y emojis**, un proyecto con sólo un emoji, un objetivo semanal de 37,5 h,
  un hueco de días sin jornadas y la jornada de hoy **en curso con una pausa abierta**.
- Las **pausas solapadas imposibles** no están en los datos de demo (la demo sólo contiene lo que la validación acepta);
  se rechazan en el formulario y en `ManageWorkClock`, y lo prueban `ShiftValidatorTest` y `WorkUseCasesTest`.

## Componentes LT* que faltan (stub local mientras tanto)

- `LTProgressRing` anima su arco con un `tween(800)` cada vez que cambia el progreso, así que un anillo que avanza cada
  segundo estaría siempre en movimiento. Lo compenso redondeando el progreso a pasos del 1 %; un parámetro
  `animate = false` (o aceptar `progress` como `() -> Float`) lo resolvería bien. Además fija su tamaño con `size`: el
  contenido del centro usa un ancho fijo de 168 dp con `autoSize` para que no se corte con fuente 2x.
- `LTChartCard` sólo admite una serie: no se puede pintar el objetivo diario como línea de referencia sobre las horas por
  día, ni dos series (horas y objetivo) juntas. La tabla accesible lista las horas de cada día.
- Campos de formulario (`TextInput`, `DatePickerButton`, `TimePickerButton`, `SheetActions`, `SheetColumn`) locales en
  `feature/work/ui/FormFields.kt`. Son los mismos candidatos a `LTNumberField` / `LTDateField` / `LTTimeField` que ya
  pidió Deporte; Trabajo necesitaría además un selector de hora que devuelva minutos del día.
- Fila con un punto de color, dos líneas y estado a la derecha (`ShiftListItem`, `ProjectRow`): local. Candidata a
  `LTListRow`.
- Selector de color de proyecto (`ColorPalette`, en `OtherSheets.kt`): paleta de diez muestras de 48 dp con nombre para
  TalkBack. Candidato a `LTColorPicker`.

## Dependencias nuevas para el catálogo

Ninguna. `core:testing` expone `InMemoryWorkRepository` (ya dependía de `core:data`).

## Decisiones

- **Una jornada cuenta en el día en que empezó.** Una nocturna (22:00 a 06:00) es una sola jornada, no se parte en dos.
- **Tiempo real, no reloj.** Todo se calcula con instantes: el cambio de hora de primavera hace que una noche de 22:00 a
  06:00 dure 7 h y la de otoño 9 h. Una jornada puede durar como máximo 24 h.
- **Jornada olvidada** = abierta desde hace más de 16 h. No suma horas (no se inventan) y la pantalla invita a indicar la
  hora real de salida. Al cerrarla a posteriori se recortan o descartan las pausas que quedarían después de la salida.
- **Saldo de horas**: horas trabajadas menos objetivo, por día. El objetivo diario es el semanal repartido a partes
  iguales entre los días laborables (40 h entre 5 días = 8 h). Los días no laborables no tienen objetivo: lo que se trabaja
  en ellos es saldo a favor. **Un día sin ninguna jornada se asume libre** (vacaciones o festivo) y no resta. **El día en
  curso nunca resta**: su déficit sólo cuenta cuando termina, pero si ya superó su objetivo el exceso suma.
- **Validación única** (`ShiftValidator`, en `core:model`): la salida debe ser posterior a la entrada, ni entrada ni salida
  en el futuro, máximo 24 h, hasta 20 pausas, cada pausa dentro de su jornada y sin solaparse, una sola pausa abierta, y
  dos jornadas no pueden solaparse. Lo usan el formulario y el dominio; un formulario inválido nunca toca la base de datos.
- **Un toque no puede duplicar nada.** `ManageWorkClock` y `ManageFocusTimer` son singletons con un `Mutex` que cubre todo
  el ciclo leer, validar y escribir. Las pruebas de concurrencia fallan si se quita el cerrojo (comprobado). "Deshacer"
  vuelve a validar: si mientras tanto se creó otra jornada que la pisa, se rechaza en lugar de contar las horas dos veces.
- **Editar no degrada.** Al abrir una jornada para editarla se recuerdan sus instantes exactos; lo que el usuario no toque
  (entrada, salida, pausas) se conserva con sus segundos, y una jornada de 20 s no se convierte en una de 24 h.
- Borrar un proyecto conserva sus jornadas. Borrar una jornada borra sus pausas (cascada). "Deshacer" lo restaura todo.

## Pendiente / no verificado

- No se ha ejecutado en dispositivo ni emulador: sólo compilación, lint, detekt y tests JVM (incluidos Robolectric del DAO).
- Sin tests de UI Compose ni Roborazzi (no hay infraestructura todavía); sí `@LTPreviews` en las pantallas y hojas (Hoy,
  Hoy con casos límite, pantalla pequeña con fuente 2x, oscuro, Foco, Foco en marcha, Semana, Proyectos, vacío, cargando,
  error, jornada olvidada y las hojas de jornada, proyecto y ajustes), pero no se han revisado visualmente. **Conviene
  mirar con fuente 2x**: el cronómetro y la cuenta atrás usan `autoSize`, y las cuatro pestañas de `PrimaryTabRow` pueden
  no caber (si no caben, pasar a `PrimaryScrollableTabRow`).
- Sin test de migración: el esquema está en `VERSION = 1` (la base se recrea en debug). **Antes del primer release** hay que
  congelar el esquema, subir versión y escribir la migración; además `ProjectColor` y `FocusKind` se guardan por nombre
  (renombrar una constante rompería los datos).
- **Servicio foreground y notificación del foco**: no implementados (ver arriba).
- **El saldo se recalcula con los ajustes actuales para todo el histórico.** Cambiar el objetivo semanal o los días
  laborables reescribe las horas extra pasadas. Una solución correcta guarda los ajustes con fecha de efecto
  (`effectiveFrom`); no la hice por alcance. Conviene avisarlo en Ajustes.
- **Una jornada abierta entre 16 h y 24 h** deja de ser "en curso": el héroe vuelve a "Entrada" y la salida normal dice
  que no hay jornada. Se cierra desde la tarjeta "Falta la salida". El umbral (16 h) es menor que el máximo de una
  jornada (24 h) a propósito, para no contar horas inventadas; una guardia de 18 h es el caso que lo sufre.
- **La pantalla no se refresca sola con el paso del tiempo**, sólo con escrituras en la base de datos: con la app abierta al
  cruzar la medianoche, "hoy" y la semana mostrada no cambian hasta la próxima escritura, y una jornada que pasa de 16 h no
  se marca como olvidada hasta entonces. El cronómetro sí avanza. Una solución es re-emitir el resumen en el cambio de día
  y en `inicio + 16 h`.
- **Cálculo en el hilo del colector**: los `Observe*` ejecutan `WorkCalculator` donde se recolecta (el `flowOn` del
  repositorio va antes del `map`), igual que el resto de áreas. Es coste lineal en el histórico; con años de datos habría
  que moverlo a `@DefaultDispatcher` o acotar las consultas por fecha.
- **La zona del `Clock` se fija al crearlo** (`CommonModule`): si el usuario viaja con el proceso vivo, las jornadas nuevas
  se guardan con la zona anterior. Un `Clock` cuyo `getZone()` consulte `ZoneId.systemDefault()` en cada llamada lo
  arreglaría para todas las áreas (es compartido, no lo toqué).
- **Rotación con "Deshacer" visible**: el estado del snackbar no sobrevive a la rotación, así que ese borrado ya no se puede
  deshacer. Los eventos se consumen sólo con la pantalla visible.
- **Borrar una sesión de foco o limpiar una nota** no tienen entrada en la UI (sí existen en el dominio:
  `ManageFocusTimer.delete/restore`, `ManageWorkData.restoreNote`). Las eliminé de los tipos de la pantalla para no dejar
  código inalcanzable.
- **Mismo día, varias jornadas**: tocar un día en Semana abre la primera jornada de ese día (o una nueva si no hay ninguna);
  las demás se alcanzan desde Hoy o añadiendo otra jornada. Una lista del día sería la mejora natural.
- Un `@Suppress("CyclomaticComplexMethod")` en `WorkViewModel.onAction`: es una tabla plana de una línea por acción
  (sin `else`, para que una acción nueva sin manejar sea un error de compilación); partirla sólo la dispersaría.
