/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_close
import anymc.shared.generated.resources.ic_maximize
import anymc.shared.generated.resources.ic_minimize
import anymc.shared.generated.resources.ic_restore
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key as keyProperty
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.text.style.BaselineShift
import anymc.shared.generated.resources.app_icon
import com.bartixw.anymc.core.updater.UpdateExecutor
import com.bartixw.anymc.core.updater.UpdateInfo
import com.bartixw.anymc.core.updater.UpdateManager
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Dialog
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess
import org.jetbrains.compose.resources.DrawableResource
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.managers.StateManager
import com.bartixw.anymc.managers.WindowManager
import com.bartixw.anymc.mc3d.Mc3DController
import com.bartixw.anymc.screens.debug.SimulationScreen
import com.bartixw.anymc.screens.link.setup.LinkSetupDownloadStep
import com.bartixw.anymc.screens.main.MainScreen
import com.bartixw.anymc.screens.main.RefreshedMainScreen
import com.bartixw.anymc.screens.modrinth.ModrinthDetailRoute
import com.bartixw.anymc.screens.modrinth.ModrinthDetailScreen
import com.bartixw.anymc.screens.modrinth.ModrinthModpackDetailRoute
import com.bartixw.anymc.screens.modrinth.ModrinthModpackDetailScreen
import com.bartixw.anymc.screens.modrinth.ModrinthModpackScreen
import com.bartixw.anymc.screens.modrinth.ModrinthRoute
import com.bartixw.anymc.screens.modrinth.ModrinthScreen
import com.bartixw.anymc.screens.profile.ProfileListScreen
import com.bartixw.anymc.screens.servers.ServersListScreen
import com.bartixw.anymc.screens.settings.CreditsScreen
import com.bartixw.anymc.screens.settings.LicensesScreen
import com.bartixw.anymc.screens.settings.SettingsScreen
import com.bartixw.anymc.screens.v2.ComponentTestScreen
import com.bartixw.anymc.screens.versions.VersionsScreen
import com.bartixw.anymc.screens.versions.create.ChooseVersionRoute
import com.bartixw.anymc.screens.versions.create.VersionsCreateChooseTypeScreen
import com.bartixw.anymc.screens.versions.create.VersionsCreateChooseVersionScreen
import com.bartixw.anymc.managers.TextureManager
import com.bartixw.anymc.screens.welcome.DownloadTexturesScreen
import com.bartixw.anymc.screens.welcome.EulaScreen
import com.bartixw.anymc.screens.welcome.WelcomeActionsScreenV2
import com.bartixw.anymc.screens.welcome.WelcomeFilesystemScreenV2
import com.bartixw.anymc.screens.welcome.WelcomeInstallingScreen
import com.bartixw.anymc.screens.welcome.WelcomeScreenV2
import com.bartixw.anymc.ui.components.McIcon

import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.LocalMinecraftColors
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.ui.theme.ThemeManager
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.imageResource
import java.util.Locale

private val initialLocale = Locale.getDefault()

