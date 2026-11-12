# Guía de diseño de Life Tracker

> Objetivo: que la app se sienta como una app nativa de iOS —limpia, calmada, fluida— pero siendo una app Android
> de verdad (edge-to-edge, botón atrás predictivo, hápticos del sistema, fuente y tamaño de pantalla del usuario).
> Se aplica a toda pantalla y componente nuevo. Las skills `compose-ui-design` y `android-architecture` siguen
> mandando para accesibilidad y arquitectura; esta guía manda para **aspecto y comportamiento visual**.

## 1. Principios

1. **Calma antes que densidad.** Una idea por tarjeta, mucho aire, una sola cifra "héroe" por pantalla.
2. **El contenido es el protagonista.** Sin sombras, bordes gruesos ni fondos de colores chillones. Las capas se
   distinguen por tono de superficie y por separadores finos, no por elevación.
3. **Un único acento.** El color de marca se usa con cuentagotas (acción principal, selección). El color de cada
   área solo aparece en su icono, su progreso y sus gráficas.
4. **Todo responde.** Cada toque tiene feedback (estado pulsado, háptico, animación corta). Nada salta de golpe.
5. **Nada se rompe.** Ningún texto cortado, ninguna altura fija, ningún desbordamiento con fuente al 200 % o en
   pantallas pequeñas (sección 9). Un componente que falla ahí no está terminado.
6. **Amable.** Lenguaje cercano, nunca culpabilizador; el rojo es solo para errores y presupuestos superados.

## 2. Color

Dos capas de superficie, como las listas agrupadas de iOS: un **fondo** y **celdas** (tarjetas) encima.

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `background` | `#F2F2F7` | `#000000` | Fondo de pantalla |
| `surface` (celda/tarjeta) | `#FFFFFF` | `#1C1C1E` | Tarjetas y filas |
| `surfaceElevated` | `#FFFFFF` | `#2C2C2E` | Hojas, diálogos, menús |
| `separator` | `onSurface` al 12 % | `onSurface` al 16 % | Línea fina entre filas (0,5 dp visual) |
| `label` | `#000000` | `#FFFFFF` | Texto principal |
| `labelSecondary` | 60 % de `label` | 60 % | Subtítulos, unidades |
| `labelTertiary` | 30 % de `label` | 30 % | Placeholders, deshabilitado |
| `accent` | marca (teal `#00796B`) | `#4DB6AC` | Acción principal, selección |
| `destructive` | `#D32F2F` | `#EF5350` | Borrar, errores |

- Fondo **negro puro** en oscuro (OLED): las celdas `#1C1C1E` destacan sin sombras.
- Colores de área (`AreaColors`): se usan al 100 % en iconos y gráficas y al **12–16 % de opacidad** como fondo
  del azulejo del icono. Nunca como fondo de una tarjeta entera ni para texto largo.
- El color nunca es el único portador de información (añade icono o texto). Contraste AA siempre.
- **Color dinámico (Material You) desactivado por defecto**: la coherencia de marca vale más que seguir el fondo de
  pantalla. Se ofrece como opción en Ajustes → Apariencia.

## 3. Tipografía

Escala inspirada en la de iOS (Dynamic Type). Todo en `sp`, así respeta la fuente del usuario.

| Rol iOS | Tamaño / interlínea | Peso | Rol Material | Uso |
|---|---|---|---|---|
| Large Title | 34 / 41 | Bold | `headlineLarge` | Título de pantalla (barra grande) |
| Title 1 | 28 / 34 | Bold | `headlineMedium` | Cifra héroe secundaria |
| Title 2 | 22 / 28 | Semibold | `titleLarge` | Título de sección grande |
| Title 3 | 20 / 25 | Semibold | `titleMedium` | Título de tarjeta |
| Headline | 17 / 22 | Semibold | `titleSmall` | Fila destacada, botón |
| Body | 17 / 22 | Regular | `bodyLarge` | Texto base |
| Callout | 16 / 21 | Regular | `bodyMedium` | Texto de apoyo |
| Subhead | 15 / 20 | Regular | `labelLarge` | Subtítulo de fila |
| Footnote | 13 / 18 | Regular | `bodySmall` | Notas, cabeceras de sección (mayúsculas) |
| Caption | 12 / 16 | Regular | `labelSmall` | Etiquetas de gráficas |

