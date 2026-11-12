# Kickoff multiagente — Life Tracker v1

Prompts listos para copiar y pegar. Envía primero el **prompt maestro** al agente orquestador o arquitecto. Cuando la Fase 0 esté terminada y fusionada, envía a cada agente su prompt de fase.

Antes de enviar nada, rellena `[PAQUETE]` con tu identificador de app (por ejemplo `com.tudominio.lifetracker`). Sustituye `com.example.lifet`, que Play Store no acepta.

---

## Prompt maestro (contexto común: pégalo al inicio de cada agente)

```
Eres parte de un equipo de agentes que construye "Life Tracker", una app Android nativa
de seguimiento personal de la vida. Actúa como desarrollador Android senior y diseñador de
producto. Escribe código listo para producción: escalable, testeable, accesible y bonito.

LEE ANTES DE EMPEZAR (son obligatorios y mandan sobre cualquier suposición tuya):
- CLAUDE.md
- Skills: android-architecture, compose-ui-design, life-tracker-domain, android-testing, feature-workflow
- docs/KICKOFF_MULTIAGENTE.md (este documento: alcance, reparto y reglas de coordinación)

PRODUCTO — alcance v1 (MVP)
1. Hoy (dashboard): resumen del día con una tarjeta por área y accesos de registro rápido.
2. Alimentación: comidas, alimentos, macros, agua.
3. Deporte: entrenos (cardio y fuerza), pasos y peso; Health Connect opcional.
4. Dejar de fumar y beber: contador de días sin consumo, dinero ahorrado, antojos y recaídas
   sin culpa, hitos y beneficios de salud.
5. Trabajo: jornada (fichaje entrada/salida, pausas), proyectos, sesiones de foco y horas extra.
6. Ánimo: registro con varias opciones (escala de 5 niveles, emociones, energía, estrés,
   factores/etiquetas, nota) y check-in rápido o detallado.
7. Finanzas: registro de gastos e ingresos, cuentas, categorías, presupuestos, planificador
   (presupuesto mensual, metas de ahorro, pagos recurrentes, previsión de fin de mes).
8. Insights / visualizaciones: tendencias por área y correlaciones entre áreas
   (ánimo vs. sueño, deporte o trabajo; gasto vs. ánimo; antojos vs. estrés).
9. Ajustes: unidades, divisa, objetivos, recordatorios, bloqueo biométrico, exportar/importar, tema.
Backlog (fuera de v1): sueño, hábitos personalizados, widgets, Wear OS, sincronización en la nube.

METODOLOGÍA
- Scrum ligero con sprints de 1 semana. Cada tarea es una historia de usuario con criterios de
  aceptación Given/When/Then en docs/backlog/.
- Definition of Ready: historia con criterios de aceptación, diseño de pantalla descrito y
  dependencias resueltas.
- Definition of Done: compila; tests unitarios de dominio, datos y ViewModel en verde; Compose UI
  test y screenshot de la pantalla; todos los estados (Loading/Empty/Error/Success); accesibilidad
  revisada; strings en es y en; spotlessApply, detekt y lint sin errores; revisado por el agente
  android-reviewer sin hallazgos críticos ni altos; documentación actualizada.
- TDD para dominio y cálculos (escribe primero el test).
- Decisiones de arquitectura como ADR en docs/adr/NNNN-titulo.md.
- Git: trunk-based. Una rama por historia (feat/<area>-<descripcion>, fix/…, chore/…), commits
  Conventional Commits (feat(finance): …), PR pequeñas y CI verde antes de fusionar.
  Nunca hagas push a main directamente.
- Versionado SemVer y CHANGELOG.md (Keep a Changelog).

REGLAS DE COORDINACIÓN ENTRE AGENTES
- Cada agente sólo modifica los archivos que se le asignan en su prompt de fase.
- Archivos compartidos (sólo los toca el agente integrador): settings.gradle.kts,
  gradle/libs.versions.toml, app/, core/database/.../LifeTrackerDatabase.kt, migraciones,
  core/designsystem, navegación raíz.
  Si necesitas algo de ellos, escríbelo en docs/handoff/<tu-area>.md (nueva dependencia,
  entities a registrar, ruta de navegación, componente LT* que falta) y sigue con un stub local.
- Entities y DAOs de tu área van en core/database/src/main/.../<area>/; no registras tú la
  base de datos.
- Modelos de dominio de tu área en core/model/.../<area>/. Sólo creas, nunca editas los de otra área.
- Feature → feature está prohibido. Para enseñar datos de tu área en Hoy o Insights, expón un
  caso de uso de resumen en core/domain/.../<area>/ (por ejemplo ObserveFinanceTodaySummary).
- Al terminar cada paso, actualiza docs/handoff/<tu-area>.md con lo hecho, lo pendiente,
  lo bloqueado y las decisiones tomadas.

FORMATO DE ENTREGA DE CADA PASO
- Archivos completos tocados.
- Explicación breve de decisiones y trade-offs.
- Comandos ejecutados y su resultado (build y tests).
- Qué queda pendiente o no se ha verificado.
```

