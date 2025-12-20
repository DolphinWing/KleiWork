package dolphin.desktop.apps.onitranslator

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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.dsttranslate.Ini
import dolphin.desktop.apps.onitranslator.compose.OniTranslatorM3Theme
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.github_root
import dolphin.desktop.apps.onitranslator.generated.resources.manual_setup
import dolphin.desktop.apps.onitranslator.generated.resources.oni_asset_dir
import dolphin.desktop.apps.onitranslator.generated.resources.oni_workshop_dir
import dolphin.desktop.apps.onitranslator.generated.resources.quick_setup
import dolphin.desktop.apps.onitranslator.generated.resources.string_map_path_cannot_be_empty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

/**
 * Data class to hold configuration paths.
 * @param stringMap Path to strings.xml.
 * @param oniWorkshopDir Path to the ONI workshop directory.
 * @param oniAssetsDir Path to the ONI assets directory.
 */
data class Configs(
    val stringMap: String = "",
    val oniWorkshopDir: String = "",
    val oniAssetsDir: String = "",
) {
    /**
     * Secondary constructor to initialize Configs from an Ini object.
     * @param ini The Ini object containing configuration data.
     */
    constructor(ini: Ini) : this(
        ini.stringMap,
        ini.oniWorkshopDir,
        ini.oniAssetsDir,
    )

    fun isValid(): Boolean {
        return stringMap.isNotBlank() && File(stringMap).isFile &&
                oniWorkshopDir.isNotBlank() && File(oniWorkshopDir).isDirectory &&
                oniAssetsDir.isNotBlank() && File(oniAssetsDir).isDirectory
    }
}

/**
 * M3ConfigPane provides a Material Design 3 interface for configuring application paths.
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
fun M3ConfigPane(
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
        M3FileChooser(
            label = stringResource(Res.string.github_root),
            path = "", // No specific "github_root" stored, only used for selection
            onPathChange = { file ->
                val s = File.separator
                onConfigChange?.invoke(
                    configs.copy(
                        stringMap = "${file}${s}desktop-app${s}resources${s}common${s}strings.xml",
                        oniWorkshopDir = "${file}${s}workshop-2906930548", // Example workshop folder name
                        oniAssetsDir = "${file}${s}oni-assets",
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

        M3FileChooser(
            label = stringResource(Res.string.oni_workshop_dir),
            path = configs.oniWorkshopDir,
            onPathChange = { onConfigChange?.invoke(configs.copy(oniWorkshopDir = it)) },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(Modifier.height(8.dp))

        M3FileChooser(
            label = stringResource(Res.string.oni_asset_dir),
            path = configs.oniAssetsDir,
            onPathChange = { onConfigChange?.invoke(configs.copy(oniAssetsDir = it)) },
            selectionMode = JFileChooser.DIRECTORIES_ONLY,
        )
        Spacer(Modifier.height(8.dp))

        // strings.xml file path with error handling
        val isStringMapError = configs.stringMap.isEmpty()
        M3FileChooser(
            label = "strings.xml",
            path = configs.stringMap,
            onPathChange = { onConfigChange?.invoke(configs.copy(stringMap = it)) },
            selectionMode = JFileChooser.FILES_ONLY,
            isError = isStringMapError,
            supportingText = if (isStringMapError) {
                { Text(stringResource(Res.string.string_map_path_cannot_be_empty)) }
            } else null
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Icon(Icons.Rounded.Close, contentDescription = "Cancel")
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onApply(configs) },
                enabled = configs.isValid()
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "Save")
                Text("Save")
            }
        }
    }
}

@Composable
fun M3ConfigDialogContent(
    configs: Configs,
    onConfigChange: ((configs: Configs) -> Unit)? = null,
    onConfigSaved: (Configs) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium, // Apply rounded corners
    ) {
        M3ConfigPane(
            configs = configs,
            onConfigChange = onConfigChange,
            onApply = onConfigSaved,
            onCancel = onDismissRequest,
        )
    }
}

/**
 * A Material Design 3 file chooser Composable.
 *
 * @param label The label for the text field.
 * @param path The current selected path.
 * @param onPathChange Callback when the path is changed.
 * @param selectionMode The selection mode for the JFileChooser (FILES_ONLY, DIRECTORIES_ONLY, FILES_AND_DIRECTORIES).
 * @param modifier Modifier for this Composable.
 * @param isError Whether the text field is in an error state.
 * @param supportingText Optional supporting text below the text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3FileChooser(
    label: String,
    path: String,
    onPathChange: (String) -> Unit,
    selectionMode: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = path,
            onValueChange = {}, // Read-only, changes handled by file chooser
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.weight(1f),
            singleLine = true,
            isError = isError,
            supportingText = supportingText,
            trailingIcon = {
                IconButton(onClick = {
                    coroutineScope.launch {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) // Use system L&F
                        val fileChooser = JFileChooser(if (path.isNotEmpty()) File(path) else null)
                        fileChooser.fileSelectionMode = selectionMode
                        val result = withContext(Dispatchers.IO) {
                            fileChooser.showOpenDialog(null) // 'null' for parent component
                        }
                        if (result == JFileChooser.APPROVE_OPTION) {
                            fileChooser.selectedFile?.let {
                                onPathChange(it.absolutePath)
                            }
                        }
                    }
                }) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Open File Chooser")
                }
            }
        )
    }
}


// Previews for M3ConfigPane components
@Preview
@Composable
private fun M3ConfigPanePreviewLight() {
    OniTranslatorM3Theme(darkTheme = false) {
        Surface {
            M3ConfigPane(
                configs = Configs(
                    stringMap = "/path/to/strings.xml",
                    oniWorkshopDir = "/path/to/workshop",
                    oniAssetsDir = "/path/to/assets"
                ),
            )
        }
    }
}

@Preview
@Composable
private fun M3ConfigPanePreviewDark() {
    OniTranslatorM3Theme(darkTheme = true) {
        Surface {
            M3ConfigPane(
                configs = Configs(
                    stringMap = "", // Test error case
                    oniWorkshopDir = "/path/to/workshop",
                    oniAssetsDir = "/path/to/assets"
                ),
            )
        }
    }
}
