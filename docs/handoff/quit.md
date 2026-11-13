# Handoff: quit (Dejar de fumar y beber)

Estado: listo para integrar

Rama `feat/quit-complete`. Todo el área funciona de extremo a extremo (Room, repositorio, casos de uso, ViewModel, UI,
gráficas, demo y tests). La navegación existente (`quitGraph()` / `QuitDestination`) no cambia.

## Pantalla (boceto)

```
┌ Dejar hábitos ──────────────────────┐
│ Resumen │ Hitos │ Historial         │
├─────────────────────────────────────┤
│ (Tabaco) (Alcohol) (+ Añadir otro)  │  selector de hábito
│ ┌ Tiempo limpio ──────────────────┐ │  héroe
│ │ 34 días y 18 h 40 min           │ │  contador EN VIVO (segundos el 1.er día, minutos después)
│ │ 410,50 € ahorrados              │ │  euros ahorrados, también en vivo
│ │ 34 días limpios desde la última │ │  tras una recaída: racha actual + récord + totales
│ │ vez · Tu récord: 34 días        │ │
│ └─────────────────────────────────┘ │
│ [ Necesito ayuda ahora ]            │  SOS (respiración guiada)
│ [Tengo un antojo] [He consumido]    │  alta rápida
│ [Antojos hoy][Sin consumir][Tropiezos]  LTStatTile
│ Próximo hito: 3 meses · faltan …    │  barra de avance en vivo
│ ┌ Antojos por hora del día ───────┐ │  LTChartCard COLUMNS (franjas de 3 h)
│ ┌ Ahorro acumulado ───────────────┐ │  LTChartCard LINE
│ ┌ Calendario de días limpios ─────┐ │  LTCalendarHeatmap (3 meses)
│ Qué suele provocar tus antojos      │
└─────────────────────────────────────┘      [ + Registrar ]  FAB -> LTQuickAddSheet
```

- **Modo reducción** (alcohol): el héroe pasa a "2 de 4" consumiciones de la semana, con el límite y el cumplimiento
  de las últimas 4 semanas ("cumplido 3 de 4"). Hay una gráfica semanal en lugar de la tarjeta de hitos.
- **Hitos**: 24 h, 48 h, 72 h, 1 semana, 2 semanas, 1 mes, 3 meses, 6 meses, 1 año, 2 años y 5 años. Los que una racha
  anterior ya alcanzó se quedan como "Ya lo conseguiste antes": una recaída no quita nada. Al alcanzar uno nuevo sale una
  celebración sobria (un diálogo tranquilo, sin confeti ni sonido) una sola vez.
- **Historial**: antojos y consumos de todos los hábitos agrupados por día; tocar una fila edita; borrar muestra
  "Deshacer". Un consumo no lleva rojo ni icono de alarma: es una marca en el camino, igual que un antojo.
- **SOS**: respiración guiada 4-4-6 con un círculo que crece, se mantiene y se encoge. Con "eliminar animaciones" el
  círculo no se mueve y se sigue con las palabras y la cuenta atrás. Al terminar ofrece anotar el antojo con "Respirar"
  ya marcado como estrategia.
- **Registrar antojo o consumo** (`LTQuickAddSheet`): arriba, las combinaciones recientes de disparador y estrategia como
  chips (un toque rellena ambos); debajo, intensidad de 1 a 5 con palabra, disparador, estrategia, hora y nota. Para un
  consumo: unidades, disparador opcional y nota, con un texto que recuerda que no borra récord, días ni ahorro.

## Tono (decisión de producto, no negociable)

Esta área trata de salud y adicciones. Una recaída **nunca** borra progreso ni se pinta de rojo, y el lenguaje es siempre
de apoyo:

