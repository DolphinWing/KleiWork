package dolphin.desktop.apps.onitranslator.model

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.menu_draft
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_old_template
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_old_translated
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_simplified_text
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_template_text
import org.jetbrains.compose.resources.StringResource

enum class EntryTagType(val label: StringResource) {
    Source(Res.string.toolbar_template_text),
    OldSource(Res.string.toolbar_old_template),
    ChsRef(Res.string.toolbar_simplified_text),
    PoSave(Res.string.toolbar_old_translated),
    Draft(Res.string.menu_draft);

    fun containerColor(colorScheme: ColorScheme): Color = when (this) {
        Source -> colorScheme.primaryContainer
        OldSource -> colorScheme.primaryContainer.copy(alpha = .5f)
        ChsRef -> colorScheme.secondaryContainer
        PoSave -> colorScheme.tertiaryContainer
        Draft -> colorScheme.tertiaryContainer.copy(alpha = .4f)
    }

    fun contentColor(colorScheme: ColorScheme): Color = when (this) {
        Source -> colorScheme.onPrimaryContainer
        OldSource -> colorScheme.onPrimaryContainer.copy(alpha = .7f)
        ChsRef -> colorScheme.onSecondaryContainer
        PoSave -> colorScheme.onTertiaryContainer
        Draft -> colorScheme.onTertiaryContainer.copy(alpha = .6f)
    }
}
