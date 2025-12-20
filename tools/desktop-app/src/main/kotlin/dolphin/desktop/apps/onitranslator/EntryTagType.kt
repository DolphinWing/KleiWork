package dolphin.desktop.apps.onitranslator

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_old_translated
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_simplified_text
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_template_text
import org.jetbrains.compose.resources.StringResource

enum class EntryTagType(val label: StringResource) {
    Templated(Res.string.toolbar_template_text),
    Simplified(Res.string.toolbar_simplified_text),
    Translated(Res.string.toolbar_old_translated);

    fun containerColor(colorScheme: ColorScheme): Color = when (this) {
        Templated -> colorScheme.primaryContainer
        Simplified -> colorScheme.tertiaryContainer
        Translated -> colorScheme.secondaryContainer
    }

    fun contentColor(colorScheme: ColorScheme): Color = when (this) {
        Templated -> colorScheme.onPrimaryContainer
        Simplified -> colorScheme.onTertiaryContainer
        Translated -> colorScheme.onSecondaryContainer
    }
}
