---
name: compose-ui-design
description: Sistema de diseño, Material 3, principios de UX, accesibilidad, layouts adaptativos y rendimiento de Jetpack Compose para Life Tracker. Úsala siempre que crees o cambies una pantalla, un componente, el tema, animaciones o gráficas.
---

# Diseño, UI y UX de Life Tracker

## Principios de producto

1. **Registrar en < 5 segundos.** La acción más frecuente de cada área (gasto, comida, entreno, sueño) debe estar a 1–2 toques: FAB contextual, bottom sheet de alta rápida, valores recientes/favoritos sugeridos.
2. **"Hoy" primero.** El dashboard responde: ¿cómo voy hoy? Tarjetas por área con progreso hacia objetivos, no tablas.
3. **Datos → significado.** Cada gráfica tiene un titular en lenguaje natural ("Dormiste 40 min más que tu media").
4. **Amable, no culpabilizador.** Rachas y objetivos motivan; nunca mensajes negativos ni rojo para "fallos" personales (rojo sólo para errores y gastos que superan presupuesto).
5. **Privado por diseño.** Explicar por qué se pide cada permiso, en el momento en que se necesita.
6. **Divulgación progresiva.** Lo básico visible; lo avanzado (categorías, reglas, export) en segundo nivel.

## Sistema de diseño (`core:designsystem`)

- Tema `LifeTrackerTheme` con **color dinámico** (Android 12+) y paleta de marca de respaldo; claro y oscuro.
- Cada área tiene un **color de acento** semántico expuesto vía `LocalAreaColors`:
  finanzas = verde/teal, deporte = naranja, comida = amarillo/ámbar, sueño = índigo/violeta. Usarlos sólo como acento (iconos, progreso, gráficas), nunca para texto largo.
- Tokens: espaciado en múltiplos de 4dp (`Spacing.xs=4, sm=8, md=16, lg=24, xl=32`), formas de M3 (`shapes.medium` para tarjetas), tipografía M3 (`displaySmall` para cifras clave, `titleMedium` para cabeceras de tarjeta, `bodyMedium` para contenido).
- Componentes propios con prefijo `LT`: `LTMetricCard`, `LTProgressRing`, `LTEmptyState`, `LTErrorState`, `LTQuickAddSheet`, `LTAmountField`, `LTSectionHeader`, `LTTopAppBar`. Las features **no** usan componentes M3 crudos cuando existe un `LT*` equivalente.
- Iconos: Material Symbols Rounded.
- Números: tabulares (`fontFeatureSettings = "tnum"`) en cifras y tablas; formato con `NumberFormat`/`DateTimeFormatter` según locale, nunca concatenación manual.

## Navegación y layout

- Barra inferior (o `NavigationSuiteScaffold` para adaptarse a rail/drawer en tablet/plegable): **Hoy · Finanzas · Deporte · Comida · Sueño**; Ajustes e Insights desde la top bar o el dashboard.
- Edge-to-edge con `Scaffold` + `WindowInsets`; nada de alturas hardcodeadas de status/nav bar.
- Layouts adaptativos con `WindowSizeClass` (compact / medium / expanded); lista-detalle en pantallas anchas.
- Predictive back soportado.

## Estados obligatorios en cada pantalla

`Loading` (skeleton/shimmer sutil, no spinner a pantalla completa si hay datos previos) · `Empty` (ilustración + texto que explica + CTA) · `Error` (mensaje humano + Reintentar) · `Success`. Formularios: validación en línea, botón deshabilitado mientras sea inválido, errores debajo del campo.

## Accesibilidad (checklist)

- [ ] Objetivos táctiles ≥ 48dp
- [ ] `contentDescription` en iconos con significado; `null` en decorativos
- [ ] Gráficas con descripción semántica resumida (`Modifier.semantics { contentDescription = … }`)
- [ ] Agrupar tarjetas con `mergeDescendants = true`
- [ ] Contraste AA (4.5:1 texto, 3:1 componentes)
- [ ] Funciona con fuente al 200% y display size grande (sin texto cortado; usar `Modifier.heightIn(min=)` no `height`)
- [ ] El color nunca es el único portador de información
- [ ] Orden de foco lógico; navegación por teclado/D-pad

## Movimiento

Animaciones cortas (150–300 ms) con `animate*AsState`, `AnimatedContent`, `AnimatedVisibility`. Respetar "eliminar animaciones" del sistema. Haptic feedback al completar un registro u objetivo.

## Rendimiento de Compose

- Parámetros estables: `@Immutable`/`@Stable` en modelos de UI, `ImmutableList` en vez de `List`.
- `LazyColumn` con `key` estable y `contentType`.
- `remember` para cálculos caros; `derivedStateOf` cuando el estado cambia más que el resultado.
- Diferir lecturas de estado de alta frecuencia: `Modifier.offset { }`, `graphicsLayer { }`.
- Lambdas de eventos referenciadas (`viewModel::onAction`) o recordadas.
- No crear objetos (formatters, `Brush`, `Paint`) dentro de composables sin `remember`.
- Activar Compose compiler reports en debug cuando se investigue rendimiento; Baseline Profiles para el arranque.

## Previews

Cada `XxxScreen` y componente `LT*` tiene previews con una anotación multipreview propia `@LTPreviews` (claro, oscuro, fuente 2x, teléfono/tablet) y datos de ejemplo de `core:testing` o `PreviewParameterProvider`.

## Estructura de un composable de pantalla

```kotlin
@Composable
internal fun SleepRoute(onNavigateToDetail: (SleepSessionId) -> Unit, viewModel: SleepViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SleepScreen(state = state, onAction = viewModel::onAction, onNavigateToDetail = onNavigateToDetail)
}

@Composable
internal fun SleepScreen(state: SleepUiState, onAction: (SleepAction) -> Unit, onNavigateToDetail: (SleepSessionId) -> Unit, modifier: Modifier = Modifier) {
    // Sin ViewModel aquí: totalmente previsualizable y testeable.
}
```
Todo composable público acepta `modifier: Modifier = Modifier` como primer parámetro opcional y lo aplica al elemento raíz.
