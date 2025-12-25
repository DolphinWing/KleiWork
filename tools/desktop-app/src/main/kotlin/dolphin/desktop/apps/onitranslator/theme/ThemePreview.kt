package dolphin.desktop.apps.onitranslator.theme

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ThemePreview(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Material 3 Color Palette",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Buttons showing Primary, Secondary, and Tertiary
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { }) {
                    Text("Primary")
                }
                FilledTonalButton(onClick = { }) {
                    Text("Secondary")
                }
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Text("Tertiary")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Color Chips for better visualization
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ColorRow(
                    "Primary",
                    MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary,
                    MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer
                )
                ColorRow(
                    "Secondary",
                    MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary,
                    MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer
                )
                ColorRow(
                    "Tertiary",
                    MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary,
                    MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer
                )
                ColorRow(
                    "Error",
                    MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError,
                    MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ColorRow(
    label: String,
    baseColor: Color,
    onBaseColor: Color,
    containerColor: Color,
    onContainerColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.bodySmall)
        ColorChip("Base", baseColor, onBaseColor)
        ColorChip("Container", containerColor, onContainerColor)
    }
}

@Composable
private fun ColorChip(label: String, color: Color, onColor: Color) {
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 40.dp)
            .background(color, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = onColor, style = MaterialTheme.typography.labelSmall)
    }
}

@Preview
@Composable
fun PreviewThemes() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Light Theme Preview
        OniTranslatorTheme(darkTheme = false) {
            ThemePreview(modifier = Modifier.weight(1f))
        }

        // Dark Theme Preview
        OniTranslatorTheme(darkTheme = true) {
            ThemePreview(modifier = Modifier.weight(1f))
        }
    }
}
