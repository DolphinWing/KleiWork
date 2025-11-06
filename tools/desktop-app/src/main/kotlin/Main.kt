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
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dolphin.android.apps.dsttranslate.PoHelper
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.desktop.apps.dsttranslate.DesktopPoHelper
import dolphin.desktop.apps.dsttranslate.Ini
import dolphin.desktop.apps.dsttranslate.PoDataModel
import dolphin.desktop.apps.dsttranslate.compose.ConfigPane
import dolphin.desktop.apps.dsttranslate.compose.DebugSaveDialog
import dolphin.desktop.apps.dsttranslate.compose.DstTranslatorTheme
import dolphin.desktop.apps.dsttranslate.compose.EditorPane
import dolphin.desktop.apps.dsttranslate.compose.EditorSpec
import dolphin.desktop.apps.dsttranslate.compose.EntryListPane
import dolphin.desktop.apps.dsttranslate.compose.SearchPane
import dolphin.desktop.apps.dsttranslate.compose.SuspectData
import dolphin.desktop.apps.dsttranslate.compose.SuspectPane
import dolphin.desktop.apps.dsttranslate.compose.ToastUi
import dolphin.desktop.apps.dsttranslate.compose.ToolbarCallback
import dolphin.desktop.apps.dsttranslate.compose.ToolbarSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URL

enum class UiState {
    Main, Editor, Search, Analysis,
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

    val model = remember {
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
                model.rememberLastWindowState(windowState)
                println("close window: exit")
                exitApplication()
            }
        },
        state = windowState,
        title = "ONI/DST PO Helper",
        icon = BitmapPainter(useResource("nisbet_ponder.png", ::loadImageBitmap)),
    ) {
        App(
            model,
            onCopyTo = ::copyToSystemClipboard,
            onCopyFrom = ::copyFromSystemClipboard,
            debug = debugMode,
            appVersion = version,
        )
    }

    LaunchedEffect(Unit) {
        // delay(500.milliseconds)
        val (position, size) = model.loadIniAndPo() // LaunchedEffect
        windowState.position = position
        windowState.size = size
    }
}

