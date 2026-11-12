---
name: android-architecture
description: Clean Architecture, MVVM + flujo unidireccional, modularización, Hilt, Room offline-first y Coroutines/Flow para Life Tracker. Úsala al crear o modificar módulos, repositorios, casos de uso, ViewModels, DI o navegación, o al decidir dónde va una pieza de código.
---

# Arquitectura Android de Life Tracker

Basada en la guía oficial de arquitectura de Android y en la app de referencia Now in Android.

## Capas

```
UI (Compose)  ──eventos──▶  ViewModel  ──▶  UseCase (opcional)  ──▶  Repository  ──▶  Room / DataStore / Health Connect
     ▲                          │
     └──────── StateFlow<UiState> ┘
```

- **UI**: composables sin lógica de negocio. Reciben `UiState` y lambdas. Separar `XxxRoute` (conecta con ViewModel) de `XxxScreen` (sin estado, previsualizable y testeable).
- **ViewModel**: transforma flujos de dominio en un único `StateFlow<UiState>` con `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. Recibe acciones vía funciones (`onAction(action)`).
- **Dominio** (`core:domain`, `feature:x/domain`): casos de uso con un solo `operator fun invoke`. Sólo cuando combinan repositorios o contienen reglas de negocio; no crear UseCases "pasarela".
- **Datos**: repositorios = única fuente de verdad. Interfaz en `core:data` (o `feature:x/data`), implementación `internal` + binding Hilt. Room es la fuente de verdad; sources externas (Health Connect) se sincronizan hacia Room.

## Plantillas

### UiState
```kotlin
sealed interface FinanceUiState {
    data object Loading : FinanceUiState
    data object Empty : FinanceUiState
    data class Success(
        val month: YearMonth,
        val balance: Money,
        val days: ImmutableList<DayGroup>,
    ) : FinanceUiState
    data class Error(@StringRes val messageRes: Int) : FinanceUiState
}
```
Usa `kotlinx.collections.immutable` para listas en estado (estabilidad en Compose).

### ViewModel
```kotlin
@HiltViewModel
class FinanceViewModel @Inject constructor(
    observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val addTransaction: AddTransactionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val month = savedStateHandle.getStateFlow(KEY_MONTH, YearMonth.now().toString())

    val uiState: StateFlow<FinanceUiState> = month
        .flatMapLatest { observeMonthlySummary(YearMonth.parse(it)) }
        .map { it.toUiState() }
        .catch { emit(FinanceUiState.Error(R.string.error_generic)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState.Loading)

    // Efectos de un solo uso (snackbar, navegación): Channel, nunca StateFlow
    private val _effects = Channel<FinanceEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: FinanceAction) { /* when (action) … */ }
}
```

### Recolección en UI
```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
```
Nunca `collectAsState()` a secas en pantallas.

### Resultado de operaciones
`core:common` define `sealed interface AppResult<out T> { Success, Failure(error: AppError) }`. Los repositorios capturan excepciones en el borde (IO, SQL, Health Connect) y devuelven errores tipados; nunca dejar escapar `Exception` genérica hasta la UI. No capturar `CancellationException`.

## Reglas

- Dispatchers inyectados con qualifiers (`@Dispatcher(IO)`); los repos cambian de hilo con `flowOn`/`withContext`, nunca la UI ni el ViewModel.
- `Clock`/`TimeZone` inyectados para que las fechas sean testeables.
- Sin dependencias Android en `core:model` ni en casos de uso.
- Visibilidad `internal` por defecto en módulos; sólo lo público que se necesita.
- Navegación: cada feature expone `fun NavGraphBuilder.financeGraph(...)` y su ruta `@Serializable`. La feature no conoce otras features: recibe lambdas `onNavigateToX`.
- Room: `exportSchema = true`, migraciones con `AutoMigration` o manuales + test de migración. Nunca `fallbackToDestructiveMigration` en release.
- DataStore para preferencias (unidades, divisa, objetivos). Nada de SharedPreferences.
- WorkManager para trabajo diferible (recordatorios, sync de Health Connect, backup). Inyectar con `HiltWorker`.
- Process death: estado de navegación/filtros en `SavedStateHandle`.

## Patrones de diseño recomendados

| Patrón | Uso en el proyecto |
|---|---|
| Repository | Abstracción de fuentes de datos |
| Use Case / Interactor | Reglas de negocio reutilizables |
| Mapper (extension fun) | `Entity.toDomain()`, `Domain.toEntity()`, `Domain.toUi()` |
| Strategy | Cálculo de métricas por tipo (ej. calorías por actividad) |
| Factory | Creación de registros desde distintas fuentes (manual, Health Connect, import CSV) |
| Observer (Flow) | Toda la reactividad |
| State machine (sealed) | UiState, flujos de onboarding y permisos |
| Adapter | `HealthConnectAdapter` traduce records de HC a modelos propios |

## Anti-patrones prohibidos

`!!`, `GlobalScope`, `runBlocking` en prod, `lateinit` para dependencias, pasar `Context`/`View` a ViewModels, lógica de negocio en composables, `MutableStateFlow` público, God-ViewModels con varios dominios, `feature → feature`, `Double` para dinero.
