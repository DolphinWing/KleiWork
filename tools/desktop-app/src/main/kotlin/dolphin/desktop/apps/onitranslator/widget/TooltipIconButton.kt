package dolphin.desktop.apps.onitranslator.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    painter: Painter,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(position),
        tooltip = {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    tooltip,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            Icon(painter = painter, contentDescription = tooltip)
            // Icon tint is handled automatically by the IconButton based on the enabled state
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    resource: DrawableResource,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    onClick: () -> Unit,
) = TooltipIconButton(
    painter = painterResource(resource),
    tooltip = tooltip,
    modifier = modifier,
    enabled = enabled,
    position = position,
    onClick = onClick,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    icon: ImageVector,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(position),
        tooltip = {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    tooltip,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            Icon(imageVector = icon, contentDescription = tooltip)
            // Icon tint is handled automatically by the IconButton based on the enabled state
        }
    }
}
