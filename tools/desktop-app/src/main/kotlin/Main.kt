// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed
// by the Apache 2.0 license that can be found in the LICENSE file.

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dolphin.desktop.apps.onitranslator.app.OniTranslatorApp
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.app_name
import dolphin.desktop.apps.onitranslator.generated.resources.nisbet_ponder
import dolphin.desktop.apps.onitranslator.model.OniTranslatorViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.io.File

fun main(args: Array<String>) = application {
//    println(args.contentToString())
    val version = args.find { it.startsWith("v=") }?.drop(2) ?: "x.x.x"

//    val tempDir: String = System.getProperty("java.io.tmpdir")
//    println("tempDir = $tempDir")
//
//    val homeDir: String = System.getProperty("user.home")
//    println("homeDir = $homeDir")

    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember {
        val workingDir: String = System.getProperties().getProperty("user.dir")
        println("workingDir = $workingDir")

        val debug = File(workingDir, "build").exists() // has build dir
        val appVersion = if (debug) "${version}D" else version
        OniTranslatorViewModel(appVersion, debugMode = debug)
    }
    val appState by viewModel.state.collectAsState()
    val windowState = rememberWindowState(
        size = appState.uiState.windowSize,
        position = appState.uiState.windowPosition
    )

    Window(
        visible = windowState.size.width.value > 0 && windowState.size.height.value > 0,
        onCloseRequest = {
            coroutineScope.launch {
                viewModel.rememberLastWindowState(windowState)
                exitApplication()
            }
        },
        state = windowState,
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.nisbet_ponder),
    ) {
        OniTranslatorApp(
            state = appState,
            onEvent = viewModel::onEvent,
            onEditorConvert = { viewModel.onConvert(it) }
        )
    }

    // Observe window state changes from ViewModel and apply to actual windowState
    LaunchedEffect(appState.uiState.windowPosition, appState.uiState.windowSize) {
        windowState.position = appState.uiState.windowPosition
        windowState.size = appState.uiState.windowSize
    }
}