@ExperimentalMaterialApi
@Composable
@Preview
fun App(
    model: PoDataModel,
    onCopyTo: (String) -> Unit,
    onCopyFrom: () -> String,
    debug: Boolean = false,
    appVersion: String = "x.x.x",
) {
    val coroutineScope = rememberCoroutineScope()

    DstTranslatorTheme {
        var uiState by remember { mutableStateOf<Pair<UiState, UiState?>>(Pair(UiState.Main, null)) }
        val entryListState = rememberLazyListState()
        val suspectState = rememberLazyListState()
        var suspectList by remember { mutableStateOf(emptyList<SuspectData>()) }

        var editorData by remember { mutableStateOf(EditorSpec()) } // editor

        var toasted by remember { mutableStateOf("") }
        var cached by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf(1) } // default to Translation tab
        val loading = model.helper.loading.collectAsState()

        // toast
        val toastJob = remember { mutableStateOf<Job?>(null) }
        fun toast(message: String) {
            toastJob.value?.cancel()
            toastJob.value = coroutineScope.launch {
                toasted = message
                delay(2000)
                toasted = ""
            }
        }

        fun changeUiState(state: UiState? = null) {
            uiState = Pair(
                state ?: uiState.second ?: UiState.Main, // change to new state or go back
                if (state == null) UiState.Main else uiState.first // if it is go back,
            )
        }

        fun showEntryEditor(entry: WordEntry) {
            // println("edit: ${entry.key}")
            editorData = model.requestEdit(entry)
            changeUiState(UiState.Editor)
        }

        fun saveEntryList(cacheIt: Boolean = false) {
            coroutineScope.launch {
                cached = false // hide debug dialog
                val (exported, cost) = model.save(cacheIt)
                if (cost > 0) {
                    toast("write $exported cost $cost ms")
                } else {
                    toast("write failed!")
                }
            }
        }

        val callback = remember {
            object : ToolbarCallback {
                override fun onRefresh() {
                    coroutineScope.launch {
                        val cost = model.translate()
                        toast("cost $cost ms")
                    }
                }

                override fun onSave() {
                    if (debug) cached = true else saveEntryList()
                }

                override fun onSearch() {
                    changeUiState(UiState.Search)
                }

                override fun onAnalyze() {
                    coroutineScope.launch {
                        val start = System.currentTimeMillis()
                        val result = model.analyze()
                        changeUiState(UiState.Analysis)
                        val cost = System.currentTimeMillis() - start
                        toast("found $result, cost $cost ms")
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            model.suspectMap.collect { suspects ->
                val list = ArrayList<SuspectData>()
                suspects.forEach { (category, map) ->
                    list.add(SuspectData.Category(category))
                    map.forEach { entry -> list.add(SuspectData.Entry(entry)) }
                }
                suspectList = list
            }
        }

        Box {
            when (uiState.first) {
                UiState.Main ->
                    MainPane(
                        model,
                        modifier = Modifier.fillMaxSize(),
                        state = entryListState,
                        onEdit = { entry -> showEntryEditor(entry) },
                        callback = callback,
                        appVersion = if (debug) "${appVersion}D" else appVersion,
                        selectedTab = selectedTab,
                        onTabChange = { coroutineScope.launch { selectedTab = it } },
                    )

                UiState.Editor ->
                    EditorPane(
                        data = editorData,
                        modifier = Modifier.fillMaxSize(),
                        onSave = { key, text ->
                            coroutineScope.launch {
                                model.edit(key, text)
                                changeUiState() // hideEntryEditor
                            }
                        },
                        onCancel = { changeUiState() /* BACK */ },
                        onCopyToClipboard = { text ->
                            onCopyTo.invoke(text)
                            toast(text)
                        },
                        onTranslate = { text -> translateByGoogle(text) },
                        onCopyFromClipboard = onCopyFrom,
                        mode = model.appMode.value,
                    )

                UiState.Search ->
                    SearchPane(
                        model = model,
                        modifier = Modifier.fillMaxSize(),
                        onSelect = { key ->
                            // println("edit key = $key")
                            // hideSearchPane()
                            model.helper.dst(key)?.let { entry ->
                                showEntryEditor(entry)
                            }
                        },
                        onCancel = { changeUiState() /* BACK */ },
                    )

                UiState.Analysis ->
                    SuspectPane(
                        suspectList = suspectList,
                        state = suspectState,
                        onEdit = { entry -> showEntryEditor(entry) },
                        onHide = { changeUiState() /* BACK */ },
                    )
            }

            if (cached) {
                DebugSaveDialog(
                    onDismissRequest = { cached = false },
                    onSave = { saveEntryList(it) },
                    title = "write to ${model.helper.getCachedFile(model.appMode.value)}?",
                    modifier = Modifier.fillMaxWidth(.5f),
                )
            }

            if (loading.value) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = .25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colors.secondary)
                }
            }

            ToastUi(toasted)
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
    val mode = model.appMode.collectAsState()
    // val enabled = model.helper.loading.collectAsState() // global status
    var spec by remember { mutableStateOf(ToolbarSpec(enableAnalyze = true)) }
    val status = model.helper.status.collectAsState()

    LaunchedEffect(Unit) {
        model.helper.loading.collect { loading ->
            spec = spec.copy(
                enabled = loading.not(),
                enableAnalyze = model.appMode.value == PoHelper.Mode.DST,
            )
        }
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(MaterialTheme.colors.secondaryVariant),
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                // modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                backgroundColor = Color.Transparent, // MaterialTheme.colors.secondaryVariant,
                contentColor = MaterialTheme.colors.onSecondary,
                modifier = Modifier.weight(1f),
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabChange?.invoke(0) },
                ) {
                    Text("Config", modifier = Modifier.padding(8.dp))
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabChange?.invoke(1) },
                ) {
                    Text("Translation", modifier = Modifier.padding(8.dp))
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
                        mode = mode.value,
                        onModeChange = { mode ->
                            composeScope.launch { model.loadIniAndPo(mode) }
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
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    // println("get data: ${clipboard.getData(DataFlavor.stringFlavor)}")
    return clipboard.getData(DataFlavor.stringFlavor).toString()
}

/**
 * Open Google Translate and translate the text to chinese.
 * See https://stackoverflow.com/a/10967469
 *
 * @param text target text
 */
fun translateByGoogle(text: String) {
    val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else return
    if (desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
            val encoded = text.replace(" ", "%20")
            val url = "https://translate.google.com.tw/?hl=zh-TW&sl=en&tl=zh-TW&text=$encoded"
            desktop.browse(URL(url).toURI())
        } catch (e: Exception) {
            println("translateByGoogle: ${e.message}")
        }
    } else {
        println("unable to search $text")
        copyToSystemClipboard(text) // workaround
    }
}
