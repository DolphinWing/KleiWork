package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.desktop.apps.dsttranslate.AppStrings

sealed class SuspectData(
    val title: String? = null,
    val entry: WordEntry = WordEntry.default(),
) {
    class Category(char: Char) : SuspectData(char.toString())
    class Entry(entry: WordEntry) : SuspectData(entry = entry)
}

@Composable
fun SuspectPane(
    suspectList: List<SuspectData>,
    state: LazyListState = rememberLazyListState(),
    onEdit: (WordEntry) -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
//    var suspectList by remember { mutableStateOf(emptyList<SuspectData>()) }
//
//    LaunchedEffect(Unit) {
//        model.suspectMap.collect { suspects ->
//            val list = ArrayList<SuspectData>()
//            suspects.forEach { (category, map) ->
//                list.add(SuspectData.Category(category))
//                map.forEach { entry -> list.add(SuspectData.Entry(entry)) }
//            }
//            suspectList = list
//        }
//    }

    Surface(modifier = modifier, contentColor = MaterialTheme.colors.surface) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyScrollableColumn(suspectList, modifier = Modifier.weight(1f), state = state) { _, suspect ->
                (suspect as? SuspectData.Category)?.let { title ->
                    Text(
                        title.title ?: "",
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.onSurface,
                    )
                }
                (suspect as? SuspectData.Entry)?.let { entry ->
                    TextButton(
                        onClick = { onEdit.invoke(entry.entry) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(entry.entry.key(), style = MaterialTheme.typography.caption)
                            Text(entry.entry.string())
                        }
                    }
                }
            }
            TextButton(onClick = onHide, modifier = Modifier.fillMaxWidth()) {
                Text(AppStrings.button_close)
            }
        }
    }
}
