package dolphin.desktop.apps.onitranslator

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.compose.OniTranslatorM3Theme
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_refresh
import dolphin.desktop.apps.onitranslator.generated.resources.button_save
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_status
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3ToolbarPane(
    modifier: Modifier = Modifier,
    listSize: Int,
    changedSize: Int,
    enabled: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp, // Add a slight elevation to distinguish from background
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status text
            Text(
                text = stringResource(Res.string.toolbar_status, listSize, changedSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            // Color legend
            EntryTagType.entries.forEach { type ->
                TextTag(
                    text = stringResource(type.label),
                    containerColor = type.containerColor(MaterialTheme.colorScheme),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp)
                )
            }

            if (onRefresh != null || onSave != null) {
                Spacer(modifier = Modifier.requiredWidth(16.dp))
            }

            // Action buttons
            onRefresh?.let { listener ->
                M3TooltipIconButton(
                    icon = Icons.Rounded.Refresh,
                    tooltip = stringResource(Res.string.button_refresh),
                    onClick = listener,
                    enabled = enabled,
                )
            }
            onSave?.let { listener ->
                M3TooltipIconButton(
                    icon = Icons.Rounded.Save,
                    tooltip = stringResource(Res.string.button_save),
                    onClick = listener,
                    enabled = enabled,
                )
            }
        }
    }
}

@Preview
@Composable
private fun M3ToolbarPanePreview() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorM3Theme(darkTheme = darkTheme) {
                Column {
                    Text("Enabled state", style = MaterialTheme.typography.titleSmall)
                    M3ToolbarPane(
                        listSize = 123,
                        changedSize = 45,
                        enabled = true
                    )
                }
                Column {
                    Text("Disabled state", style = MaterialTheme.typography.titleSmall)
                    M3ToolbarPane(
                        listSize = 123,
                        changedSize = 45,
                        enabled = false
                    )
                }
            }
        }
    }
}
