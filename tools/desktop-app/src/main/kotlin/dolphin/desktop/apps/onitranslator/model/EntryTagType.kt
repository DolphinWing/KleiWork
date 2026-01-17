package dolphin.desktop.apps.onitranslator.model

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_old_translated
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_simplified_text
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_template_text
import org.jetbrains.compose.resources.StringResource

enum class EntryTagType(val label: StringResource) {
    Original(Res.string.toolbar_template_text),
    Simplified(Res.string.toolbar_simplified_text),
    Translated(Res.string.toolbar_old_translated);

    fun containerColor(colorScheme: ColorScheme): Color = when (this) {
        Original -> colorScheme.primaryContainer
        Simplified -> colorScheme.secondaryContainer
        Translated -> colorScheme.tertiaryContainer
    }

    fun contentColor(colorScheme: ColorScheme): Color = when (this) {
        Original -> colorScheme.onPrimaryContainer
        Simplified -> colorScheme.onSecondaryContainer
        Translated -> colorScheme.onTertiaryContainer
    }
}
