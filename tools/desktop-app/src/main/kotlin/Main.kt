// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed
// by the Apache 2.0 license that can be found in the LICENSE file.

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.desktop.apps.dsttranslate.DesktopPoHelper
import dolphin.desktop.apps.dsttranslate.Ini
import dolphin.desktop.apps.dsttranslate.PoDataModel
import dolphin.desktop.apps.dsttranslate.compose.ConfigPane
import dolphin.desktop.apps.dsttranslate.compose.DebugSaveDialog
import dolphin.desktop.apps.dsttranslate.compose.EditorPane
import dolphin.desktop.apps.dsttranslate.compose.EditorSpec
import dolphin.desktop.apps.dsttranslate.compose.EntryListPane
import dolphin.desktop.apps.dsttranslate.compose.OniTranslatorTheme
import dolphin.desktop.apps.dsttranslate.compose.SearchPane
import dolphin.desktop.apps.dsttranslate.compose.ToastUi
import dolphin.desktop.apps.dsttranslate.compose.ToastWrap
import dolphin.desktop.apps.dsttranslate.compose.ToolbarCallback
import dolphin.desktop.apps.dsttranslate.compose.ToolbarSpec
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.app_name
import dolphin.desktop.apps.onitranslator.generated.resources.debug_save_dialog_title
import dolphin.desktop.apps.onitranslator.generated.resources.nisbet_ponder
import dolphin.desktop.apps.onitranslator.generated.resources.tab_config
import dolphin.desktop.apps.onitranslator.generated.resources.tab_translation
import dolphin.desktop.apps.onitranslator.generated.resources.toast_cost_ms
import dolphin.desktop.apps.onitranslator.generated.resources.toast_write_failed
import dolphin.desktop.apps.onitranslator.generated.resources.toast_write_success
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI

enum class UiState {
    Main, Editor, Search,
}

@ExperimentalMaterialApi
fun main(args: Array<String>) = application {
//    println(args.contentToString())
    val version = args.find { it.startsWith("v=") }?.drop(2) ?: "x.x.x"

//    val osName: String = System.getProperties().getProperty("os.name")
//    println("os.name = $osName")

    val debugMode by remember { mutableStateOf(false) }

//    val tempDir: String = System.getProperty("java.io.tmpdir")
//    println("tempDir = $tempDir")
//
//    val homeDir: String = System.getProperty("user.home")
//    println("homeDir = $homeDir")

    val dataModel = remember {
        val workingDir: String = System.getProperties().getProperty("user.dir")
        println("workingDir = $workingDir")

        val debug = File(workingDir, "build").exists() // has build dir
        println("debug = $debug")

        val ini = Ini(workingDir)
        PoDataModel(DesktopPoHelper(ini, debug = debug).apply { prepare() })
    }
    val windowState = rememberWindowState(size = DpSize(0.dp, 0.dp), position = WindowPosition.PlatformDefault)
    val coroutineScope = rememberCoroutineScope()

    Window(
        visible = windowState.size.width.value > 0 && windowState.size.height.value > 0,
        onCloseRequest = {
            coroutineScope.launch {
                println("close window: save")
                dataModel.rememberLastWindowState(windowState)
                println("close window: exit")
                exitApplication()
            }
        },
        state = windowState,
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.nisbet_ponder),
    ) {
        App(
            dataModel,
            onCopyTo = ::copyToSystemClipboard,
            onCopyFrom = ::copyFromSystemClipboard,
            debug = debugMode,
            appVersion = version,
        )
    }

    LaunchedEffect(Unit) {
        val (position, size) = dataModel.loadIni() // LaunchedEffect
        windowState.position = position
        windowState.size = size
    }
}

