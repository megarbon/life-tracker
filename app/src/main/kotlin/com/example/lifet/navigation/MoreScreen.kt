package com.example.lifet.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.lifet.R
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import kotlinx.serialization.Serializable

/** Destino del menú "Más"; vive en `app` porque sólo es un índice hacia otras áreas. */
@Serializable
internal data object MoreDestination

/** Entrada del menú "Más": áreas secundarias que no caben en la barra inferior. */
internal data class MoreEntry(
    @param:StringRes val label: Int,
    val destination: Any,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoreScreen(
    entries: List<MoreEntry>,
    onEntryClick: (MoreEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.more_title)) }) },
    ) { innerPadding ->
        LazyColumn(contentPadding = PaddingValues(top = innerPadding.calculateTopPadding())) {
            items(entries, key = { it.label }) { entry ->
                ListItem(
                    headlineContent = { Text(stringResource(entry.label)) },
                    // 56dp: por encima del mínimo táctil de 48dp.
                    modifier =
                        Modifier
                            .heightIn(min = 56.dp)
                            .clickable { onEntryClick(entry) }
                            .padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@LTPreviews
@Composable
private fun MoreScreenPreview() {
    LifeTrackerTheme {
        MoreScreen(
            entries =
                listOf(
                    MoreEntry(R.string.more_quit, Unit),
                    MoreEntry(R.string.more_work, Unit),
                    MoreEntry(R.string.more_settings, Unit),
                ),
            onEntryClick = {},
        )
    }
}
