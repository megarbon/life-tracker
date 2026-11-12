# Iteración 2: funcionalidad real, diseño pulido y datos de prueba

Prompts para pasar de pantallas placeholder a una app usable y bonita, con datos de prueba realistas para probarlo todo.
Hay un **prompt común** que va al inicio de cada agente y tres fases:
- **Fase A**: un agente, el integrador. Bloquea al resto.
- **Fase B**: un agente por área, en paralelo.
- **Fase C**: Hoy, Insights y QA.

---

## Diagnóstico del estado actual (revisado en el repo)

| Hecho | Falta |
|---|---|
| Multimódulo (`core:*`, 9 `feature:*`), convention plugins, Hilt, Room, DataStore, navegación type-safe con `NavigationSuiteScaffold` | Las 9 features son placeholders con `LTEmptyState` |
| Tema, `AreaColors`, `Spacing`, `@LTPreviews`, `LTEmptyState`, `LTErrorState` | Resto de componentes `LT*` (tarjetas de métrica, gráficas, quick add, skeleton…) |
| `Money`, `Area`, `UserPreferences`, `GetTodayUseCase`, `TestClock`, `MainDispatcherRule` | Modelos, entities, DAOs y repositorios de cada área; la BD sólo tiene `AppMetaEntity` |
| detekt y spotless | Vico (gráficas), iconos extendidos, Roborazzi, CI de GitHub Actions, `docs/backlog`, `docs/handoff` |
| — | Iconos de pestañas provisionales (`Star`, `ShoppingCart`, `Favorite`); paquete todavía `com.example.lifet` |
| — | Datos de prueba y modo demo |

---

## Prompt común (pégalo al inicio de cada agente)

```
Eres parte del equipo que construye Life Tracker. Actúa como desarrollador Android senior y
diseñador de producto.

Lee primero: CLAUDE.md, docs/KICKOFF_MULTIAGENTE.md (reglas de coordinación y alcance),
docs/PROMPT_ITERACION_2.md (este plan) y las skills android-architecture, compose-ui-design,
life-tracker-domain, android-testing y feature-workflow.

ESTADO: el esqueleto existe (módulos, tema, navegación), pero las pantallas son placeholders.
OBJETIVO DE LA ITERACIÓN: cada área tiene funcionalidad real persistida en Room, un diseño
pulido y coherente, y datos de prueba realistas para probar todos los estados y gráficas.

PRINCIPIOS DE DISEÑO (aplícalos en cada pantalla)
- Jerarquía clara: arriba una métrica "héroe" grande (displaySmall, números tabulares) con su
  contexto ("de 2.000 kcal", "quedan 340 € este mes"); debajo tarjetas de detalle; al final el
  historial.
- Color de acento del área (AreaColors) en iconos, progreso y gráficas; superficies con
  surfaceContainer* de Material 3, no colores planos inventados.
- Registrar en < 5 s: FAB contextual + LTQuickAddSheet con valores recientes y favoritos.
- Microinteracciones: animación de progreso al entrar, AnimatedContent al cambiar cifras,
  haptic al guardar y snackbar con "Deshacer" al borrar.
- Estados vacíos que enseñan (ilustración o icono + texto + CTA) y skeletons en vez de spinners.
- Accesibilidad: 48 dp, contentDescription, gráficas con resumen para TalkBack y fuente al 200 %
  sin cortes.
- Coherencia: usa sólo componentes LT*. Si falta alguno, pídelo en docs/handoff/<area>.md y
  usa un stub local temporal.

DATOS DE PRUEBA
- Cada área aporta un generador determinista (Random con semilla fija y fechas relativas al
  Clock inyectado, nunca a la fecha real) con estos escenarios: VACÍO, NUEVO (3 días),
  HABITUAL (6 meses de datos realistas, con patrones semanales y estacionales creíbles) y
  EDGE_CASES (cruces de medianoche, cambio de hora DST, divisas distintas, importes enormes,
  textos larguísimos, emojis, recaídas, días sin datos).
- Los mismos datos alimentan previews, screenshot tests, tests de UI y el modo demo de la app.
- Los datos de prueba nunca se incluyen en el build de release.

DEFINITION OF DONE DE LA ITERACIÓN (por área)
- CRUD completo persistido en Room, con migración y test de migración.
- ViewModel con UiState sellado y tests con Turbine (Loading/Empty/Success/Error).
- Al menos 2 gráficas con LTChartCard y datos reales.
- Previews de cada pantalla con los escenarios VACÍO, HABITUAL y EDGE_CASES (claro, oscuro y
  fuente 2x), y screenshot tests con Roborazzi.
- Compose UI test del flujo principal (crear, editar y borrar).
- ./gradlew spotlessApply detekt lint testDebugUnitTest en verde.
- Revisión con el agente android-reviewer sin hallazgos críticos ni altos.
- docs/handoff/<area>.md actualizado.
Trabaja en ramas feat/<area>-… con Conventional Commits y un PR por paso.
Al acabar cada paso: build, tests, resumen y lo que queda pendiente.
```

