package dolphin.desktop.apps.onitranslator.model

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.StringResource

sealed interface SnackbarMessage {
    val duration: SnackbarDuration get() = SnackbarDuration.Short
    val actionLabel: String? get() = null
    val withDismissAction: Boolean get() = false

    data class Text(val text: String) : SnackbarMessage
    class Resource(
        val resource: StringResource,
        vararg val formatArgs: Any
    ) : SnackbarMessage
}


/**
 * A singleton manager for showing Snackbar messages.
 */
object SnackbarManager {
    private val _messages = MutableStateFlow<SnackbarMessage?>(null)
    val messages: StateFlow<SnackbarMessage?> = _messages.asStateFlow()

    fun showMessage(message: SnackbarMessage) {
        _messages.value = message
    }

    fun showMessage(message: String) {
        _messages.value = SnackbarMessage.Text(message)
    }

    fun showMessage(resource: StringResource, vararg formatArgs: Any) {
        _messages.value = SnackbarMessage.Resource(resource, *formatArgs)
    }

    fun dismissMessage() {
        _messages.value = null
    }
}
