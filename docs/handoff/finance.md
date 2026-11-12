# Handoff: finance

Estado: listo para integrar

Rama `feat/finance-complete`. Todo el área funciona de extremo a extremo (Room, repositorio, casos de uso, ViewModel, UI,
gráficas, demo y tests). La navegación existente (`financeGraph()`) no cambia.

## Pantalla (boceto)

```
┌ Finanzas ───────────────────────────┐
│ <  Marzo 2026  >                    │  cambio de mes (hasta 12 meses vista)
│ Resumen│Movimientos│Presupuestos│Cuentas
├─────────────────────────────────────┤
│ Disponible este mes                 │  héroe (LTMetricCard)
│  1.240,50 €                         │
│  Ingresos 2.100 € · gastos 859 €    │
│ ┌ Presupuesto del mes ────────────┐ │  barra de progreso; rojo si se supera
│ ┌ Gasto de los últimos 6 meses ───┐ │  LTChartCard COLUMNS
│ ┌ Saldo a final de mes ───────────┐ │  LTChartCard LINE
│ Gasto por categoría                 │  lista con barras (contra presupuesto o reparto)
│ Próximos recurrentes                │  previstos en cursiva
└─────────────────────────────────────┘      [ + ]  FAB -> LTQuickAddSheet
```

Movimientos: agrupados por día (más reciente primero) con total diario, búsqueda de texto sobre TODO el histórico
(notas y nombre de categoría), chips de tipo, categoría y cuenta. Tocar una fila edita; borrar muestra "Deshacer".
Cuentas: cuentas con saldo calculado, transferencias, recurrentes y categorías (editables y archivables).

## Entities y DAOs (YA registrados en LifeTrackerDatabase)

Registrado con líneas mínimas (nombres totalmente cualificados) en `entities = [...]` y `abstract fun financeDao()`.
No requiere acción: sólo conservar ambas listas al resolver conflictos de merge.

- `finance_accounts`, `finance_categories`, `finance_transactions`, `finance_transfers`, `finance_budgets`,
  `finance_recurring_rules`. Importes `Long` en unidades menores + `currency` ISO. Fechas `LocalDate`, `createdAt`
  `Instant` y `zoneId`.
- `FinanceDao` (público) y `FinanceDatabaseModule` (`core/database/.../finance/di/`) con su `@Provides`.
- `schemas/.../1.json` se regenera con el esquema; si hay conflicto con otras áreas, regenerarlo tras el merge
  (`./gradlew :core:database:kspDebugKotlin`).

## Hoy: qué cablear

`ObserveFinanceTodaySummary` (core:domain, `com.example.lifet.core.domain.finance`) emite `FinanceTodaySummary`:

| Campo | Uso sugerido en `LTMetricCard` |
|---|---|
| `availableThisMonth` | valor héroe ("Disponible este mes") |
| `spentToday` | subtítulo ("Hoy has gastado X") |
| `budgetRemaining` (`null` si no hay presupuestos) | subtítulo / progreso |
| `hasData` | `false` -> estado vacío que invita a registrar el primer gasto |

Formatear con `Money.format(locale)` de `util/MoneyFormat`. Icono y color: `Area.FINANCE.icon` y `areaColors.finance`.

## Insights

`ObserveFinanceSeries(range: DateRange)` emite `FinanceSeries` con un `FinanceDayPoint` por día hasta hoy
(`income`, `spent` neto de devoluciones, `balance`). Los días sin movimientos van con cero, no se omiten. Sólo
cuenta la divisa principal (la de la primera cuenta activa).

## Acciones rápidas para Hoy

- "Gasto": abrir la ruta de Finanzas (`FinanceDestination`); el FAB abre la hoja de alta. Si se quiere abrir la hoja
  directamente, falta un argumento de navegación (`FinanceDestination(openAdd = true)`): no lo he añadido para no
  cambiar la API pública de `feature/finance` que usa `app`.

## Recurrentes

`SyncRecurringTransactions` (core:domain) genera las ocurrencias hasta fin de mes o 30 días vista (lo que llegue más
tarde). Se invoca al abrir Finanzas. Si Hoy se abre antes que Finanzas, convendría llamarlo también al arrancar la app
o desde un `WorkManager` diario (pendiente: no hay WorkManager en esta iteración).

## Demo

`FinanceDemoDataGenerator` registrado con `@IntoSet` en `core/demo/.../finance/di/FinanceDemoModule`.
Escenarios: EMPTY (categorías y cuenta de partida), NEW_USER (3 días), REGULAR (6 meses: nómina el día 1, alquiler,
Netflix/Spotify/gimnasio, supermercado 2-3 veces por semana, ocio en fin de semana, un mes con presupuesto superado,
una devolución, ahorro mensual por transferencia), EDGE_CASES (USD y JPY, importes de 10^14 céntimos, notas de 800
caracteres, emojis y CJK/árabe, medianoche, cambio de hora, cuenta archivada, categoría archivada, presupuesto 0).

## Componentes LT* que faltan (stub local mientras tanto)

- Donut por categoría: `LTChartCard` no soporta donut. Se muestra la lista con barras de progreso.
- Filas de lista tipo "ListItem" con importe: hay una local `TransactionListItem` en `feature/finance/ui`.
- Selector de fecha y campo de texto de nombre: locales en `FormFields.kt`. Candidatos a `LTDateField` / `LTTextField`.
- Gráfica de saldo "real vs. previsto" (pedida en el prompt original): `LTChartCard` sólo admite una serie, así que se
  muestra el saldo a cierre de mes; falta un `LTChartCard` multiserie para pintar también la previsión.

## Dependencias nuevas para el catálogo

Ninguna. `core:testing` ahora depende de `core:data` (para `InMemoryFinanceRepository`); `core:domain` depende de
`core:data` (repositorio) y `core:demo` ya dependía de `core:data`.

## Decisiones

- "Previsto" no se almacena: un movimiento es previsto mientras su fecha sea posterior a hoy. Así nunca queda
  desfasado y pasa a real solo.
- Una devolución es un importe positivo en una categoría de gasto (reduce el gasto de la categoría, no es ingreso).
- Divisas: sólo se agrega la divisa de la primera cuenta activa; las demás se avisan en el resumen y se ven por cuenta.
  No hay conversión.
- Las categorías y la cuenta de partida se guardan sin nombre y se muestran localizadas (es/en).
- Borrar una ocurrencia de un recurrente no la regenera (la regla recuerda `generatedThrough`).

## Pendiente / no verificado

- No se ha ejecutado en dispositivo ni emulador: sólo compilación, lint y tests JVM.
- Sin tests de UI Compose ni Roborazzi (no hay infraestructura todavía); sí `@LTPreviews` en las pantallas.
- Sin test de migración: el esquema está en VERSION = 1 (la base se recrea en debug).
- Iconos de categoría: claves fijas mapeadas a `material-icons-extended`; la UI no permite elegir icono al crear una
  categoría (usa el genérico).
- Presupuesto: un único límite mensual por categoría (el mismo todos los meses).
