package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.desktop.apps.dsttranslate.AppStrings
import dolphin.desktop.apps.dsttranslate.PoDataModel
import kotlinx.coroutines.launch

enum class SearchType {
    Key, Origin, Text,
}

// See https://stackoverflow.com/a/69148766
@Composable
fun SearchPane(
    model: PoDataModel,
    modifier: Modifier = Modifier,
    selectedValue: String? = null,
    onSelect: ((String) -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
) {
    val composeScope = rememberCoroutineScope()
    var currentEntry: WordEntry? = null
    val data = model.searchText.collectAsState()
    var last by remember { mutableStateOf("") }
    val filteredItems = model.searchList.collectAsState()
    val searchType = model.searchType.collectAsState()

    fun refreshItems(text: String) {
        composeScope.launch { model.search(text) }
        // println("found ${filteredItems.size}")
    }

    fun WordEntry.toSearchText() = when (searchType.value) {
        SearchType.Origin -> origin()
        SearchType.Key -> key()
        SearchType.Text -> string()
    }

    LaunchedEffect(Unit) {
        selectedValue?.let { model.search(it) }
    }

    Column(modifier = modifier.background(MaterialTheme.colors.surface)) {
        TextField(
            data.value,
            onValueChange = { text -> refreshItems(text) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(),
            placeholder = { Text(AppStrings.search_placeholder) },
            trailingIcon = {
                Row {
                    IconButton(onClick = { last = data.value }) {
                        Icon(
                            Icons.Rounded.Save,
                            contentDescription = AppStrings.content_description_cached,
                            tint = if (data.value.isNotEmpty()) Color.Gray else Color.LightGray,
                        )
                    }
                    LazyToolTip(tooltip = last, backgroundColor = Color.Transparent) {
                        IconButton(onClick = { refreshItems(last) }) {
                            Icon(
                                Icons.Rounded.Replay,
                                contentDescription = AppStrings.content_description_undo,
                                tint = if (last.isNotEmpty()) Color.Gray else Color.LightGray
                            )
                        }
                    }
                    IconButton(onClick = { refreshItems("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = AppStrings.content_description_clear)
                    }
                }
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            SearchType.entries.forEach { type ->
                Row(
                    modifier = Modifier.clickable {
                        composeScope.launch { model.searchType(type) }
                    }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (type == searchType.value) {
                            Icons.Rounded.RadioButtonChecked
                        } else {
                            Icons.Rounded.RadioButtonUnchecked
                        },
                        contentDescription = null
                    )
                    Text(
                        when (type) {
                            SearchType.Key -> AppStrings.search_type_key
                            SearchType.Origin -> AppStrings.search_type_origin
                            SearchType.Text -> AppStrings.search_type_text
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        LazyScrollableColumn(filteredItems.value, modifier = Modifier.weight(1f)) { _, entry ->
            TextButton(onClick = {
                currentEntry = entry
                refreshItems(entry.toSearchText())
            }) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        entry.key(),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.secondary,
                    )
                    if (searchType.value == SearchType.Origin) {
                        Text(
                            entry.origin(),
                            style = MaterialTheme.typography.caption,
                            color = Color.Gray,
                        )
                    }
                    Text(entry.string(), style = MaterialTheme.typography.body1)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                val entry =
                    currentEntry ?: model.helper.allValues().find { entry -> entry.toSearchText() == data.value }
                onSelect?.invoke(entry?.key() ?: "")
            }, modifier = Modifier.weight(1f)) {
                Text(AppStrings.button_edit)
            }
            TextButton(onClick = { onCancel?.invoke() }, modifier = Modifier.weight(1f)) {
                Text(AppStrings.button_close)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSearchPaneAll() {
    DstTranslatorTheme {
        SearchPane(PreviewDefaults.model)
    }
}

@Preview
@Composable
private fun PreviewSearchPaneShowSelected() {
    DstTranslatorTheme {
        SearchPane(PreviewDefaults.model, selectedValue = "ch")
    }
}