- Se muestra "X días limpios desde la última vez" **junto al** récord histórico y los totales (días limpios, ahorro).
- El ahorro acumulado **nunca baja**: un consumo sólo descuenta lo consumido de lo que se habría gastado, con suelo en 0.
- Los consumos se llaman "tropiezos" y "no cuentan en tu contra". En el calendario un día con consumo se ve más tenue,
  nunca vacío ni de otro color.
- El rojo (`colorScheme.error`) sólo aparece en errores de formulario (un campo mal rellenado), jamás en un consumo.
- Sin signos de exclamación; sin "fallo", "culpa" ni "has perdido".
- El SOS incluye un recordatorio discreto de que se puede pedir ayuda profesional y de que el 112 atiende emergencias.
  **Pendiente de revisión humana**: el texto de apoyo (`quit_sos_support`) lo ha redactado el agente; conviene que una
  persona lo lea antes de publicar.

## Entities y DAOs (YA registrados en LifeTrackerDatabase)

Registrado con líneas mínimas (nombres totalmente cualificados) en `entities = [...]` y `abstract fun quitDao()`.
No requiere acción: sólo conservar ambas listas al resolver conflictos de merge.

- `quit_habits`, `quit_cravings` (FK en cascada al hábito) y `quit_consumptions` (FK en cascada al hábito).
- Dinero como `Long` en unidades menores + código ISO 4217 (`packPriceMinor` + `currency`). Instante UTC + `ZoneId` por
  registro, de modo que el día lógico no cambia si el usuario viaja.
- `QuitDao` (público) y `QuitDatabaseModule` (`core/database/.../quit/di/`) con su `@Provides`.
- Se usa `@Upsert` (no `REPLACE`), porque `REPLACE` borraría el hábito y arrastraría sus antojos por la cascada.
- `schemas/.../1.json` se regenera con el esquema; si hay conflicto con otras áreas, regenerarlo tras el merge
  (`./gradlew :core:database:kspDebugKotlin`).

## Hoy: qué cablear

`ObserveQuitTodaySummary` (core:domain, `com.example.lifet.core.domain.quit`) emite `QuitTodaySummary`:

| Campo | Uso sugerido en `LTMetricCard` |
|---|---|
| `hasData` | `false` -> estado vacío que invita a añadir el primer hábito |
| `primaryHabitName` / `primaryKind` / `primaryMode` | título de la tarjeta ("Tabaco") e icono del hábito |
| `cleanSince` | instante en que empezó la racha actual: con él la tarjeta dibuja el contador en vivo |
| `cleanSeconds` | tiempo limpio al emitir (valor héroe: "34 días") |
| `bestStreakSeconds` | récord; mostrarlo junto a la racha si es mayor ("récord: 41 días") |
| `totalSaved` | dinero ahorrado de todos los hábitos en la divisa del principal |
| `hasOtherCurrencies` | `true` si hay hábitos en otra divisa que `totalSaved` no suma |
| `nextMilestone` | próximo hito (`key`, `remainingSeconds`, `progress`) para el subtítulo y la barra |
| `cravingToday` / `cravingsToday` | "Hoy has anotado 2 antojos y sigues aquí" (nunca en tono de alarma) |
| `consumedToday` | no usarlo para avisar con rojo; a lo sumo, un texto de apoyo |
| `weekUnits` / `weeklyLimit` | sólo si el principal está en reducción: "2 de 4 esta semana" |

Icono y color: `Area.QUIT.icon` y `areaColors.quit`.

**Contador en vivo en Hoy**: `ObserveQuitTodaySummary` no emite cada segundo (sería recalcular toda la base). La tarjeta
debe usar `cleanSince` y el reloj inyectado, igual que hace `LiveClock.kt` en `feature/quit` (`rememberLiveNow`): una
`State<Instant>` que se actualiza cada segundo el primer día y cada minuto después, sólo mientras la pantalla está
visible, y leída únicamente por el `Text` que cambia (no por toda la tarjeta). Hay también `CleanTime.of(seconds)` y
`spanText(...)` para dar formato.

