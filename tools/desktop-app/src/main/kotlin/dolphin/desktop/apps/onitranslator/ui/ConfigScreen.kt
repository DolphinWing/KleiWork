package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_apply
import dolphin.desktop.apps.onitranslator.generated.resources.button_cancel
import dolphin.desktop.apps.onitranslator.generated.resources.draft_path
import dolphin.desktop.apps.onitranslator.generated.resources.github_root
import dolphin.desktop.apps.onitranslator.generated.resources.glossary_dir
import dolphin.desktop.apps.onitranslator.generated.resources.label_auto_save
import dolphin.desktop.apps.onitranslator.generated.resources.label_auto_save_interval
import dolphin.desktop.apps.onitranslator.generated.resources.manual_setup
import dolphin.desktop.apps.onitranslator.generated.resources.oni_asset_dir
import dolphin.desktop.apps.onitranslator.generated.resources.oni_workshop_dir
import dolphin.desktop.apps.onitranslator.generated.resources.quick_setup
import dolphin.desktop.apps.onitranslator.generated.resources.string_map_path_cannot_be_empty
import dolphin.desktop.apps.onitranslator.model.Configs
import dolphin.desktop.apps.onitranslator.model.PoHelper
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.FilePicker
import org.jetbrains.compose.resources.stringResource
import java.io.File
import javax.swing.JFileChooser

/**
 * ConfigScreen provides a Material Design 3 interface for configuring application paths.
 * It allows users to set paths for the strings.xml file, ONI workshop directory, and ONI assets directory.
 * It also supports a "GitHub Root" feature to auto-populate paths based on a selected root directory.
 *
 * @param configs The current configuration settings.
 * @param onConfigChange Callback to be invoked when configuration settings change.
 * @param onApply Callback to be invoked when the Save button is clicked with valid configs.
 * @param onCancel Callback to be invoked when the Cancel button is clicked or dialog is dismissed.
 * @param modifier Modifier for this Composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    configs: Configs,
    onConfigChange: ((configs: Configs) -> Unit)? = null,
    onApply: (Configs) -> Unit = {},
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        // Quick Setup Section
        Text(stringResource(Res.string.quick_setup), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FilePicker(
            label = stringResource(Res.string.github_root),
            path = "", // No specific "github_root" stored, only used for selection
            onPathChange = { file ->
                val s = File.separator
                onConfigChange?.invoke(

                    configs.copy(
                        dataBankPath = "${file}${s}tools${s}desktop-app${s}src${s}main${s}composeResources${s}files${s}replacement_strings.xml",
                        oniWorkshopDir = "${file}${s}workshop-2906930548", // workshop folder name
                        oniAssetsDir = "${file}${s}oni-assets",
                        glossaryDir = "${file}${s}oni-assets",
                    )
                )
            },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Manual Setup Section
        Text(stringResource(Res.string.manual_setup), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        FilePicker(
            label = stringResource(Res.string.oni_workshop_dir),
            path = configs.oniWorkshopDir,
            onPathChange = { onConfigChange?.invoke(configs.copy(oniWorkshopDir = it)) },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(Modifier.height(8.dp))

        FilePicker(
            label = stringResource(Res.string.oni_asset_dir),
            path = configs.oniAssetsDir,
            onPathChange = { onConfigChange?.invoke(configs.copy(oniAssetsDir = it)) },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(Modifier.height(8.dp))

        FilePicker(
            label = stringResource(Res.string.glossary_dir),
            path = configs.glossaryDir,
            onPathChange = { onConfigChange?.invoke(configs.copy(glossaryDir = it)) },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(Modifier.height(16.dp))

        // Draft path (read-only)
        val draftPath = File(System.getProperty("java.io.tmpdir"), PoHelper.ONI_PO).absolutePath
        OutlinedTextField(
            value = draftPath,
            onValueChange = {},
            label = { Text(stringResource(Res.string.draft_path)) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            )
        )
        Spacer(Modifier.height(16.dp))

        // Auto-save Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(Res.string.label_auto_save),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    stringResource(Res.string.label_auto_save_interval, configs.autoSaveIntervalMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = configs.autoSaveEnabled,
                onCheckedChange = { onConfigChange?.invoke(configs.copy(autoSaveEnabled = it)) }
            )
        }

        if (configs.autoSaveEnabled) {
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Slider(
                    value = configs.autoSaveIntervalMinutes.toFloat(),
                    onValueChange = { newValue ->
                        onConfigChange?.invoke(configs.copy(autoSaveIntervalMinutes = newValue.toInt()))
                    },
                    valueRange = 1f..30f,
                    steps = 28 // 30 - 1 - 1 = 28 steps to make it 1-minute increments
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1", style = MaterialTheme.typography.labelSmall)
                    Text("30", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // replacement_strings.xml file path with error handling
        val isStringMapError = configs.dataBankPath.isEmpty()
        FilePicker(
            label = "replacement_strings.xml",
            path = configs.dataBankPath,
            onPathChange = { onConfigChange?.invoke(configs.copy(dataBankPath = it)) },
            selectionMode = JFileChooser.FILES_ONLY,
            isError = isStringMapError,
            supportingText = if (isStringMapError) {
                { Text(stringResource(Res.string.string_map_path_cannot_be_empty)) }
            } else null
        )

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.button_cancel))
            }
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = { onApply(configs) },
                enabled = !isStringMapError
            ) {
                Icon(Icons.Rounded.Done, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.button_apply))
            }
        }
    }
}

@Composable
internal fun ConfigDialogContent(
    configs: Configs,
    onConfigChange: (configs: Configs) -> Unit = {},
    onApply: (Configs) -> Unit = {},
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
    ) {
        ConfigScreen(
            configs = configs,
            onConfigChange = onConfigChange,
            onApply = onApply,
            onCancel = onDismissRequest,
        )
    }
}

@Preview
@Composable
private fun ConfigScreenPreviewLight() {
    OniTranslatorTheme(darkTheme = false) {
        Surface {
            ConfigScreen(
                configs = Configs(
                    dataBankPath = "/path/to/strings.xml",
                    oniWorkshopDir = "/path/to/workshop",
                    oniAssetsDir = "/path/to/assets",
                    autoSaveEnabled = true,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ConfigScreenPreviewDark() {
    OniTranslatorTheme(darkTheme = true) {
        Surface {
            ConfigScreen(
                configs = Configs(
                    dataBankPath = "",
                    oniWorkshopDir = "/path/to/workshop",
                    oniAssetsDir = "/path/to/assets"
                ),
            )
        }
    }
}