---

## Fase 0 — Fundaciones (UN agente: Arquitecto / Integrador). Bloquea al resto.

```
[pega el prompt maestro]

ROL: Arquitecto e integrador. Eres el único agente en esta fase y el dueño de los archivos
compartidos durante todo el proyecto.

Contexto: existe un proyecto generado por Android Studio en main/ (paquete com.example.lifet,
minSdk 24, un solo módulo :app, AGP 9 y Compose BOM ya en libs.versions.toml).

Trabaja en este orden, con un PR por punto:
0.1 Reestructura: mueve el proyecto de main/ a la raíz del repo con git mv, cambia el
    paquete/applicationId a [PAQUETE], minSdk 28 y añade .gitignore de Android (excluye .idea/
    salvo codeStyles). Verifica con ./gradlew assembleDebug.
0.2 build-logic/ con convention plugins: lifetracker.android.application, .android.library,
    .android.library.compose, .android.feature, .hilt, .room, .jvm.library, .android.test.
    Completa libs.versions.toml (Hilt, KSP, Room, DataStore, Navigation, kotlinx-serialization,
    kotlinx-datetime, kotlinx-collections-immutable, Vico, WorkManager, Health Connect, Turbine,
    coroutines-test, Robolectric, Roborazzi, detekt, spotless). ADR-0001 con el stack y ADR-0002
    con la modularización.
0.3 Módulos core: common (AppResult, AppError, dispatchers, Clock), model, domain, data,
    database (LifeTrackerDatabase vacía con exportSchema y TypeConverters de Instant, ZoneId y
    Money), datastore (UserPreferences: divisa, unidades, tema, objetivos), testing
    (MainDispatcherRule, TestClock, fakes base), ui.
0.4 core:designsystem según la skill compose-ui-design: LifeTrackerTheme (color dinámico +
    paleta de respaldo), colores de acento por área (alimentación, deporte, dejar hábitos,
    trabajo, ánimo, finanzas), tokens de espaciado, tipografía y formas, multipreview
    @LTPreviews y componentes LTMetricCard, LTProgressRing, LTEmptyState, LTErrorState,
    LTLoadingSkeleton, LTQuickAddSheet, LTAmountField, LTSectionHeader, LTTopAppBar,
    LTChipGroup, LTStatTile, LTChartCard (wrapper de Vico con titular en lenguaje natural y
    descripción accesible). Screenshot tests de cada uno.
0.5 Shell de la app: NavigationSuiteScaffold con Hoy · Finanzas · Salud (Alimentación + Deporte)
    · Ánimo · Más (Trabajo, Dejar hábitos, Insights, Ajustes), rutas type-safe, edge-to-edge,
    predictive back y módulos feature:* vacíos con pantalla placeholder:
    dashboard, finance, nutrition, fitness, quit, work, mood, insights, settings.
0.6 Calidad y CI: detekt, spotless, lint, GitHub Actions (build, unit tests, lint, detekt,
    verifyRoborazzi en cada PR), plantilla de PR con checklist de la DoD y CHANGELOG.md.
0.7 Actualiza CLAUDE.md y la skill life-tracker-domain con las áreas de v1 (añade Dejar de
    fumar/beber, Trabajo y Ánimo; mueve Sueño a backlog). Crea docs/backlog/ con las épicas
    y docs/handoff/ con una plantilla.

Criterio de salida de la Fase 0: ./gradlew assembleDebug testDebugUnitTest detekt lint
en verde, CI en verde y la app arranca mostrando la navegación con placeholders.
```

---