@ExperimentalMaterialApi
@Composable
@Preview
fun App(
    dataModel: PoDataModel,
    onCopyTo: (String) -> Unit,
    onCopyFrom: () -> String,
    debug: Boolean = false,
    appVersion: String = "x.x.x",
) {
    val coroutineScope = rememberCoroutineScope()

    OniTranslatorTheme {
        var uiState by remember { mutableStateOf<Pair<UiState, UiState?>>(Pair(UiState.Main, null)) }
        val entryListState = rememberLazyListState()

        var editorData by remember { mutableStateOf(EditorSpec()) } // editor
        var toasted by remember { mutableStateOf<ToastWrap?>(null) }
        var cached by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf(1) } // default to Translation tab
        val loading = dataModel.helper.loading.collectAsState()

        // toast
        val toastJob = remember { mutableStateOf<Job?>(null) }
        fun toast(message: ToastWrap) {
            toastJob.value?.cancel()
            toastJob.value = coroutineScope.launch {
                toasted = message
                delay(2000)
                toasted = null
            }
        }

        fun changeUiState(state: UiState? = null) {
            uiState = Pair(
                state ?: uiState.second ?: UiState.Main, // change to new state or go back
                if (state == null) UiState.Main else uiState.first // if it is go back,
            )
        }

        fun showEntryEditor(entry: WordEntry) {
            editorData = dataModel.requestEdit(entry)
            changeUiState(UiState.Editor)
        }

        fun saveEntryList(cacheIt: Boolean = false) {
            coroutineScope.launch {
                cached = false // hide debug dialog
                val (exported, cost) = dataModel.save(cacheIt)
                if (cost > 0) {
                    toast(ToastWrap.WriteSuccess(exported, cost))
                } else {
                    toast(ToastWrap.WriteFailed)
                }
            }
        }

        val callback = remember {
            object : ToolbarCallback {
                override fun onRefresh() {
                    coroutineScope.launch {
                        val cost = dataModel.translate()
                        toast(ToastWrap.ShowCost(cost))
                    }
                }

                override fun onSave() {
                    if (debug) cached = true else saveEntryList()
                }

                override fun onSearch() {
                    changeUiState(UiState.Search)
                }
            }
        }

        LaunchedEffect(Unit) {
            dataModel.loadIniAndPo()
        }

        Box {
            when (uiState.first) {
                UiState.Main ->
                    MainPane(
                        dataModel,
                        modifier = Modifier.fillMaxSize(),
                        state = entryListState,
                        onEdit = { entry -> showEntryEditor(entry) },
                        callback = callback,
                        appVersion = if (debug) "${appVersion}D" else appVersion,
                        selectedTab = selectedTab,
                        onTabChange = { selectedTab = it },
                    )

                UiState.Editor ->
                    EditorPane(
                        data = editorData,
                        modifier = Modifier.fillMaxSize(),
                        onSave = { key, text ->
                            coroutineScope.launch {
                                dataModel.edit(key, text)
                                changeUiState() // hideEntryEditor
                            }
                        },
                        onCancel = { changeUiState() /* BACK */ },
                        onCopyToClipboard = { text ->
                            onCopyTo.invoke(text)
                            toast(ToastWrap.ShowString(text))
                        },
                        // onTranslate = { text -> translateByGoogle(text) },
                        onCopyFromClipboard = onCopyFrom,
                    )

                UiState.Search ->
                    SearchPane(
                        model = dataModel,
                        modifier = Modifier.fillMaxSize(),
                        onSelect = { key ->
                            dataModel.helper.translated(key)?.let { entry ->
                                showEntryEditor(entry)
                            }
                        },
                        onCancel = { changeUiState() /* BACK */ },
                    )
            }

            if (cached) {
                val file = dataModel.helper.getOutputFile(true)
                DebugSaveDialog(
                    onDismissRequest = { cached = false },
                    onSave = { saveEntryList(it) },
                    title = stringResource(Res.string.debug_save_dialog_title, file.toString()),
                    modifier = Modifier.fillMaxWidth(.5f),
                )
            }

            if (loading.value) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = .25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    // CircularProgressIndicator(color = MaterialTheme.colors.secondary)
                }
            }

            toasted?.let { ToastUi(it) }
        }
    }
}

@Composable
private fun MainPane(
    model: PoDataModel,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    callback: ToolbarCallback? = null,
    onEdit: ((WordEntry) -> Unit)? = null,
    appVersion: String = "x.x.x",
    selectedTab: Int = 0,
    onTabChange: ((tab: Int) -> Unit)? = null,
) {
    val composeScope = rememberCoroutineScope()
    val configs = model.configs.collectAsState()
    val status = model.helper.status.collectAsState()
    val loading by model.helper.loading.collectAsState()
    val spec by remember(loading) { mutableStateOf(ToolbarSpec(enabled = !loading)) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(MaterialTheme.colors.secondaryVariant),
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                backgroundColor = Color.Transparent, // MaterialTheme.colors.secondaryVariant,
                contentColor = MaterialTheme.colors.onSecondary,
                modifier = Modifier.weight(1f),
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabChange?.invoke(0) },
                ) {
                    Text(stringResource(Res.string.tab_config), modifier = Modifier.padding(8.dp))
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabChange?.invoke(1) },
                ) {
                    Text(stringResource(Res.string.tab_translation), modifier = Modifier.padding(8.dp))
                }
            }
            Text(
                appVersion,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colors.onSecondary,
            )
        }

        Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp, horizontal = 8.dp)) {
            when (selectedTab) {
                0 ->
                    ConfigPane(
                        configs = configs.value,
                        onConfigChange = { newConfigs ->
                            composeScope.launch { model.saveConfig(newConfigs) }
                        },
                    )

                1 ->
                    EntryListPane(
                        model = model,
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        onEdit = onEdit,
                        callback = callback,
                        spec = spec,
                    )
            }

            Text(
                status.value,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

/**
 * Copying text to the clipboard using Java
 * See https://stackoverflow.com/a/6713290
 */
fun copyToSystemClipboard(text: String) {
    val stringSelection = StringSelection(text)
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(stringSelection, null)
}

/**
 * Copy text from system clipboard
 * See https://stackoverflow.com/q/11596368
 */
fun copyFromSystemClipboard(): String {
    return try {
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.getData(DataFlavor.stringFlavor).toString()
    } catch (e: Exception) {
        println("copyFromSystemClipboard: ${e.message}")
        ""
    }
}

/**
 * Open Google Translate and translate the text to chinese.
 * See https://stackoverflow.com/a/10967469
 *
 * @param text target text
 */
@Suppress("unused")
fun translateByGoogle(text: String) {
    val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else return
    if (desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
            val encoded = text.replace(" ", "%20")
            val url = "https://translate.google.com.tw/?hl=zh-TW&sl=en&tl=zh-TW&text=$encoded"
            desktop.browse(URI(url))
        } catch (e: Exception) {
            println("translateByGoogle: ${e.message}")
        }
    } else {
        println("unable to search $text")
        copyToSystemClipboard(text) // workaround
    }
}
