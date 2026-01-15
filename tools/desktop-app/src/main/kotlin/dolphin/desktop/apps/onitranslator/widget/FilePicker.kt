package dolphin.desktop.apps.onitranslator.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

/**
 * A Material Design 3 file picker Composable.
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
fun FilePicker(
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