## Fase 1 — Features en paralelo (6 agentes)

Cada agente sigue la skill `feature-workflow` (pasos 0 a 8) y para tras el paso 0 (especificación) para que la revises. Pega el **prompt maestro** antes de cada uno.

### Agente 1 — Finanzas

```
ROL: dueño de feature:finance, core/model/.../finance, core/domain/.../finance,
core/data/.../finance y core/database/.../finance.

Historias principales:
- Registrar un gasto o ingreso en < 5 s: importe, categoría, cuenta, fecha y nota opcional.
  Sugerencias de categorías recientes.
- Cuentas (efectivo, banco, tarjeta) con saldo; transferencias que no cuentan como gasto.
- Categorías jerárquicas con icono y color (semilla por defecto, editables).
- Pagos recurrentes (suscripciones, nómina, alquiler) que generan movimientos previstos.
- Planificador: presupuesto mensual por categoría, metas de ahorro con fecha objetivo y
  aportaciones, y previsión de fin de mes (ingresos previstos − gastos previstos − recurrentes).
- Búsqueda y filtros por fecha, categoría, cuenta e importe.

Pantallas: Resumen del mes · Movimientos (lista agrupada por día) · Alta/edición (bottom sheet) ·
Presupuestos · Metas de ahorro · Recurrentes · Cuentas y categorías.

Visualizaciones: donut de gasto por categoría, barras de gasto mensual (12 meses),
línea de saldo acumulado vs. previsión, barras de progreso de presupuesto, progreso de metas.

Reglas: Money en Long (céntimos) + ISO 4217; nunca Double. Las transferencias no son gasto.
Edge cases: reembolsos, cambio de mes, divisa distinta, categoría borrada con movimientos,
presupuesto superado, deshacer al borrar.
Exponer para Hoy: ObserveFinanceTodaySummary (gastado hoy, presupuesto restante del mes).
```

### Agente 2 — Alimentación

```
ROL: dueño de feature:nutrition y de las carpetas nutrition en core/model, domain, data y database.

Historias:
- Registrar comida por tipo (desayuno, comida, cena, snack) con alimentos y gramos o raciones.
- Alta rápida: recientes, favoritos, "repetir comida de ayer" y comidas guardadas (plantillas).
- Base de alimentos propia (por 100 g: kcal, proteínas, hidratos, grasas, fibra, azúcar, sal) y
  alimentos personalizados.
- Escáner de código de barras (ML Kit) con Open Food Facts como fuente opcional y caché local.
- Agua: +250 ml con un toque y objetivo diario.
- Objetivos de kcal y macros definidos por el usuario (sin lenguaje de dieta restrictiva).

Pantallas: Diario del día · Añadir comida/buscar alimento · Detalle de alimento · Agua · Objetivos.
Visualizaciones: anillo de kcal vs. objetivo, barras apiladas de macros, distribución por comida,
tendencia semanal y mensual de kcal y agua.
Health Connect (opt-in): NutritionRecord, HydrationRecord.
Exponer para Hoy: ObserveNutritionTodaySummary.
```

### Agente 3 — Deporte

```
ROL: dueño de feature:fitness y de las carpetas fitness en core/*.
Si hace falta un wrapper común de Health Connect, créalo en core/health (eres su dueño).

Historias:
- Registrar entreno de cardio (tipo, duración, distancia, kcal) y de fuerza (ejercicios,
  series × repeticiones × peso, RPE).
- Rutinas/plantillas de entreno reutilizables y récords personales automáticos.
- Pasos diarios y minutos activos desde Health Connect (opt-in), con alta manual siempre posible.
- Peso corporal y medidas.

Pantallas: Resumen de actividad · Historial de entrenos · Registrar entreno (cardio/fuerza) ·
Rutinas · Ejercicio (progreso y PRs) · Peso.
Visualizaciones: pasos vs. objetivo (barras 7/30 días), minutos activos semanales
(referencia OMS 150 min), volumen de fuerza por semana, progresión por ejercicio, línea de peso
con media móvil, calendario de actividad (heatmap).
Edge cases: Health Connect no instalado o permisos denegados, duplicados entre fuentes, unidades kg/lb y km/mi.
Exponer para Hoy: ObserveFitnessTodaySummary.
```

### Agente 4 — Dejar de fumar y beber