- **Cifra héroe**: 48–56 sp, `Bold`, números tabulares (`tnum`), `maxLines = 1` con `autoSize` hasta 28 sp.
- Números siempre **tabulares** (ya activo en `displaySmall`, `headlineMedium`, `titleLarge`; ampliar a `bodyLarge`
  y `labelLarge` donde haya cifras). Formato con `NumberFormat`/`Money.format`, nunca concatenando.
- Fuente: la de Apple (SF) tiene licencia solo para plataformas Apple, así que no se incluye. Se usa la del sistema
  (Roboto/Google Sans según el móvil) con `letterSpacing` ligeramente negativo en títulos grandes (−0,2 sp), o
  **Inter** (licencia OFL) si se quiere clavar el aspecto (decisión abierta, sección 12).
- Máximo **dos pesos** por pantalla (Regular y Semibold/Bold). Sin cursivas ni subrayados salvo enlaces.

## 4. Espaciado, forma y tamaños

Rejilla de 4 dp (`Spacing`: xs 4 · sm 8 · md 16 · lg 24 · xl 32). Reglas:

- **Margen horizontal de pantalla: 16 dp** (20 dp en ≥ 600 dp de ancho). En pantallas anchas el contenido se centra
  con **ancho máximo 600 dp**.
- Entre tarjetas: 12 dp. Entre secciones: 24–32 dp. Padding interior de tarjeta: 16 dp.
- **Fila de lista**: alto mínimo **52 dp** (una línea) / 64 dp (dos líneas). Objetivo táctil ≥ 48 dp siempre.
- **Esquinas**: tarjetas y celdas 16 dp · azulejos de icono 8 dp (36 dp de lado) · botones y chips 12 dp o cápsula ·
  hojas inferiores 28 dp arriba · campos de texto 12 dp. Las esquinas de un contenedor y las de lo que lleva dentro
  son concéntricas (radio interior = exterior − padding).
- **Sin sombras** (`elevation = 0`). Los diálogos y menús flotantes pueden llevar una sombra muy suave.
- Iconos: Material Symbols **Rounded**, trazo regular, 24 dp (20 dp en filas densas). Alineados al centro óptico de
  la primera línea de texto.
- Separadores: línea de 0,5 dp equivalente (`Dp.Hairline`), con **sangría** que empieza en el texto (no en el icono).

## 5. Estructura de pantalla

1. **Barra grande colapsable** (`LTTopAppBar`): título Large Title a la izquierda que se encoge al hacer scroll;
   acciones como iconos (máx. 2). Atrás con flecha solo si no es raíz.
2. **Contenido en listas agrupadas**: `LTGroupedList` (secciones con cabecera en mayúsculas 13 sp y celdas redondeadas
   con separadores internos). Es el patrón por defecto de listados y ajustes.
3. **Héroe arriba**: la cifra principal con su contexto en una línea ("de 2.000 kcal").
4. **Detalle debajo**: tarjetas, gráficas, historial.
5. **Acción principal**: botón de acción flotante tonal para "Registrar", o botón relleno anclado abajo en
   formularios. Una sola acción primaria por pantalla.
6. **Barra inferior**: 5 destinos con etiqueta siempre visible; icono *outlined* en reposo y *filled* al seleccionar;
   indicador discreto (sin píldora de color fuerte). Se oculta en pantallas de detalle.

## 6. Componentes (catálogo `LT*`)

Existentes que hay que **pulir** a esta guía: `LTMetricCard`, `LTStatTile`, `LTChartCard`, `LTEmptyState`,
`LTErrorState`, `LTChipGroup`, `LTQuickAddSheet`, `LTDateRangeSelector`, `LTProgressRing`, `LTTopAppBar`.

A **crear**:

