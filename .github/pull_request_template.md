## Qué cambia

<!-- Resumen en 2-3 líneas y por qué. -->

## Definition of Done

- [ ] `./gradlew spotlessApply detekt lintDebug testDebugUnitTest :core:model:test assembleDebug` en verde
- [ ] Tests de la lógica nueva (ViewModel con Turbine: Loading / Empty / Success / Error)
- [ ] `@LTPreviews` de cada pantalla nueva (claro, oscuro, fuente grande)
- [ ] Strings en `values` (es) y `values-en` (en), sin texto hardcodeado
- [ ] Accesibilidad: `contentDescription`, objetivos táctiles ≥ 48 dp, resumen de gráficas para TalkBack
- [ ] Dinero en `Long` + `CurrencyCode`; `Clock` y dispatchers inyectados
- [ ] Si cambia el esquema de Room: migración y test (tras congelar el esquema)
- [ ] `docs/handoff/<área>.md` actualizado si afecta a otras áreas

## Cómo probarlo

<!-- Pasos o escenario de demo (Más → Opciones de desarrollador). -->
