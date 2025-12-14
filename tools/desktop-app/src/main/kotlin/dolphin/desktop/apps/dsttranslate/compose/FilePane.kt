package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import dolphin.desktop.apps.dsttranslate.AppStrings
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter

@Composable
fun FileChooserPane(
    file: String?,
    onFileChange: (File) -> Unit,
    modifier: Modifier = Modifier,
    filter: FileFilter? = null,
    selectionMode: Int = JFileChooser.FILES_ONLY,
) {
    val chooser = JFileChooser().apply {
        fileSelectionMode = selectionMode
        fileFilter = filter
        selectedFile = if (file != null) File(file) else null
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = file ?: "",
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            // textStyle = MaterialTheme.typography.body2,
        )
        IconButton(onClick = {
            val ret = chooser.showOpenDialog(ComposeWindow())
            if (ret == JFileChooser.APPROVE_OPTION) {
                onFileChange.invoke(chooser.selectedFile)
            }
        }) {
            Icon(
                when (selectionMode) {
                    JFileChooser.FILES_ONLY -> Icons.Rounded.FileCopy
                    else -> Icons.Rounded.Folder
                },
                contentDescription = AppStrings.content_description_folder,
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewFileChooserPane() {
    OniTranslatorTheme {
        Column {
            FileChooserPane(
                file = "/home/jimmyhu",
                onFileChange = {},
                selectionMode = JFileChooser.DIRECTORIES_ONLY,
            )
            FileChooserPane(
                file = "/home/jimmyhu/work/clipboard.txt",
                onFileChange = {},
                selectionMode = JFileChooser.FILES_ONLY
            )
        }
    }
}

@Preview
@Composable
private fun PreviewThis() {
    OniTranslatorTheme {
        Text("Sample Text")
    }
}