| Componente | Qué es | Equivalente iOS |
|---|---|---|
| `LTGroupedList` / `LTListSection` / `LTListRow` | Lista agrupada con icono en azulejo, título, valor, chevron | `Form` / `List(.insetGrouped)` |
| `LTSegmentedControl` | Selector de 2–4 opciones (Día · Semana · Mes) | `Picker(.segmented)` |
| `LTToggleRow`, `LTStepperRow`, `LTValueRow` | Filas de ajustes | Filas de Ajustes |
| `LTPrimaryButton`, `LTSecondaryButton`, `LTTextButton` | Relleno, tonal y texto; altura 50 dp, esquina 12 dp | `.borderedProminent` |
| `LTSwipeRow` | Fila con acciones al deslizar (borrar, editar) | `swipeActions` |
| `LTSheet` | Hoja con **grabber** y alturas (media / completa) | `presentationDetents` |
| `LTBadge`, `LTIconTile` | Pastilla de estado y azulejo de icono | Etiquetas e iconos de Ajustes |

Reglas de componente:

- Cada componente: parámetros mínimos, `modifier` primero tras los obligatorios, estados `enabled/pressed/disabled`,
  `@LTPreviews`, `contentDescription` y semántica ya resueltos dentro.
- **Estado pulsado**: la celda se oscurece/aclara un 8 % (no ripple grande); en botones, escala a 0,97 con muelle.
- Nunca altura fija: `heightIn(min = …)`. Nunca ancho fijo de texto: `weight(1f)` + `maxLines` + `Ellipsis`.
- Un componente nuevo se añade a la **galería** (Más → Opciones de desarrollador) antes de usarse en pantallas.

## 7. Movimiento y tacto

- **Muelles, no tweens**: `spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)` para tamaño,
  posición y escala; `tween(200–300, FastOutSlowInEasing)` solo para opacidad.
- **Navegación**: entrada deslizando desde la derecha con el destino anterior desplazándose ~30 % a la izquierda y
  atenuándose; salida al revés. Hojas: suben desde abajo con el fondo atenuado al 32 %. Botón atrás **predictivo**.
- **Cifras**: al cambiar, `AnimatedContent` con deslizamiento vertical corto (ya en `LTMetricCard`); anillos y barras
  se animan al entrar una sola vez.
- **Listas**: inserción/borrado con `animateItem()`; nunca saltos.
- **Hápticos** (`LocalHapticFeedback`): `Confirm` al guardar o completar, `Reject` en error de validación,
  `SegmentTick` al cambiar de segmento, `LongPress` al abrir menú contextual. Nunca en scroll.
- **Deshacer, no confirmar**: borrar es inmediato con snackbar "Deshacer" (confirmación solo para borrados
  destructivos de muchos datos).
- Respeta "eliminar animaciones" del sistema (`rememberReducedMotion()`): sin muelles ni deslizamientos, solo fundido.
- Presupuesto: ninguna interacción tarda más de 300 ms en responder visualmente.

## 8. Gráficas y datos

- Una gráfica = **un titular en lenguaje natural** arriba ("Gastaste un 12 % menos que la semana pasada") y la
  gráfica debajo. Ver `LTChartCard`.
- Barras con esquinas redondeadas (4 dp) y separación; líneas de 2,5 dp con puntos solo al pulsar; relleno de área con
  degradado del 24 % al 0 % del color del área.
- Sin rejilla pesada: solo 2–3 líneas horizontales finas (separador) y etiquetas de 12 sp en `labelSecondary`.
- Pulsar muestra un indicador con valor y fecha; el eje X con 4–6 etiquetas como máximo.
- Siempre alternativa accesible: resumen para TalkBack y "Ver como tabla".
- Estados vacíos de gráfica: silueta gris con texto "Aún no hay datos" (nunca un eje vacío).

## 9. Cero fallos de tamaño (reglas duras)

1. **Sin `height`/`width` fijos** en contenedores con texto; usa `heightIn(min)`, `widthIn(max)`, `weight`,
   `fillMaxWidth`, `aspectRatio`.
2. Todo texto con `maxLines` y `overflow` definidos; los que no pueden cortarse permiten crecer en vertical.
3. Todo `Row` con texto largo reparte con `weight(1f)` el que cede, y deja `wrapContentWidth` al valor numérico.
4. **Insets**: `Scaffold` + `WindowInsets`; nada de alturas de barra de estado/navegación a mano; teclado con
   `imePadding()` en formularios y hojas.
