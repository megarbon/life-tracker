---
name: feature-workflow
description: Flujo paso a paso para construir una feature completa de Life Tracker (de la especificación a los tests y la revisión) de forma incremental. Úsala cuando el usuario pida una nueva feature, pantalla o área, o diga "construye X".
---

# Flujo para construir una feature

Trabaja **un paso cada vez**. Al acabar cada paso: compila, ejecuta los tests afectados, resume lo hecho y para hasta que el usuario confirme (salvo que pida hacerlo todo seguido).

## Paso 0 — Especificación (sin código)
- Objetivo del usuario y la "acción principal" de la pantalla.
- Historias de usuario y criterios de aceptación.
- Edge cases y estados (ver `life-tracker-domain` y `compose-ui-design`).
- Estructura de paquetes y archivos (`ui/`, `domain/`, `data/`, `di/`, `navigation/`) con una línea por archivo.
- Preguntas abiertas para el usuario.

## Paso 1 — Modelo de dominio
Modelos inmutables en `core:model` (o `feature:x` si son exclusivos). Tests de la lógica pura.

## Paso 2 — Datos
Entities, DAO, migración (+ test), repositorio offline-first, mapeos, binding Hilt. Tests del DAO y del repositorio con fakes.

## Paso 3 — Dominio
Casos de uso sólo si aportan reglas o combinan fuentes. Tests unitarios.

## Paso 4 — ViewModel
`UiState` sellado, `Action`s, `Effect`s, `SavedStateHandle`. Tests con Turbine de todos los estados.

## Paso 5 — UI
`Route` + `Screen` sin estado, componentes `LT*`, todos los estados, accesibilidad, previews `@LTPreviews`, strings en recursos (es + en). Compose UI test de la pantalla y screenshot test.

## Paso 6 — Navegación e integración
Ruta `@Serializable`, `NavGraphBuilder.xGraph()`, entrada en la barra inferior o el dashboard. Tarjeta de resumen para "Hoy" si aplica.

## Paso 7 — Revisión
Lanza el agente `android-reviewer` sobre el diff. Corrige los hallazgos críticos y altos; lista el resto.

## Paso 8 — Cierre
`./gradlew spotlessApply detekt lint testDebugUnitTest`. Resume qué se hizo, qué se verificó y qué queda pendiente.
