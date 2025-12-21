package dolphin.desktop.apps.onitranslator.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorM3Theme
import dolphin.desktop.apps.onitranslator.widget.TextTag
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OniTranslatorBottomBar(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    listSize: Int = 0,
    versionText: String = "",
) {
    Surface(
        modifier = modifier,
        color = BottomAppBarDefaults.containerColor,
        tonalElevation = BottomAppBarDefaults.ContainerElevation,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLoading) "Loading..." else "$listSize items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            VerticalDivider(modifier = Modifier.size(1.dp, 16.dp))
            Spacer(Modifier.width(4.dp))
            EntryTagType.entries.forEach { type ->
                TextTag(
                    text = stringResource(type.label),
                    containerColor = type.containerColor(MaterialTheme.colorScheme),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
            }
            VerticalDivider(modifier = Modifier.size(1.dp, 16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                versionText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview
@Composable
private fun OniTranslatorBottomBarPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { dark ->
            OniTranslatorM3Theme(darkTheme = dark) {
                OniTranslatorBottomBar(isLoading = false, listSize = 10, versionText = "2.0.0")
            }
            OniTranslatorM3Theme(darkTheme = dark) {
                OniTranslatorBottomBar(isLoading = true, versionText = "2.0.0")
            }
        }
    }
}
