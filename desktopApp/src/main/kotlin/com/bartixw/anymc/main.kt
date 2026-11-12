/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.foundation.text.BasicText
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.app_icon
import anymc.shared.generated.resources.common_cancel
import anymc.shared.generated.resources.settings_uninstall_confirm_btn
import anymc.shared.generated.resources.settings_uninstall_delete_data
import anymc.shared.generated.resources.settings_uninstall_delete_storage
import anymc.shared.generated.resources.settings_uninstall_dialog_desc
import anymc.shared.generated.resources.settings_uninstall_dialog_title
import com.bartixw.anymc.core.system.SystemRegistryManager
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.core.utils.StandardPlatformPaths
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Checkbox
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.toClipEntry
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.lwjgl.glfw.GLFW
import java.awt.Dimension
import java.io.File
import javax.swing.UIManager
import javax.swing.plaf.metal.MetalLookAndFeel
import kotlin.system.exitProcess

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String> = emptyArray()) {
    PlatformUtils.initialize(StandardPlatformPaths("AnyMC"))

    val argsMap = args.toList().chunked(2).associate { if (it.size == 2) it[0] to it[1] else it[0] to "" }
    val updateApiHost = argsMap["--update-api-host"] ?: argsMap["--api-host"]
    if (!updateApiHost.isNullOrBlank()) {
        ConfigManager.loadBlocking()
        ConfigManager.update { it.copy(updateApiHost = updateApiHost) }
        println("Changed update API host to $updateApiHost")
    }

    // GraalVM native image check - set java.home to the executable directory to allow AWT/Skiko to find native libs
    val currentJavaHome = System.getProperty("java.home")
    if (currentJavaHome == null || currentJavaHome.isEmpty() || currentJavaHome == "null") {
        val imagePath = System.getProperty("org.graalvm.nativeimage.imagepath")
        val path = if (imagePath != null) {
            File(imagePath).parentFile.absolutePath
        } else {
            File(".").absolutePath
        }
        System.setProperty("java.home", path)
    }

    PlatformUtils.initialize(StandardPlatformPaths("AnyMC"))

    // Handle CLI --uninstall argument (e.g. invoked by Windows Settings "Uninstall" button)
    if (args.contains("--uninstall") || args.contains("/uninstall")) {
        val isQuiet = args.contains("--quiet") || args.contains("/quiet")
        if (isQuiet) {
            SystemRegistryManager.uninstallLauncher()
            exitProcess(0)
        }

        try {
            UIManager.setLookAndFeel(MetalLookAndFeel())
        } catch (_: Exception) {}

        application {
            val state = rememberWindowState(width = 520.dp, height = 340.dp)
            Window(
                onCloseRequest = ::exitApplication,
                title = "AnyMC Launcher - Uninstall",
                state = state,
                icon = painterResource(Res.drawable.app_icon),
                undecorated = true,
                resizable = false
            ) {
                var deleteUserData by remember { mutableStateOf(false) }
                var deleteSharedStorage by remember { mutableStateOf(false) }

                MinecraftTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MinecraftTheme.colors.dialogBackground)
                            .border(1.dp, MinecraftTheme.colors.dialogBorder)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = stringResource(Res.string.settings_uninstall_dialog_title).uppercase(),
                                style = MinecraftTheme.typographyAnyMC.copy(fontSize = 24.sp)
                            )

                            Text(
                                text = stringResource(Res.string.settings_uninstall_dialog_desc),
                                style = MinecraftTheme.typographyPixel.copy(color = MinecraftTheme.colors.textSecondary)
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = deleteUserData,
                                    onCheckedChange = { deleteUserData = it }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.settings_uninstall_delete_data), style = MinecraftTheme.typographyPixel)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = deleteSharedStorage,
                                    onCheckedChange = { deleteSharedStorage = it }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.settings_uninstall_delete_storage), style = MinecraftTheme.typographyPixel)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                content = { Text(stringResource(Res.string.common_cancel)) },
                                type = ButtonType.Secondary,
                                onClick = ::exitApplication
                            )

                            Spacer(Modifier.width(12.dp))

                            Button(
                                content = { Text(stringResource(Res.string.settings_uninstall_confirm_btn).uppercase()) },
                                type = ButtonType.Destructive,
                                onClick = {
                                    SystemRegistryManager.uninstallLauncher(
                                        deleteUserData = deleteUserData,
                                        deleteSharedStorage = deleteSharedStorage
                                    )
                                    exitProcess(0)
                                }
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // Force GLFW to use X11 on Linux and initialize it on the main thread
    try {
        val osName = System.getProperty("os.name").lowercase()
        if (osName.contains("linux")) {
            GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_X11)
        }
        
        if (!GLFW.glfwInit()) {
            println("Main: CRITICAL - Failed to initialize GLFW")
        } else {
            println("Main: GLFW initialized successfully")
        }
    } catch (e: Throwable) {
        println("Main: Failed to set GLFW_PLATFORM hint or initialize: ${e.message}")
    }

    try {
        UIManager.setLookAndFeel(MetalLookAndFeel())
    } catch (e: Exception) {
        e.printStackTrace()
    }

    application {
        var appError by remember { mutableStateOf<Throwable?>(null) }
        val exceptionHandlerFactory = WindowExceptionHandlerFactory {
            WindowExceptionHandler { throwable ->
                appError = throwable
            }
        }

        CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides exceptionHandlerFactory) {
            val state = rememberWindowState(
                width = 1200.dp,
                height = 630.dp,
            )

            Window(
                onCloseRequest = ::exitApplication,
                title = "AnyMC Launcher",
                state = state,
                icon = painterResource(Res.drawable.app_icon),
                undecorated = true,
            ) {
                window.minimumSize = Dimension(1200, 630)
                val currentError = appError
                if (currentError != null) {
                    val clipboard = LocalClipboard.current
                    val coroutineScope = rememberCoroutineScope()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF444444))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            BasicText("AnyMC Launcher uległ awarii", style = TextStyle(color = Color(0xFFEEEEEE)))
                            Spacer(Modifier.size(16.dp))
                            val scrollState = rememberScrollState()
                            Row (Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(scrollState)
                                        .background(Color(0xFF222222))
                                        .weight(1f)
                                ) {
                                    val stackTrace = currentError.stackTraceToString()
                                    BasicText(
                                        text = "${currentError.message}\n$stackTrace",
                                        style = TextStyle(color = Color(0xFFEEEEEE)),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                VerticalScrollbar(rememberScrollbarAdapter(scrollState))
                            }
                            Spacer(Modifier.size(16.dp))
                            BasicText("Musisz uruchomić ponownie AnyMC Launcher, aby zamknąć ten błąd", style = TextStyle(color = Color(0xFFEEEEEE)))
                            Spacer(Modifier.size(16.dp))

                            Row {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF333333))
                                        .clickable(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val stackTrace = currentError.stackTraceToString()
                                                    clipboard.setClipEntry("${currentError.localizedMessage}\n$stackTrace".toClipEntry())
                                                }
                                            }
                                        )
                                ) {
                                    BasicText(
                                        text = "Kopiuj błąd",
                                        style = TextStyle(color = Color(0xFFEEEEEE)),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                Spacer(Modifier.size(8.dp))

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF333333))
                                        .clickable(
                                            onClick = {
                                                (::exitApplication)()
                                            }
                                        )
                                ) {
                                    BasicText(
                                        text = "Zamknij aplikację",
                                        style = TextStyle(color = Color(0xFFEEEEEE)),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LaunchedEffect(Unit) {
                        try {
                            val screen = java.awt.Toolkit.getDefaultToolkit().screenSize
                            window.maximumSize = Dimension(screen.width, screen.height)
                        } catch (e: Throwable) {
                            println("Nie udało się pobrać rozmiaru ekranu: ${e.message}")
                        }
                    }

                    val config by ConfigManager.data.collectAsState(ConfigManager.data.value)
                    LaunchedEffect(config.controllerSupport) {
                        if (config.controllerSupport) {
                            GamepadManager.startPolling(this, window)
                        } else {
                            GamepadManager.stopPolling()
                        }
                    }

                    App(
                        isMaximized = state.placement == WindowPlacement.Maximized,
                        onMinimize = { state.isMinimized = true },
                        onRestore = { state.isMinimized = false },
                        onMaximize = {
                            state.placement = if (state.placement == WindowPlacement.Maximized) {
                                WindowPlacement.Floating
                            } else {
                                WindowPlacement.Maximized
                            }
                        },
                        onClose = ::exitApplication,
                        windowDraggableArea = { modifier, content ->
                            this@Window.WindowDraggableArea(modifier) {
                                content()
                            }
                        }
                    )
                }
            }
        }
    }
}