---

## Fase A — Fundaciones compartidas (agente integrador; bloquea al resto)

```
[prompt común]

ROL: integrador, dueño de los archivos compartidos. Un PR por punto:

A0 (opcional pero recomendado ahora, después costará más): renombra el paquete y el
   applicationId de com.example.lifet a [PAQUETE] en todos los módulos.
A1 Dependencias en libs.versions.toml y convention plugins: Vico (compose-m3),
   material-icons-extended (o Material Symbols), Roborazzi + plugin, kotlinx-datetime si hace
   falta, Compose UI test. ADR que justifique cada una.
A2 Componentes de core:designsystem, cada uno con @LTPreviews y screenshot test:
   LTMetricCard (icono de área, título, valor héroe, subtítulo, progreso opcional, onClick),
   LTProgressRing (animado, multianillo opcional), LTStatTile, LTSectionHeader (con acción
   "Ver todo"), LTQuickAddSheet (ModalBottomSheet con chips de recientes), LTAmountField
   (teclado numérico, formato por locale, Money), LTChipGroup (selección simple y múltiple),
   LTLoadingSkeleton (shimmer que respeta "eliminar animaciones"), LTTopAppBar (large, con
   colapso al hacer scroll), LTDateRangeSelector (Día/Semana/Mes/Año + flechas), LTChartCard
   (wrapper de Vico: titular en lenguaje natural, leyenda, contentDescription resumida y
   alternativa de tabla accesible), LTCalendarHeatmap (para ánimo, días limpios y actividad),
   LTUndoSnackbar helper y LTConfirmDialog.
A3 Iconos definitivos de las pestañas y de cada área (core:ui AreaPresentation): sustituye
   Star/ShoppingCart/Favorite por iconos con significado (Restaurant, FitnessCenter,
   AccountBalanceWallet, SelfImprovement/Mood, Work, SmokeFree…), con versión rellena para
   la pestaña seleccionada.
A4 Infraestructura de datos de prueba:
   - core:testing: DemoScenario {EMPTY, NEW_USER, REGULAR, EDGE_CASES}, utilidades de fecha
     relativas al Clock, SeededRandom y builders base.
   - Módulo :core:demo (sólo debugImplementation) con DemoDataSeeder, que orquesta los
     generadores de cada área (interfaz AreaDemoDataGenerator con multibinding de Hilt
     @IntoSet, para que cada área registre el suyo sin tocar archivos compartidos).
   - Pantalla "Opciones de desarrollador" en Ajustes, visible sólo en debug: cargar escenario,
     borrar todos los datos, avanzar el reloj ±1 día (Clock falso de debug) y forzar estado de
     error.
   - Al primer arranque en debug, ofrecer cargar el escenario REGULAR.
A5 Base de datos: estrategia para que cada área añada entities sin conflictos (plantilla en
   docs/handoff, migraciones numeradas por orden de llegada y test de migración genérico).
A6 CI en .github/workflows: build, unit tests, detekt, lint y verifyRoborazzi en cada PR, con
   caché de Gradle. Plantilla de PR con la DoD.
A7 Crea docs/backlog/ (una épica por área con las historias de esta iteración) y docs/handoff/
   (una plantilla por área).

Salida: build y CI en verde, catálogo de componentes visible en previews y modo demo funcional
(aunque ninguna área tenga aún generador).
```

