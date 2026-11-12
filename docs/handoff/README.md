# Handoff entre agentes

Los ficheros compartidos son del **integrador**. Los agentes de área no los editan: dejan su petición en
`docs/handoff/<area>.md` y el integrador la procesa.

## Ficheros compartidos (sólo el integrador)

- `settings.gradle.kts`, `gradle/libs.versions.toml`, `build-logic/**`
- `app/**` (incluida la navegación raíz)
- `core/designsystem/**` (si falta un componente `LT*`, pídelo en tu handoff y usa un stub local)
- `core/database/.../LifeTrackerDatabase.kt` y `DatabaseModule.kt`

## Lo que SÍ es de cada área

- `feature/<area>/**`
- `core/model/.../<area>/`, `core/domain/.../<area>/`, `core/data/.../<area>/`
- `core/database/.../<area>/` (entities y DAOs; los DAOs se declaran `public` o `internal` según los use `core:data`)
- `core/demo/.../<area>/` (generador de datos de prueba)

## Cómo pedir una entidad nueva en la base de datos

Como Room necesita una lista única de entidades, el agente NO edita `LifeTrackerDatabase`. En su handoff escribe:

```
## Entities
- FinanceTransactionEntity (core/database/.../finance/FinanceTransactionEntity.kt)
- FinanceCategoryEntity (...)
## DAOs
- FinanceDao (...)
## Migración
- Solo tablas nuevas: no toca tablas existentes.
```

El integrador registra las entities, sube la versión y añade la migración `N → N+1` (solo `CREATE TABLE`,
que no puede romper datos existentes) con su test.

## Plantilla de handoff

Copia `docs/handoff/_TEMPLATE.md` a `docs/handoff/<area>.md`.
