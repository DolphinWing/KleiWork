package dolphin.desktop.apps.onitranslator.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import org.jetbrains.compose.resources.stringResource

@Composable
fun TextTag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(containerColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun TextTag(tagType: EntryTagType, modifier: Modifier = Modifier, text: String = stringResource(tagType.label)) {
    TextTag(
        text = text,
        modifier = modifier,
        containerColor = tagType.containerColor(MaterialTheme.colorScheme),
        contentColor = tagType.contentColor(MaterialTheme.colorScheme),
    )
}