```
ROL: dueño de feature:quit y de las carpetas quit en core/*.

Diseña un motor genérico de "hábito a dejar" (QuitHabit) reutilizable para tabaco y alcohol y
ampliable a otros (cafeína, azúcar…).
Historias:
- Configurar el hábito: fecha de inicio, consumo y coste previos (cigarrillos/día y precio del
  paquete; bebidas/semana y precio medio), objetivo (dejarlo del todo o reducir).
- Contador en vivo de tiempo sin consumir, dinero ahorrado y unidades evitadas.
- Registrar antojo (intensidad 1–5, disparador, lugar, cómo lo gestioné) y registrar consumo o
  recaída sin culpa: el contador se reinicia pero se conservan el récord y el total acumulado.
- Hitos y beneficios de salud orientativos para el tabaco (20 min, 24 h, 72 h, 2 semanas,
  1 año…), con texto prudente y fuente citada, nunca consejo médico.
- Herramientas SOS: ejercicio de respiración guiado (animación) y lista de motivos personales.
- Modo reducción para alcohol: límite semanal y días sin alcohol.

Pantallas: Resumen por hábito · Configurar hábito · Registrar antojo/consumo · Hitos ·
SOS · Historial.
Visualizaciones: rachas (actual vs. récord), dinero ahorrado acumulado, antojos por hora del
día y disparador, consumo semanal vs. límite, calendario de días limpios.
Tono: empático y motivador; colores neutros para las recaídas. Enlace configurable a recursos
de ayuda profesional.
Privacidad: datos especialmente sensibles; respeta el bloqueo biométrico de la app.
Exponer para Hoy: ObserveQuitTodaySummary.
```

### Agente 5 — Trabajo

```
ROL: dueño de feature:work y de las carpetas work en core/*.

Historias:
- Fichar entrada y salida, y pausas, con un toque; jornada en curso visible y notificación
  persistente opcional.
- Proyectos o clientes y asignación de tiempo a cada uno.
- Sesiones de foco (temporizador tipo pomodoro configurable) vinculadas a proyecto, con
  servicio foreground correcto (Android 14+) y notificación.
- Horas objetivo semanales y cálculo de horas extra y saldo.
- Nota del día: logros y bloqueos.

Pantallas: Hoy en el trabajo · Temporizador de foco · Historial/semana · Proyectos ·
Objetivos de jornada.
Visualizaciones: horas por día de la semana vs. objetivo, distribución por proyecto (barras
horizontales), sesiones de foco por día, saldo de horas extra acumulado, heatmap horario
de productividad.
Edge cases: jornada que cruza medianoche, olvidé fichar la salida (sugerir cierre y permitir
editar), cambio de zona horaria, proceso muerto con el temporizador activo.
Exponer para Hoy: ObserveWorkTodaySummary.
```

### Agente 6 — Ánimo

```
ROL: dueño de feature:mood y de las carpetas mood en core/*.

Historias:
- Check-in rápido (1 toque): escala de 5 niveles con iconos o expresiones y colores accesibles.
- Check-in detallado (opcional y progresivo):
  · Emociones múltiples, agrupadas (alegría, calma, gratitud, ansiedad, tristeza, enfado,
    cansancio…), editables.
  · Energía (1–5) y estrés (1–5).
  · Factores/etiquetas: sueño, trabajo, deporte, familia, pareja, amigos, salud, dinero,
    clima, comida, ocio… y personalizados.
  · Nota libre y frase de gratitud opcional.
- Varios registros al día, con recordatorio configurable (WorkManager y notificación).
- Si el ánimo es muy bajo de forma sostenida (por ejemplo 5 días seguidos en el nivel 1), mostrar
  con delicadeza una tarjeta de recursos de apoyo. Sin diagnósticos.

Pantallas: Check-in rápido (también accesible desde Hoy) · Check-in detallado · Diario
(timeline) · Calendario de ánimo · Detalle de entrada · Gestión de emociones y factores.
Visualizaciones: calendario "year in pixels", línea de ánimo con media de 7 días, frecuencia de
emociones, impacto de factores (ánimo medio con y sin cada factor), energía vs. estrés.
Exponer para Hoy e Insights: ObserveMoodTodaySummary y ObserveMoodSeries(range).
```

---

## Fase 2 — Integración y visualización transversal (2 agentes en paralelo)

