package dolphin.desktop.apps.onitranslator.widget

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dolphin.desktop.apps.onitranslator.model.SnackbarManager
import dolphin.desktop.apps.onitranslator.model.SnackbarMessage
import org.jetbrains.compose.resources.getString

@Composable
fun OniSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val snackbarMessage by SnackbarManager.messages.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            val resultMessage = when (message) {
                is SnackbarMessage.Text -> message.text
                is SnackbarMessage.Resource -> getString(message.resource, *message.formatArgs)
            }
            val visuals = object : SnackbarVisuals {
                override val message: String = resultMessage
                override val actionLabel: String? = message.actionLabel
                override val withDismissAction: Boolean = message.withDismissAction
                override val duration = message.duration
            }
            hostState.showSnackbar(visuals)
            SnackbarManager.dismissMessage() // Auto-dismiss after showing
        }
    }

    SnackbarHost(hostState, modifier = modifier)
}