**Hito recién alcanzado**: `nextMilestone` sólo mira hacia delante. Para felicitar en Hoy basta comparar
`cleanSeconds` con los `MilestoneKey.seconds`.

## Insights

`ObserveQuitSeries(range: DateRange)` emite `QuitSeries` con un `QuitDayPoint` por día hasta hoy (`tracked`, `clean`,
`cravings`, `maxCravingIntensity`, `consumedUnits`). Los días sin datos van con cero, no se omiten.

- Un día es **limpio** si había un hábito en marcha y ningún consumo de los hábitos que se dejan del todo (si todos
  están en reducción, de cualquiera). Beber dentro del límite semanal no convierte cada día en "no limpio".
- Los antojos y las unidades cuentan todos los hábitos.
- Para "antojos vs. estrés": `maxCravingIntensity` por día contra el ánimo/estrés de Ánimo. Las horas y los disparadores
  de los antojos están en `HabitProgress.cravingsByHour` y `topTriggers` (vía `ObserveQuitOverview`).

## Acciones rápidas para Hoy

- "Antojo": abrir la ruta de Dejar hábitos (`QuitDestination`); el botón "Tengo un antojo" y el FAB abren la hoja de
  alta. Abrir la hoja directamente necesitaría un argumento de navegación (`QuitDestination(openCraving = true)`): no lo
  añadí para no cambiar la API pública de `feature/quit` que usa `app`.
- Un acceso directo al **SOS** desde Hoy sería muy útil (un antojo no espera a navegar dos pantallas), por la misma razón.

## Demo

`QuitDemoDataGenerator` registrado con `@IntoSet` en `core/demo/.../quit/di/QuitDemoModule`.

- **EMPTY**: sin datos. **NEW_USER**: tabaco dejado hace 3 días con 6 antojos superados (café, tras comer, aburrimiento).
- **REGULAR**: tabaco dejado hace 47 días con **una recaída el día 12** (cena con amigos, 3 cigarrillos); antojos más
  frecuentes e intensos al principio y que se calman con el tiempo, concentrados tras el café y tras comer. Alcohol en
  modo reducción desde hace 12 semanas con límite de 4 a la semana, **cumplido 3 de las últimas 4 semanas**.
- **EDGE_CASES**: más de 6 años de abstinencia con recaídas en el cambio de hora (un segundo antes y después del salto),
  un hábito recién empezado hace 40 minutos, el precio máximo del formulario con el consumo máximo (el ahorro satura, no
  desborda), una divisa sin decimales (JPY), nombre y notas en su longitud máxima con emojis, recaídas a las 00:00 y a
  las 23:59:59, otra zona horaria (Tokio) con recaídas cerca de la medianoche local, treinta antojos en un solo día, un
  hueco sin datos y un límite semanal de una sola copa superado.

## Componentes LT* que faltan (stub local mientras tanto)

- `LTCalendarHeatmap` sólo dibuja un mes y no admite pulsar un día ni etiquetar el mes: el calendario de días limpios se
  compone mes a mes con un título propio (3 meses). Un `onDayClick` abriría los registros de ese día.
- `LTChartCard` sólo admite una serie y no marca líneas de referencia: no se puede dibujar el límite semanal sobre la
  gráfica de consumo por semana (se dice en el titular), ni el récord sobre el ahorro.
- `LTChartCard` fija su altura en 200 dp (`height(200.dp)`), lo que contradice la regla "cero alturas fijas" de la guía.
- Un `LTLiveText` (o `rememberLiveNow` en `core:designsystem`) que expusiera un instante que avanza con el ciclo de
  vida; hoy vive local en `feature/quit/LiveClock.kt`. Lo necesitarán Hoy y Trabajo (cronómetro).
- Un componente de respiración/temporizador circular (`LTBreathCircle`) por si otra área lo reutiliza (p. ej. Ánimo).
- Campos de formulario (`NumberField`, `TextInput`, `DateField`, `TimeField`) locales en `feature/quit/ui/FormFields.kt`.
  Son los mismos que Deporte y Finanzas tienen duplicados: candidatos a `LTNumberField` / `LTDateField` / `LTTimeField`.