@Composable
private fun RowScope.McTitleBarIcon(
    resource: DrawableResource,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val inputMode = LocalInputModeManager.current.inputMode

    McIcon(
        resource,
        contentDescription = contentDescription,
        modifier = Modifier
            .weight(1f)
            .then(if (isFocused && inputMode == InputMode.Keyboard) Modifier.border(2.dp, Color.White) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(10.dp)
    )
}

@Composable
@Preview
fun App(
    isMaximized: Boolean = false,
    onMinimize: () -> Unit = {},
    onRestore: () -> Unit = {},
    onMaximize: () -> Unit = {},
    onClose: () -> Unit = {},
    windowDraggableArea: @Composable (Modifier, @Composable () -> Unit) -> Unit = { modifier, content ->
        Box(modifier) { content() }
    }
) {
    LaunchedEffect(onMinimize, onRestore) {
        WindowManager.onMinimize = onMinimize
        WindowManager.onRestore = onRestore
    }

    remember {
        if (!ConfigManager.isLoaded) ConfigManager.loadBlocking()
        if (!StateManager.isLoaded) StateManager.loadBlocking()
        if (!AccountManager.isLoaded) AccountManager.loadBlocking()
        ThemeManager.initialize()
    }

    val startDestination = when {
        !ConfigManager.data.value.eulaAccepted -> "welcome/eula"
        !TextureManager.hasAllTextures() -> "welcome/download_textures"
        ConfigManager.data.value.isNew -> "welcome"
        else -> "main"
    }

    val themeName by ConfigManager.data.map { it.theme }.collectAsState(ConfigManager.data.value.theme)
    val themeColors = remember(themeName) {
        ThemeManager.getThemeColors(themeName)
    }

    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    val rootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        AccountManager.refreshActiveAccount()
        if (ConfigManager.data.value.eulaAccepted && TextureManager.hasAllTextures()) {
            Mc3DController.init()
        }
        if (ConfigManager.data.value.controllerSupport) {
            rootFocusRequester.requestFocus()
        }
    }

    MinecraftTheme(
        colors = themeColors
    ) {
        val config by ConfigManager.data.collectAsState(ConfigManager.data.value)
        var startupUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        var isDownloadingStartup by remember { mutableStateOf(false) }
        var downloadProgressStartup by remember { mutableStateOf(0f) }
        var updateCompletedStartup by remember { mutableStateOf(false) }
        var preparedExtractedDirStartup by remember { mutableStateOf<File?>(null) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            val now = System.currentTimeMillis()
            val lastCheck = config.lastUpdateCheckTimestamp
            // 1 hour rate limit = 3600_000 ms
            if (now - lastCheck > 3600_000L) {
                try {
                    val client = HttpClient(CIO) {
                        install(ContentNegotiation) {
                            json(Json { ignoreUnknownKeys = true })
                        }
                    }
                    val manager = UpdateManager(client, currentVersion = "1.0.0", apiHost = config.updateApiHost)
                    val info = manager.checkForUpdate()
                    if (info != null) {
                        startupUpdateInfo = info
                    }
                    ConfigManager.update { it.copy(lastUpdateCheckTimestamp = now) }
                    client.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        startupUpdateInfo?.let { info ->
            Dialog(
                onDismissRequest = { if (!isDownloadingStartup) startupUpdateInfo = null },
                title = { Text("Update Available: ${info.latestVersion}") },
                buttons = {
                    if (!isDownloadingStartup && !updateCompletedStartup) {
                        Button(
                            content = { Text("Download & Update") },
                            onClick = {
                                coroutineScope.launch {
                                    isDownloadingStartup = true
                                    try {
                                        val client = HttpClient(CIO) {
                                            install(ContentNegotiation) {
                                                json(Json { ignoreUnknownKeys = true })
                                            }
                                        }
                                        val executor = UpdateExecutor()
                                        val extracted = executor.downloadAndPrepareUpdate(client, info) { progress ->
                                            downloadProgressStartup = progress
                                        }
                                        preparedExtractedDirStartup = extracted
                                        updateCompletedStartup = true
                                        client.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isDownloadingStartup = false
                                    }
                                }
                            },
                            type = ButtonType.Primary
                        )
                    }
                    if (updateCompletedStartup) {
                        Button(
                            content = { Text("Restart & Apply Update") },
                            onClick = {
                                preparedExtractedDirStartup?.let {
                                    UpdateExecutor().applyUpdateAndRestart(it)
                                } ?: exitProcess(0)
                            },
                            type = ButtonType.Primary
                        )
                    }
                    Button(
                        content = { Text(if (updateCompletedStartup) "Close" else "Cancel") },
                        onClick = {
                            if (updateCompletedStartup) {
                                exitProcess(0)
                            } else {
                                startupUpdateInfo = null
                            }
                        },
                        type = ButtonType.Secondary
                    )
                }
            ) {
                Column {
                    Text("A new version (${info.latestVersion}) is available!")
                    Spacer(Modifier.size(8.dp))
                    if (isDownloadingStartup) {
                        Text("Downloading update... ${(downloadProgressStartup * 100).toInt()}%")
                    } else if (updateCompletedStartup) {
                        Text("Update installed successfully! Please restart the app.")
                    } else {
                        info.releaseNotes?.let {
                            Spacer(Modifier.size(8.dp))
                            Text("Release Notes:")
                            Text(it)
                        }
                    }
                }
            }
        }

        LaunchedEffect(config.language) {
            val locale = when (config.language) {
                "pl" -> Locale.forLanguageTag("pl")
                "en" -> Locale.forLanguageTag("en")
                else -> initialLocale
            }
            Locale.setDefault(locale)
        }

        key(config.language) {
            Column(
                modifier = Modifier
                    .focusRequester(rootFocusRequester)
                    .focusGroup()
                    .onPreviewKeyEvent { event ->
                        if (config.controllerSupport && event.type == KeyEventType.KeyDown) {
                            println("App: onPreviewKeyEvent: ${event.keyProperty}, inputMode: ${inputModeManager.inputMode}")
                            val direction = when (event.keyProperty) {
                                Key.DirectionUp -> FocusDirection.Up
                                Key.DirectionDown -> FocusDirection.Down
                                Key.DirectionLeft -> FocusDirection.Left
                                Key.DirectionRight -> FocusDirection.Right
                                else -> null
                            }
                            if (direction != null) {
                                val moved = focusManager.moveFocus(direction)
                                println("App: Manual focus move: $direction, result=$moved")
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    }
                    .focusable()
            ) {
                windowDraggableArea(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(MinecraftTheme.colors.backgroundDirt)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            bitmap = imageResource(Res.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.size(38.dp)
                        )
                        Row(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ANYMC LAUNCHER ", style = MinecraftTheme.typographyAnyMC.merge(baselineShift = BaselineShift(0.3f)))
                        }
                        Row(
                            Modifier
                                .width(IntrinsicSize.Min)
                        ) {
                            McTitleBarIcon(
                                Res.drawable.ic_minimize,
                                contentDescription = "Minimize",
                                onClick = onMinimize
                            )
                            McTitleBarIcon(
                                if (isMaximized) Res.drawable.ic_restore else Res.drawable.ic_maximize,
                                contentDescription = if (isMaximized) "Restore" else "Maximize",
                                onClick = onMaximize
                            )
                            McTitleBarIcon(
                                Res.drawable.ic_close,
                                contentDescription = "Close",
                                onClick = onClose
                            )
                        }
                    }
                }
                AppNavigation(startDestination)
            }
        }
    }
}

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()
    val config by ConfigManager.data.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = LocalMinecraftColors.current.backgroundDirt)
            .focusGroup()
            .focusable()
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn() },
            exitTransition = { slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut() }
        ) {
            composable(route = "main") {
                if (config.refreshedUI) RefreshedMainScreen(navController)
                else MainScreen(navController)
            }
            composable(route = "profile/list") {
                ProfileListScreen(navController)
            }
            composable(route = "versions/list") {
                VersionsScreen(navController)
            }
            composable<com.bartixw.anymc.screens.nbt.NbtEditorRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<com.bartixw.anymc.screens.nbt.NbtEditorRoute>()
                com.bartixw.anymc.screens.nbt.NbtEditorScreen(navController, args.installationId)
            }
            composable(route = "versions/create/type") {
                VersionsCreateChooseTypeScreen(navController)
            }
            composable(route = "versions/create/modpack") {
                ModrinthModpackScreen(navController)
            }
            composable<ModrinthModpackDetailRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ModrinthModpackDetailRoute>()
                ModrinthModpackDetailScreen(navController, args.projectId)
            }
            composable<ChooseVersionRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ChooseVersionRoute>()
                VersionsCreateChooseVersionScreen(navController, args.type)
            }
            composable<ModrinthRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ModrinthRoute>()
                ModrinthScreen(navController, args.installationId, args.mcVersion, args.loader, args.projectType)
            }
            composable<ModrinthDetailRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ModrinthDetailRoute>()
                ModrinthDetailScreen(navController, args.projectId, args.installationId, args.mcVersion, args.loader, args.projectType)
            }
            composable(route = "settings") {
                if (config.refreshedUI) com.bartixw.anymc.screens.settings.v2.SettingsScreenV2(navController)
                else SettingsScreen(navController)
            }
            composable(route = "settings_v2") {
                com.bartixw.anymc.screens.settings.v2.SettingsScreenV2(navController)
            }
            composable(route = "settings/credits") {
                CreditsScreen(navController)
            }
            composable(route = "settings/licenses") {
                LicensesScreen(navController)
            }
            composable(route = "welcome/eula") {
                EulaScreen(navController)
            }
            composable(route = "welcome/download_textures") {
                DownloadTexturesScreen(navController)
            }
            composable(route = "welcome") {
                WelcomeScreenV2(navController)
            }
            composable(route = "welcome/filesystem") {
                WelcomeFilesystemScreenV2(navController)
            }
            composable(route = "welcome/actions") {
                WelcomeActionsScreenV2(navController)
            }
            composable(route = "welcome/installing") {
                WelcomeInstallingScreen(navController)
            }
            composable(route = "servers/list") {
                ServersListScreen(navController)
            }
            composable(route = "test") {
                ComponentTestScreen(navController)
            }
            composable(route = "debug/simulation") {
                SimulationScreen(navController)
            }
            composable(route = "link/setup/download") {
                LinkSetupDownloadStep(navController)
            }
        }
    }
}
