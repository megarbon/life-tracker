# Skills y plugins externos recomendados

Las skills propias del proyecto están en `.claude/skills/` y se cargan solas. Estas externas las complementan; instálalas con los comandos de cada sección.

> Revisado en noviembre de 2026. Son repos de terceros: echa un vistazo al código antes de instalar y prefiere siempre los oficiales.

## Imprescindibles

### 1. Android Skills oficiales de Google — `android/skills` ⭐ oficial
Skills basadas en developer.android.com, pensadas para las áreas donde los LLM fallan más: `jetpack-compose`, `navigation`, `performance`, `security`, `testing/testing-setup`, `build-system/agp/agp-9-upgrade`, `profilers/android-profiler`, `play`, `wear`, `device-ai`, `android-cli`…
```
/plugin marketplace add android/skills
/plugin install android-skills@android-skills
```
Alternativa con Android CLI: `android skills add --all --project=.`

### 2. Chris Banes skills — `chrisbanes/skills` (Apache 2.0)
De un referente de Compose (ex-Google). Muy útiles: `compose-state-and-effects`, `compose-performance`, `compose-component-design`, `compose-animations`, `compose-ui-testing-patterns`, `kotlin-concurrency-and-flow`, `kotlin-api-design`, `gradle-run`.
```
/plugin marketplace add chrisbanes/skills
/plugin install chrisbanes-skills@chrisbanes-skills
```

### 3. Skills de Anthropic — `anthropics/skills`
`skill-creator` (para mejorar/medir nuestras skills propias) y skills de diseño/ejemplo.
```
/plugin marketplace add anthropics/skills
/plugin install example-skills@anthropic-agent-skills
```

## Opcionales

| Repo | Para qué | Instalación |
|---|---|---|
| `dpconde/claude-android-skill` (MIT, WIP) | Clean Architecture al estilo Now in Android, generador de módulos de feature, plantillas de version catalog | `git clone https://github.com/dpconde/claude-android-skill.git ~/.claude/skills/claude-android-skill` |
| `yschimke/skills` | `compose-preview`: renderiza `@Preview` a PNG fuera de Android Studio (útil para que Claude vea la UI) | ver README del repo |
| `wshobson/agents` → `mobile-android-design` | Guía de diseño Material para móvil | `/plugin marketplace add wshobson/agents` |

## No recomendadas (por ahora)

Colecciones masivas tipo "awesome-*" con cientos de skills genéricas: añaden ruido al contexto y se solapan con las nuestras. Mejor pocas y buenas.

## Fuentes

- https://github.com/android/skills · https://developer.android.com/skills
- https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html
- https://github.com/chrisbanes/skills
- https://github.com/anthropics/skills
- https://github.com/dpconde/claude-android-skill
- https://claudemarketplaces.com/skills/wshobson/agents/mobile-android-design
