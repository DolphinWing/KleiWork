// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed
// by the Apache 2.0 license that can be found in the LICENSE file.

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dolphin.desktop.apps.onitranslator.app.OniTranslatorApp
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.app_name
import dolphin.desktop.apps.onitranslator.generated.resources.nisbet_ponder
import dolphin.desktop.apps.onitranslator.model.DesktopPoHelper
import dolphin.desktop.apps.onitranslator.model.Ini
import dolphin.desktop.apps.onitranslator.model.PoDataModel
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

fun main(args: Array<String>) = application {
//    println(args.contentToString())
    val version = args.find { it.startsWith("v=") }?.drop(2) ?: "x.x.x"

//    val osName: String = System.getProperties().getProperty("os.name")
//    println("os.name = $osName")

    var debugMode by remember { mutableStateOf(false) }

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
        debugMode = debug

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
        OniTranslatorApp(
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
