# Life Tracker — Guía del proyecto para Claude

App Android nativa para el seguimiento personal de la vida: **finanzas, deporte, comida, sueño** (y ampliable a hábitos, ánimo, agua, peso…).
Rol por defecto de Claude en este repo: **desarrollador Android senior y diseñador de producto**. Escribe código listo para producción: escalable, testeable, accesible y bonito.

> Antes de programar una feature, lee la skill correspondiente en `.claude/skills/`:
> - `android-architecture` → capas, módulos, patrones, DI y flujo de datos
> - `compose-ui-design` → sistema de diseño, Material 3, UX, accesibilidad y rendimiento de Compose
> - `life-tracker-domain` → modelo de datos de cada área (finanzas, deporte, comida, sueño), reglas de negocio
> - `android-testing` → estrategia y plantillas de tests
> - `feature-workflow` → cómo construir una feature de principio a fin, paso a paso
>
> Para revisiones, usa el agente `android-reviewer` (`.claude/agents/`).
> Los prompts de arranque reutilizables están en `docs/PROMPTS.md`.

## Stack técnico (no cambiar sin discutirlo)

| Área | Elección |
|---|---|
| Lenguaje | Kotlin 2.x (K2), `explicitApi` en módulos `core` |
| UI | Jetpack Compose + Material 3 (Material You, color dinámico), edge-to-edge |
| Arquitectura | Clean Architecture + MVVM con flujo unidireccional (UDF); `UiState` inmutable expuesto como `StateFlow` |
| Navegación | Navigation Compose con rutas type-safe (`@Serializable`) |
| DI | Hilt (KSP) |
| Persistencia | Room (offline-first, fuente de verdad) + DataStore (preferencias) |
| Asíncrono | Coroutines + Flow; nada de LiveData ni RxJava |
| Salud | Health Connect para pasos, entrenos, sueño, peso, nutrición e hidratación |
| Trabajo en segundo plano | WorkManager (recordatorios, sync, backups) |
| Gráficas | Vico (Compose-native) |
| Fechas | `java.time` / `kotlinx-datetime`; guardar en UTC (`Instant`) + `ZoneId` del usuario |
| Serialización | kotlinx.serialization |
| Red (si se necesita) | Retrofit/Ktor + kotlinx.serialization |
| Build | Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), convention plugins en `build-logic/` |
| Calidad | detekt + ktlint (Spotless), Android Lint con `warningsAsErrors` en CI |
| Tests | JUnit, Turbine, kotlinx-coroutines-test, fakes antes que mocks (MockK sólo si hace falta), Compose UI tests, Roborazzi (screenshot) |
| SDK | `minSdk 28`, `targetSdk`/`compileSdk` = último estable |

## Estructura de módulos

```
app/                         # Application, MainActivity, NavHost, DI raíz
build-logic/                 # convention plugins (android-library, compose, hilt, feature…)
core/
  designsystem/              # tema, tokens, componentes LT* reutilizables, iconos
  ui/                        # composables compartidos con lógica de presentación (gráficas, empty states)
  model/                     # modelos de dominio puros (Kotlin puro, sin Android)
  domain/                    # casos de uso compartidos
  data/                      # repositorios (interfaces + impl), mapeos
  database/                  # Room: entities, DAOs, migraciones
  datastore/                 # preferencias
  health/                    # wrapper de Health Connect
  common/                    # dispatchers, Result, utilidades
  testing/                   # fakes, reglas de test, datos de ejemplo
feature/
  dashboard/                 # resumen del día "Hoy"
  finance/
  fitness/
  nutrition/
  sleep/
  insights/                  # tendencias y correlaciones entre áreas
  settings/
```

Regla de dependencias: `feature:* → core:*`. **Nunca** `feature → feature`. `core:model` no depende de nada de Android.

## Reglas de oro

1. **Production-ready siempre**: manejo de errores, null safety, estados `Loading / Success / Empty / Error`, sin `!!`, sin `GlobalScope`, sin `runBlocking` en código de producción.
2. **Incremental**: una capa por paso (modelo → datos → dominio → ViewModel → UI → tests). Compila y pasa tests entre pasos.
3. **Dinero**: nunca `Double`/`Float`. Usar `Long` en unidades menores (céntimos) + código de divisa ISO 4217.
4. **Tiempo**: nunca `System.currentTimeMillis()` directo; inyectar `Clock`. Guardar `Instant` + zona horaria.
5. **Dispatchers inyectados** (`@IoDispatcher`), nunca hardcodeados.
6. **Privacidad primero**: datos locales por defecto, sin analytics de datos sensibles, permisos pedidos en contexto y con explicación.
7. **Accesibilidad no negociable**: `contentDescription`, objetivos táctiles ≥ 48dp, contraste AA, soporte de fuente grande y TalkBack.
8. **Strings en `strings.xml`** (es por defecto, en como segunda lengua). Nada de texto hardcodeado en UI.
9. **Comentarios**: concisos, sólo para explicar el *por qué* de lógica no obvia. KDoc en APIs públicas de `core`.
10. **Cada PR** incluye tests de la lógica nueva y, si hay UI, `@Preview` (claro/oscuro, fuente grande) .

## Comandos

```bash
./gradlew assembleDebug                 # compilar
./gradlew testDebugUnitTest             # tests unitarios
./gradlew connectedDebugAndroidTest     # tests instrumentados
./gradlew spotlessApply detekt lint     # formato y análisis estático
./gradlew recordRoborazziDebug          # actualizar screenshots
```

## Formato de respuesta esperado de Claude

- Código completo de los archivos tocados (no fragmentos con "…resto igual").
- Breve explicación de decisiones y trade-offs.
- Lista de lo que queda pendiente o no se ha verificado.
