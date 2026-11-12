package com.example.lifet.feature.nutrition.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource

/**
 * Resuelve un recurso de texto dentro de un `LaunchedEffect` sin `LocalContext.current.getString`, que no se entera
 * de los cambios de configuración (idioma, tamaño de fuente). Cada llamada a [get] se hace con el idioma actual,
 * porque [resolve] se vuelve a ejecutar en composición y el `State` siempre guarda la última versión.
 */
internal class EffectMessages(
    private val resolver: State<(Int) -> String>,
) {
    fun get(
        @StringRes id: Int,
    ): String = resolver.value(id)
}

/**
 * [ids] son todos los recursos que el efecto puede necesitar. Se leen con [stringResource] en composición, que sí
 * es sensible a la configuración, y el `LaunchedEffect` consulta el resultado a través de [EffectMessages].
 */
@Composable
internal fun rememberEffectMessages(vararg ids: Int): EffectMessages {
    val resolved = ids.associateWith { stringResource(it) }
    val state = rememberUpdatedState<(Int) -> String> { id -> resolved[id].orEmpty() }
    return EffectMessages(state)
}
