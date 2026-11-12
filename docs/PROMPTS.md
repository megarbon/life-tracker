# Prompts para construir Life Tracker con Claude

Basado en las "10 técnicas de prompting para Android" (rol, arquitectura explícita, production-ready, incremental, edge cases, estructura, comentarios estratégicos, recomposición, modo revisión, refactor) y adaptado a este proyecto.

> Como `CLAUDE.md` y las skills de `.claude/skills/` ya fijan rol, stack y reglas, en el día a día los prompts pueden ser cortos. Las plantillas de abajo son para cuando quieras máxima precisión.

---

## 0. Prompt de arranque del proyecto (úsalo una sola vez)

```
Eres un desarrollador Android senior y diseñador de producto. Vamos a crear "Life Tracker",
una app para seguir mi vida: finanzas, deporte, comida y sueño, con un dashboard "Hoy" y una
sección de tendencias.

Lee CLAUDE.md y las skills android-architecture, compose-ui-design y life-tracker-domain.

Paso 1 (sólo este paso): crea el esqueleto del proyecto.
- Gradle Kotlin DSL, version catalog, convention plugins en build-logic/
- Módulos: app, core:{designsystem, ui, model, domain, data, database, datastore, common, testing}
  y feature:dashboard vacío con una pantalla placeholder
- Tema Material 3 con color dinámico, edge-to-edge, Hilt configurado, Navigation type-safe
- detekt + spotless + lint configurados
Requisitos: código production-ready, compila con ./gradlew assembleDebug.
Salida: árbol de carpetas, archivos completos, breve explicación y siguientes pasos.
```

## 1. Plantilla reutilizable (power prompt)

```
Eres un desarrollador Android senior.
Tarea: [qué quieres]
Contexto: [módulo / feature / archivos relevantes]
Stack: Kotlin · Jetpack Compose · Material 3 · MVVM + UDF · Hilt · Room · Coroutines + Flow
Requisitos:
- Production-ready, Clean Architecture, sin lógica en la UI
- Estados Loading / Success / Empty / Error
- Manejo de errores y edge cases: [lista]
- Accesible (TalkBack, fuente grande, 48dp)
- Tests: [unitarios del ViewModel / UseCase / DAO]
Salida:
- Archivos completos
- Explicación breve de decisiones
- Qué queda pendiente
```

## 2. Prompts incrementales para una feature (técnica 4)

Ejemplo con **Finanzas**. Envía uno, revisa, y después el siguiente:

1. `Usa la skill feature-workflow. Feature: finanzas. Paso 1: propón la estructura de paquetes y el modelo de dominio (Transaction, Category, Account, Budget). No escribas aún la capa de datos.`
2. `Paso 2: capa de datos — entities Room, DAOs, migración, repositorio offline-first y mapeos. Incluye tests del DAO.`
3. `Paso 3: casos de uso (AddTransaction, GetMonthlySummary, GetBudgetStatus) con tests.`
4. `Paso 4: ViewModel con StateFlow<FinanceUiState>, eventos de UI y efectos de un solo uso. Tests con Turbine.`
5. `Paso 5: UI en Compose siguiendo compose-ui-design: lista agrupada por día, resumen mensual, FAB para añadir, bottom sheet de alta rápida. Previews claro/oscuro/fuente grande.`
6. `Paso 6: revisa todo con el agente android-reviewer y aplica las correcciones.`

## 3. Edge cases y estados de error (técnica 5)

```
Para [pantalla/flujo], enumera primero todos los edge cases (sin datos, permisos denegados,
Health Connect no instalado, cambio de zona horaria, cambio de día a medianoche, importes
negativos, divisa distinta, base de datos migrada, proceso muerto y restaurado) y después
implementa su manejo con estados de UI explícitos.
```

## 4. Estructura antes que código (técnica 6)

```
Antes de escribir código para [feature], dame la estructura de paquetes y archivos
(ui/, domain/, data/, di/) con una línea por archivo explicando su responsabilidad.
```

## 5. Comentarios estratégicos (técnica 7)

```
Añade comentarios concisos sólo donde la lógica no sea obvia (el porqué, no el qué).
KDoc en las APIs públicas de core. Nada de comentarios en boilerplate.
```

## 6. Rendimiento de Compose (técnica 8)

```
Optimiza [pantalla] para recomposición: estabilidad de parámetros (@Immutable / colecciones
inmutables), remember / derivedStateOf, keys estables en LazyColumn, lambdas estables,
lectura de estado diferida (lambdas en Modifier.offset/graphicsLayer). Explica cada cambio y
cómo lo verificaste (Layout Inspector / compose compiler reports).
```

## 7. Modo revisión (técnica 9)

```
Usa el agente android-reviewer sobre [archivos / el diff actual]. Revisa rendimiento, fugas de
memoria, ciclos de vida, arquitectura, accesibilidad y seguridad. Prioriza por severidad y no
reescribas nada hasta que yo apruebe.
```

## 8. Refactor y migraciones (técnica 10)

```
Migra [LiveData → StateFlow | XML → Compose | lógica en Activity → ViewModel + UseCase]
en [archivos], manteniendo el comportamiento. Añade tests de caracterización antes del cambio.
```

## 9. Diseño y UX

```
Usa la skill compose-ui-design. Antes de programar [pantalla], describe: objetivo del usuario,
jerarquía de información, acción principal, estados vacíos/carga/error, microinteracciones y
cómo se ve en tablet y con fuente al 200%. Luego impleméntala.
```

---

### Consejos

- Una petición = una capa o un objetivo. Cuanto más acotado, menos errores.
- Pide siempre "explica qué no has verificado".
- Revisa el código como si fuera de un junior brillante: tú sigues siendo el arquitecto.
