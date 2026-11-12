# Handoff entre agentes

Los ficheros compartidos son del **integrador**. Los agentes de área no los editan: dejan su petición en
`docs/handoff/<area>.md` y el integrador la procesa.

## Ficheros compartidos (sólo el integrador)

- `settings.gradle.kts`, `gradle/libs.versions.toml`, `build-logic/**`
- `app/**` (incluida la navegación raíz)
- `core/designsystem/**` (si falta un componente `LT*`, pídelo en tu handoff y usa un stub local)
- `core/database/.../DatabaseModule.kt`

## Lo que SÍ es de cada área

- `feature/<area>/**`
- `core/model/.../<area>/`, `core/domain/.../<area>/`, `core/data/.../<area>/`
- `core/database/.../<area>/` (entities y DAOs; los DAOs se declaran `public` o `internal` según los use `core:data`)
- `core/demo/.../<area>/` (generador de datos de prueba)

## Base de datos (regla de la iteración 2)

Cada área **registra sus propias entities y DAOs** en `LifeTrackerDatabase` (una línea en `entities = [...]` y un
`abstract fun xDao()`), y se queda en `VERSION = 1` mientras no haya datos reales: en builds debug la base se recrea
sola al cambiar el esquema (`DatabaseModule`). Los conflictos de merge en esas dos listas se resuelven conservando
ambas líneas. Antes del primer uso real se congela el esquema y desde ahí toda entidad nueva exige migración con test.

## Plantilla de handoff

Copia `docs/handoff/_TEMPLATE.md` a `docs/handoff/<area>.md`.
