---
name: android-testing
description: Estrategia y plantillas de testing para Life Tracker (unit tests de ViewModels y casos de uso con Turbine, DAOs Room, migraciones, Compose UI tests, screenshot tests con Roborazzi). Úsala al escribir o arreglar tests o al añadir lógica que necesite cobertura.
---

# Testing en Life Tracker

## Pirámide

1. **Unit (JVM, mayoría)**: modelos, mapeos, casos de uso, cálculos de métricas/objetivos, ViewModels.
2. **Integración**: DAOs Room (in-memory, Robolectric o instrumentado), migraciones, repositorios con fakes.
3. **UI**: Compose UI tests de `XxxScreen` (sin ViewModel), screenshot tests con Roborazzi de componentes `LT*` y pantallas en claro/oscuro/fuente grande.
4. **E2E mínimos**: flujos críticos (añadir gasto, registrar comida).

## Reglas

- **Fakes antes que mocks**: `FakeTransactionRepository` en `core:testing` con un `MutableStateFlow` interno. MockK sólo para colaboradores difíciles de falsear.
- `runTest` + `StandardTestDispatcher`; regla `MainDispatcherRule` en `core:testing`.
- `Clock` fijo (`TestClock`) y zona horaria explícita: los tests no dependen de la fecha real.
- Flows con **Turbine** (`flow.test { awaitItem() … }`).
- Nombres descriptivos: `` `when budget exceeded then status is OVER` ``.
- Un comportamiento por test; Given / When / Then.
- Cubrir siempre: caso feliz, vacío, error, y los edge cases listados en `life-tracker-domain`.

## Plantillas

```kotlin
class FinanceViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val repository = FakeTransactionRepository()

    @Test
    fun `emits Empty when there are no transactions this month`() = runTest {
        val viewModel = FinanceViewModel(ObserveMonthlySummaryUseCase(repository), …)
        viewModel.uiState.test {
            assertEquals(FinanceUiState.Loading, awaitItem())
            assertEquals(FinanceUiState.Empty, awaitItem())
        }
    }
}
```

```kotlin
class SleepScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsEmptyStateAndCta() {
        composeRule.setContent { LifeTrackerTheme { SleepScreen(state = SleepUiState.Empty, onAction = {}, onNavigateToDetail = {}) } }
        composeRule.onNodeWithText(context.getString(R.string.sleep_empty_cta)).assertIsDisplayed().assertHasClickAction()
    }
}
```

Migraciones Room: `MigrationTestHelper` con los esquemas exportados en `core/database/schemas`.

## Comandos

```bash
./gradlew testDebugUnitTest
./gradlew :core:database:connectedDebugAndroidTest
./gradlew verifyRoborazziDebug   # compara screenshots
./gradlew recordRoborazziDebug   # regenera tras cambio visual intencionado
```