Empieza cuando al menos 3 features hayan completado su paso 4 (ViewModel).

### Agente 7 — Integrador (el mismo de la Fase 0)

```
ROL: integrador. Procesa todos los docs/handoff/*.md:
- Registra entities y DAOs en LifeTrackerDatabase, con migraciones y test de migración.
- Añade dependencias pedidas a libs.versions.toml.
- Conecta los grafos de navegación de cada feature en app/ y en la barra inferior.
- Añade al designsystem los componentes LT* pedidos (y sustituye los stubs locales de las features).
- Onboarding inicial: elegir áreas activas, divisa, unidades y objetivos básicos; permisos sólo en contexto.
- Ajustes (feature:settings): tema, divisa, unidades, recordatorios, bloqueo biométrico
  (BiometricPrompt), exportar/importar JSON y CSV cifrado y borrar todos los datos.
- Hoy (feature:dashboard): tarjetas reordenables por área usando los Observe*TodaySummary,
  acciones rápidas (gasto, comida, agua, ánimo, fichar, antojo), saludo según la hora y estados
  vacíos que invitan a configurar cada área.
```

### Agente 8 — Insights y visualizaciones

```
ROL: dueño de feature:insights y core/domain/.../insights.

- Selector de rango (7 d, 30 d, 90 d, año, personalizado) y de áreas.
- Pantalla de tendencias por área reutilizando los Observe*Series de cada dominio.
- Correlaciones (funciones puras con tests, mínimo 14 días de datos y lenguaje prudente):
  ánimo vs. deporte, ánimo vs. horas trabajadas, gasto vs. ánimo, antojos vs. estrés,
  kcal vs. ánimo y energía.
- Resumen semanal y mensual ("Tu semana"): logros, rachas y comparativa con el periodo anterior,
  con notificación de resumen opcional.
- Todas las gráficas con LTChartCard: titular en lenguaje natural, descripción para TalkBack,
  tabla de datos accesible alternativa y colores del área.
```

---

## Fase 3 — Calidad y release (2 agentes en paralelo)

### Agente 9 — QA, accesibilidad y rendimiento

```
ROL: QA. No añades features.
- Ejecuta android-reviewer sobre cada módulo; abre issues/historias por hallazgo y corrige los
  críticos y altos en ramas fix/.
- Auditoría de accesibilidad: TalkBack en todos los flujos, fuente al 200 %, contraste y
  objetivos táctiles. Añade tests de accesibilidad (checks de Compose / ATF).
- Rendimiento: Baseline Profile y Macrobenchmark de arranque y scroll de listas,
  compose compiler reports e informe de recomposiciones.
- Tests E2E de flujos críticos: onboarding, añadir gasto, check-in de ánimo, fichar,
  registrar antojo y añadir comida.
- Datos de prueba: generador de 6 meses de datos realistas (sólo en debug) para validar gráficas.
```

### Agente 10 — Release

```
ROL: release.
- Firma de release, R8 con reglas revisadas, App Bundle y versionCode/versionName automáticos.
- Privacidad: dataExtractionRules, política de privacidad, formulario de Data Safety y
  declaración de permisos de Health Connect.
- Iconos adaptativos y monocromo, splash screen API, capturas de tienda generadas desde los
  screenshot tests.
- CHANGELOG 1.0.0, tag v1.0.0 y pista interna de Play Console (te dejo los pasos manuales
  listados; no subas nada sin mi confirmación).
```

---

## Mapa de dependencias

```
Fase 0 (Arquitecto) ──► Fase 1: Finanzas · Alimentación · Deporte · Dejar hábitos · Trabajo · Ánimo  (en paralelo)
                                         │ (≥3 features con ViewModel listo)
                                         ▼
                        Fase 2: Integrador (Hoy, Ajustes, Onboarding) ‖ Insights
                                         ▼
                        Fase 3: QA/A11y/Perf ‖ Release
```

## Consejos para lanzar los agentes

- Usa un worktree o rama por agente para que no se pisen los archivos.
- Revisa el paso 0 (especificación) de cada feature antes de dejarles programar: es donde más barato sale corregir.
- Si dos agentes necesitan lo mismo (por ejemplo un componente de gráfica), lo decide el integrador, no ellos.
- Fusiona a main a menudo y vuelve a basar las ramas largas en main.
