#!/usr/bin/env bash
# Crea un módulo feature:<id> con pantalla placeholder, ruta type-safe y strings es/en.
# Uso: scripts/new-feature.sh <id> <Nombre> <titulo-es> <titulo-en> <descripcion-es> <descripcion-en>
set -euo pipefail

ID="$1"; NAME="$2"; TITLE_ES="$3"; TITLE_EN="$4"; DESC_ES="$5"; DESC_EN="$6"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MOD="$ROOT/feature/$ID"
PKG="com.example.lifet.feature.$ID"
SRC="$MOD/src/main/kotlin/${PKG//./\/}"
LOWER="$(echo "$ID" | tr '[:upper:]' '[:lower:]')"

if [ -d "$MOD" ]; then echo "feature/$ID ya existe, no se toca"; exit 0; fi
mkdir -p "$SRC/navigation" "$MOD/src/main/res/values" "$MOD/src/main/res/values-en"

cat > "$MOD/build.gradle.kts" <<EOF
plugins {
    id("lifetracker.android.feature")
}

android {
    namespace = "$PKG"
}
EOF

printf '<?xml version="1.0" encoding="utf-8"?>\n<manifest />\n' > "$MOD/src/main/AndroidManifest.xml"

cat > "$MOD/src/main/res/values/strings.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="${LOWER}_title">$TITLE_ES</string>
    <string name="${LOWER}_empty_title">Aún no hay nada aquí</string>
    <string name="${LOWER}_empty_message">$DESC_ES</string>
</resources>
EOF

cat > "$MOD/src/main/res/values-en/strings.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="${LOWER}_title">$TITLE_EN</string>
    <string name="${LOWER}_empty_title">Nothing here yet</string>
    <string name="${LOWER}_empty_message">$DESC_EN</string>
</resources>
EOF

cat > "$SRC/${NAME}Screen.kt" <<EOF
package $PKG

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme

/** Placeholder: la pantalla real se construye con el flujo feature-workflow. */
@Composable
internal fun ${NAME}Route(modifier: Modifier = Modifier) {
    ${NAME}Screen(modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ${NAME}Screen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.${LOWER}_title)) }) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            LTEmptyState(
                icon = Icons.Rounded.Home,
                title = stringResource(R.string.${LOWER}_empty_title),
                message = stringResource(R.string.${LOWER}_empty_message),
            )
        }
    }
}

@LTPreviews
@Composable
private fun ${NAME}ScreenPreview() {
    LifeTrackerTheme { ${NAME}Screen() }
}
EOF

cat > "$SRC/navigation/${NAME}Navigation.kt" <<EOF
@file:Suppress("MatchingDeclarationName") // Ruta + grafo viven juntos por convención XNavigation.kt

package $PKG.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ${PKG}.${NAME}Route
import kotlinx.serialization.Serializable

/** Ruta type-safe de ${TITLE_EN}. */
@Serializable
public data object ${NAME}Destination

/** La feature no conoce a otras features: la navegación saliente llega como lambdas \`onNavigateToX\`. */
public fun NavGraphBuilder.${ID}Graph() {
    composable<${NAME}Destination> { ${NAME}Route() }
}
EOF

echo "creado feature/$ID"