## Dependencias nuevas para el catálogo

Ninguna. `core:testing` expone `InMemoryQuitRepository` (ya dependía de `core:data`).

## Decisiones

- **Racha y récord**: la racha termina en cada consumo; el récord es la racha más larga que ha existido y nunca baja. El
  consumo sólo reinicia la racha actual: el récord, los días limpios totales y el ahorro acumulado se conservan.
- **Días limpios totales**: días completos desde el inicio sin ningún consumo ese día; el día en curso aún no cuenta.
- **Ahorro** = (consumo previo × tiempo transcurrido − consumido) × precio de la unidad, con suelo en cero. Se calcula con
  `BigInteger` (nunca `Double`) y satura en `Long.MAX_VALUE`: un precio o un consumo enormes no desbordan.
- **Hitos**: un mes son 30 días y un año 365; son referencias, no calendario. En modo reducción no hay hitos de tiempo.
- **Hábito principal**: el que se deja del todo y empezó antes; si no hay ninguno, el más antiguo.
- **Zona y medianoche**: cada registro guarda su `ZoneId`; el día lógico (`date`) sale de ahí, así que una recaída a las
  00:00 cuenta para el día nuevo y un cambio de hora no la desplaza. El tiempo limpio usa instantes reales: el día del
  cambio de hora tiene 23 o 25 horas.
- **El "ahora" de una hoja** lo estampa el ViewModel al abrirla (`QuitEditor.*.now`): validar contra el instante de
  apertura evita rechazar un registro válido si la hoja se deja abierta un rato.
- **Contador en vivo sin recomponer la pantalla**: el instante vive en un `State` que sólo lee el `Text` que cambia, y se
  detiene al pasar a segundo plano (`repeatOnLifecycle(STARTED)`).
- Borrar un hábito borra sus antojos y consumos (cascada). "Deshacer" lo restaura **entero** en una sola transacción.

## Pendiente / no verificado

- No se ha ejecutado en dispositivo ni emulador: sólo compilación, lint y tests JVM.
- Sin tests de UI Compose ni Roborazzi (no hay infraestructura todavía); sí `@LTPreviews` en las pantallas y hojas
  (resumen, resumen recién empezado, años de abstinencia, reducción, hitos, historial con textos larguísimos, vacío,
  cargando, error, hojas de antojo, consumo y hábito, y SOS), pero no se han revisado visualmente.
- La **animación del círculo del SOS** y el contador en vivo no se han visto moverse: la lógica (`BreathingCycle`,
  `CleanTime`, `millisUntilNextTick`) tiene tests JVM, pero el aspecto y el ritmo hay que probarlos en el móvil, con y sin
  "eliminar animaciones".
- Sin test de migración: el esquema está en VERSION = 1 (la base se recrea en debug).
- No hay recordatorios ni notificaciones (p. ej. "has llegado a una semana"): requeriría WorkManager y permisos.
- El importe y la divisa se piden al crear el hábito (EUR por defecto). Cuando Ajustes exponga la divisa del usuario,
  `QuitAction.OpenHabit(currency = ...)` ya la admite; falta cablearla desde `UserPreferences`.
- Los **objetivos** (cumplimiento de las últimas 4 semanas, 8 semanas en la gráfica, 3 meses en el calendario) son
  constantes de `QuitRules`.
- No se pueden editar a mano el consumo previo ni el precio de un hábito en curso sin que cambie el ahorro ya calculado
  (se recalcula con los valores nuevos desde el inicio): es la decisión más simple, pero conviene saberlo.
- Los textos de hitos (monóxido de carbono, gusto y olfato...) son afirmaciones generales de salud pública redactadas por
  el agente, no consejo médico; conviene una revisión humana antes de publicar.
