@file:OptIn(ExperimentalFoundationApi::class)

package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TooltipIconButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipArea(tooltip = {
        Surface(
            modifier = Modifier.shadow(4.dp),
            color = Color(255, 255, 210),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = tooltip,
                modifier = Modifier.padding(10.dp)
            )
        }
    }) {
        IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            content()
        }
    }
}

@Composable
fun TooltipTextButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    Box(modifier = modifier) {
        TooltipArea(tooltip = {
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = Color(255, 255, 210),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = tooltip,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }) {
            TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = enabled, colors = colors) {
                content()
            }
        }
    }
}

@Composable
fun TooltipButton(
    onClick: () -> Unit,
    tooltip: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    Box(modifier = modifier) {
        TooltipArea(tooltip = {
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = Color(255, 255, 210),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = tooltip,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }) {
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = enabled, colors = colors) {
                content()
            }
        }
    }
}
