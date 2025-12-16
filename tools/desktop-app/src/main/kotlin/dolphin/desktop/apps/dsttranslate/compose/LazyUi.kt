package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.toast_cost_ms
import dolphin.desktop.apps.onitranslator.generated.resources.toast_write_failed
import dolphin.desktop.apps.onitranslator.generated.resources.toast_write_success
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyToolTip(
    tooltip: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val color = backgroundColor ?: MaterialTheme.colors.secondaryVariant.copy(alpha = .5f)
    if (tooltip.isNotEmpty()) {
        TooltipArea(
            tooltip = {
                // composable tooltip content
                Surface(
                    modifier = Modifier.shadow(2.dp),
                    color = MaterialTheme.colors.secondary,
                    shape = RoundedCornerShape(4.dp),
                    // border = BorderStroke(1.dp, MaterialTheme.colors.secondary)
                ) {
                    Text(
                        text = tooltip,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSecondary,
                    )
                }
            },
            modifier = modifier.background(color),
            delayMillis = 600,
            content = content,
        )
    } else {
        content()
    }
}

@Composable
fun <T> LazyScrollableColumn(
    list: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    itemContent: @Composable LazyItemScope.(Int, T) -> Unit,
) {
    Box(modifier = modifier) {
        LazyColumn(Modifier.fillMaxSize().padding(end = 12.dp), state) {
            itemsIndexed(list, itemContent = itemContent)
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                .padding(horizontal = 2.dp),
            adapter = rememberScrollbarAdapter(scrollState = state),
        )
    }
}

@Composable
fun LazyScrollingPane(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier) {
        val stateVertical = rememberScrollState(0)
        val stateHorizontal = rememberScrollState(0)

        Box(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(stateVertical)
                .padding(end = 12.dp, bottom = 12.dp)
                .horizontalScroll(stateHorizontal),
            content = content,
            contentAlignment = Alignment.TopStart,
        )

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                .padding(horizontal = 2.dp),
            adapter = rememberScrollbarAdapter(stateVertical)
        )

        HorizontalScrollbar(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(end = 12.dp, bottom = 2.dp),
            adapter = rememberScrollbarAdapter(stateHorizontal)
        )
    }
}

/**
 * https://stackoverflow.com/a/68143597
 */
@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float = 1f,
    borderColor: Color = MaterialTheme.colors.primaryVariant,
) {
    Text(
        text = text,
        Modifier.border(1.dp, borderColor).weight(weight).padding(8.dp),
        style = MaterialTheme.typography.body1
    )
}

sealed interface ToastWrap {
    data class WriteSuccess(val exported: String, val cost: Long) : ToastWrap
    data object WriteFailed : ToastWrap
    data class ShowCost(val cost: Long) : ToastWrap
    data class ShowString(val message: String) : ToastWrap
}

@Composable
fun BoxScope.ToastUi(toast: ToastWrap) {
    val text = when (toast) {
        is ToastWrap.ShowString -> toast.message
        is ToastWrap.ShowCost -> stringResource(Res.string.toast_cost_ms, toast.cost)
        is ToastWrap.WriteFailed -> stringResource(Res.string.toast_write_failed)
        is ToastWrap.WriteSuccess -> stringResource(Res.string.toast_write_success, toast.exported, toast.cost)
    }
    if (text.isNotEmpty()) {
        AnimatedVisibility(
            visible = true,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
            // exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)),
        ) {
            Text(
                text,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colors.primary.copy(alpha = .25f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .background(Color.White.copy(.8f), shape = RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colors.secondary.copy(alpha = .2f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                style = MaterialTheme.typography.subtitle1,
            )
        }
    }
}
