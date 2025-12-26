package dolphin.desktop.apps.onitranslator.model

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition

/**
 * Data class to hold window geometry settings.
 *
 * @param x The x-coordinate of the window.
 * @param y The y-coordinate of the window.
 * @param width The width of the window.
 * @param height The height of the window.
 */
data class WindowConfig(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 1200f,
    val height: Float = 800f,
    val darkTheme: Boolean? = null,
) {
    /**
     * Converts the stored coordinates to a Compose [WindowPosition].
     * Returns [WindowPosition.PlatformDefault] if coordinates are invalid.
     */
    fun toWindowPosition(): WindowPosition =
        if (x > 0 && y > 0) WindowPosition(x.dp, y.dp) else WindowPosition.PlatformDefault

    /**
     * Converts the stored dimensions to a Compose [DpSize].
     * Returns [DpSize.Unspecified] if dimensions are invalid.
     */
    fun toDpSize(): DpSize = if (width > 0 && height > 0) DpSize(width.dp, height.dp) else DpSize.Unspecified
}