5. **Scroll siempre posible**: cualquier pantalla con formulario o contenido variable es desplazable.
6. Imágenes e iconos con tamaño en `dp` explícito y `contentScale` definido.
7. Contenido centrado con ancho máximo en pantallas anchas; en teléfonos pequeños, una columna.
8. Listas con `key` y `contentType`; elementos con altura mínima, nunca fija.

**Matriz de verificación** (antes de dar una pantalla por terminada, en preview o en el móvil):

| Eje | Valores |
|---|---|
| Fuente | 0,85 · 1,0 · 1,3 · **2,0** |
| Tamaño de pantalla | Pequeño 360×640 · normal 411×891 · grande 600×960 |
| Tema | Claro · Oscuro |
| Orientación | Vertical · Horizontal |
| Datos | Vacío · Habitual · Casos límite (texto de 200 caracteres, importe de 14 dígitos, emoji) |

`@LTPreviews` cubre claro/oscuro/2x; añadir una preview de **pantalla pequeña** y otra de **casos límite**. Cuando
exista Roborazzi, esas previews se convierten en capturas de referencia que fallan en CI si algo cambia.

## 10. Voz y contenido

- Español de tú, cercano y breve. Verbos en infinitivo en acciones: "Registrar gasto", "Añadir comida".
- Títulos en frase, no en Mayúsculas Cada Palabra. Cabeceras de sección en MAYÚSCULAS pequeñas.
- Cifras con unidad y contexto: "1.240,50 €", "de 2.000 kcal", "hace 3 días".
- Estados vacíos que enseñan: icono + 1 frase de qué es + 1 botón claro.
- Errores humanos y accionables: "No se pudo guardar. Inténtalo de nuevo." + botón; nunca códigos ni excepciones.
- Todo en `strings.xml` (es y en). Sin signos de exclamación salvo celebraciones puntuales.

## 11. Cómo se aplica (hoja de ruta)

| Fase | Qué | Dónde | Riesgo |
|---|---|---|---|
| 1. Tokens | Colores, tipografía, formas, `Dp.Hairline` y fondo agrupado | `core/designsystem/theme/` | Bajo: cambia todo de golpe sin tocar pantallas |
| 2. Componentes | `LTGroupedList`, `LTListRow`, `LTSegmentedControl`, botones, `LTSheet`, `LTSwipeRow` + pulir los existentes | `core/designsystem/component/` | Medio: se prueban en la galería |
| 3. Navegación | Transiciones tipo iOS, barra inferior con iconos filled/outlined, barra grande | `app/navigation/`, `LTTopAppBar` | Bajo |
| 4. Pantallas | Migrar Hoy, Ánimo, Finanzas, Alimentación, Deporte a los componentes nuevos | `feature/*` | Medio: una pantalla por commit |
| 5. Red de seguridad | Roborazzi con la matriz de la sección 9 | `core/testing`, CI | Medio |

Las fases 1–3 son del integrador (tocan `core/designsystem` y `app`). La fase 4 se hace **cuando terminen los agentes
de área** para no pisar su trabajo; mientras tanto ellos siguen las secciones 4, 7, 9 y 10 con los componentes actuales.

## 12. Decisiones abiertas (con mi recomendación)

1. **Color dinámico**: ¿apagado por defecto (recomendado) o siguiendo el fondo de pantalla?
2. **Fuente**: ¿la del sistema (recomendado, cero riesgo) o Inter incluida en la app (más fiel a iOS, +300 KB)?
3. **Tema por defecto**: ¿seguir al sistema (recomendado) o forzar oscuro como las apps de salud de iOS?

## 13. Checklist de pantalla terminada

- [ ] Barra grande colapsable y margen de 16 dp
- [ ] Una cifra héroe con contexto y números tabulares
- [ ] Listas agrupadas con separadores con sangría; sin sombras
- [ ] Solo componentes `LT*`; ninguno con altura fija
- [ ] Estados Loading (skeleton), Empty (enseña), Error (reintentar) y Success
- [ ] Animaciones con muelle, háptico al guardar, snackbar "Deshacer" al borrar
- [ ] Probada con fuente 2x, pantalla pequeña, oscuro y casos límite
- [ ] `contentDescription`, objetivos ≥ 48 dp, contraste AA
- [ ] Textos en `strings.xml` (es/en)