---

## Fase B — Áreas en paralelo (un agente por área)

Plantilla. Pega el **prompt común** y después esto, rellenando el bloque del área:

```
ROL: dueño de feature:<area> y de las carpetas <area> en core/model, core/domain, core/data y
core/database. Sigue feature-workflow y PARA tras el paso 0 (especificación con bocetos ASCII
de cada pantalla) para que lo revise.

Además del alcance de abajo, entrega:
- <Area>DemoDataGenerator registrado con @IntoSet y los 4 escenarios.
- Observe<Area>TodaySummary en core/domain para la pantalla Hoy.
- Observe<Area>Series(range) para Insights.

[BLOQUE DEL ÁREA]
```

### Bloques por área (alcance de esta iteración)

**Finanzas**
```
Funcionalidad: movimientos (gasto/ingreso) con categoría, cuenta, fecha y nota; cuentas y
transferencias; categorías por defecto editables; presupuesto mensual por categoría;
recurrentes (se generan como "previstos"); búsqueda y filtros.
Pantallas: Resumen del mes (héroe: disponible este mes; tarjetas: presupuesto, gasto por
categoría y próximos recurrentes) · Movimientos agrupados por día con total diario ·
Alta/edición en bottom sheet · Presupuestos · Cuentas y categorías.
Gráficas: donut por categoría, barras de los últimos 6 meses y línea de saldo real vs. previsto.
Demo REGULAR: nómina el día 1, alquiler, 3 suscripciones, supermercado 2-3 veces por semana,
ocio en fin de semana, un mes con presupuesto superado y una devolución.
```

**Alimentación**
```
Funcionalidad: comidas por tipo con alimentos y gramos; base de alimentos local (semilla de
unos 150 alimentos comunes en España) y alimentos personalizados; recientes, favoritos y
"repetir ayer"; agua con +250 ml; objetivos de kcal y macros.
Pantallas: Diario del día (héroe: anillo de kcal; macros en barras; secciones por comida) ·
Buscar/añadir alimento · Detalle de alimento con selector de ración · Objetivos.
Gráficas: kcal de 7 y 30 días vs. objetivo, y reparto de macros.
Demo REGULAR: desayunos repetitivos, comidas variadas, algún día sin registrar, fines de
semana con más kcal y agua irregular.
```

**Deporte**
```
Funcionalidad: entrenos de cardio (tipo, duración, distancia y kcal) y de fuerza (ejercicios y
series reps × kg); plantillas de rutina; PRs automáticos; peso corporal; pasos manuales.
Health Connect queda fuera de esta iteración, pero deja la interfaz preparada.
Pantallas: Resumen (héroe: minutos activos de la semana vs. 150) · Historial · Registrar
entreno · Detalle de ejercicio con progresión · Peso.
Gráficas: minutos por semana, progresión de un ejercicio, peso con media móvil y heatmap de
actividad.
Demo REGULAR: 3-4 entrenos por semana con progresión de cargas creíble, una semana de
vacaciones sin actividad y un peso con tendencia suave.
```

**Dejar de fumar y beber**
```
Funcionalidad: hábitos a dejar (tabaco y alcohol) con fecha de inicio, consumo y precio previos;
contador en vivo; dinero ahorrado; registro de antojos (intensidad, disparador y estrategia) y de
consumos o recaídas sin culpa (se conservan récord y totales); hitos; SOS con respiración guiada.
Pantallas: Resumen por hábito (héroe: tiempo limpio en vivo + € ahorrados) · Registrar antojo
o consumo · Hitos · SOS · Historial.
Gráficas: antojos por hora y por disparador, ahorro acumulado y calendario de días limpios.
Demo REGULAR: tabaco dejado hace 47 días con una recaída el día 12; alcohol en modo reducción
con límite semanal, cumplido 3 de 4 semanas.
```

