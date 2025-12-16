package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_cancel
import dolphin.desktop.apps.onitranslator.generated.resources.button_no
import dolphin.desktop.apps.onitranslator.generated.resources.button_yes
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DebugSaveDialog(
    onDismissRequest: () -> Unit,
    onSave: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Text(title, style = MaterialTheme.typography.h6)
        },
        buttons = {
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                TextButton(onClick = onDismissRequest) { Text(stringResource(Res.string.button_cancel)) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { onSave(false) }) { Text(stringResource(Res.string.button_no)) }
                TextButton(
                    onClick = { onSave(true) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colors.secondary,
                    ),
                ) { Text(stringResource(Res.string.button_yes)) }
            }
        },
        modifier = modifier,
    )
}
