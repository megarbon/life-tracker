package com.example.lifet.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Multipreview estándar: claro, oscuro y fuente al 200%. Toda pantalla y componente `LT*` la usa. */
@Preview(name = "Claro", showBackground = true)
@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Fuente 2x", showBackground = true, fontScale = 2f)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
public annotation class LTPreviews