**Trabajo**
```
Funcionalidad: fichar entrada, salida y pausas; proyectos; sesiones de foco con temporizador
(servicio foreground y notificación); horas objetivo semanales y saldo; nota del día.
Pantallas: Hoy en el trabajo (héroe: jornada en curso con cronómetro) · Foco · Semana · Proyectos.
Gráficas: horas por día vs. objetivo, reparto por proyecto y saldo de horas extra.
Demo REGULAR: jornada de lunes a viernes con variación, 2 olvidos de fichar la salida, una
jornada que cruza medianoche y 3 proyectos.
```

**Ánimo**
```
Funcionalidad: check-in rápido de 5 niveles (1 toque) y detallado (emociones múltiples,
energía 1-5, estrés 1-5, factores/etiquetas, nota y gratitud); varias entradas al día;
emociones y factores editables; recordatorio diario.
Pantallas: Check-in (rápido arriba, detalle desplegable) · Diario (timeline) · Calendario
"year in pixels" · Detalle de entrada.
Gráficas: ánimo con media de 7 días, emociones más frecuentes e impacto de factores (ánimo medio
con y sin cada factor).
Demo REGULAR: ánimo con ciclo semanal (lunes más bajos), mejor en días con deporte y una racha
baja de 5 días (para probar la tarjeta de recursos de apoyo, mostrada con delicadeza).
```

---

## Fase C — Integración, Hoy, Insights y QA

### Integrador
```
[prompt común]
- Procesa todos los docs/handoff/*.md: entities, migraciones, navegación, componentes pedidos
  (y sustituye los stubs).
- Hoy: saludo según la hora; tarjetas LTMetricCard por área con Observe*TodaySummary, reordenables
  y ocultables; fila de acciones rápidas (gasto, comida, agua, ánimo, fichar, antojo); estados
  vacíos que invitan a configurar cada área.
- Ajustes: divisa, unidades, objetivos, tema y recordatorios, más las opciones de desarrollador
  en debug.
```

### Insights
```
[prompt común]
Pantalla de tendencias con LTDateRangeSelector, una sección por área (Observe*Series) y
correlaciones como funciones puras con tests (≥ 14 días de datos y lenguaje prudente):
ánimo vs. deporte, ánimo vs. horas de trabajo, gasto vs. ánimo y antojos vs. estrés.
Usa el escenario REGULAR para comprobar que las correlaciones sembradas en los datos de demo
aparecen.
```

### QA
```
[prompt común]
No añades features. Con el modo demo:
- Recorre cada pantalla en los 4 escenarios, en claro y oscuro, con fuente 200 %, TalkBack,
  tablet y horizontal; anota los fallos en docs/qa/iteracion-2.md con captura y severidad.
- Tests E2E de los flujos: cargar demo → registrar gasto → check-in de ánimo → fichar →
  registrar antojo → añadir comida → ver Hoy actualizado.
- Ejecuta android-reviewer módulo a módulo y corrige los críticos y altos en ramas fix/.
- Revisa el rendimiento del scroll de listas largas (escenario REGULAR) y la recomposición.
```

---

## Orden recomendado

```
Fase A (integrador) ─► Fase B: Finanzas ‖ Alimentación ‖ Deporte ‖ Dejar hábitos ‖ Trabajo ‖ Ánimo
                                   └─► Fase C: Integrador (Hoy, Ajustes) ‖ Insights ─► QA
```

Si tienes pocos agentes, prioriza **A → Ánimo → Finanzas → Hoy**: con eso ya usas la app a diario y ves el resultado del diseño con datos reales.
