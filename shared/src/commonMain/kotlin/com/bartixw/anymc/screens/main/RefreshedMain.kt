/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_error
import anymc.shared.generated.resources.common_none
import anymc.shared.generated.resources.common_ok
import anymc.shared.generated.resources.ic_boots
import anymc.shared.generated.resources.ic_bricks
import anymc.shared.generated.resources.ic_chevron_up
import anymc.shared.generated.resources.ic_clock
import anymc.shared.generated.resources.ic_close
import anymc.shared.generated.resources.ic_console
import anymc.shared.generated.resources.ic_door_in_out
import anymc.shared.generated.resources.ic_globe
import anymc.shared.generated.resources.ic_home
import anymc.shared.generated.resources.ic_glasses
import anymc.shared.generated.resources.ic_image
import anymc.shared.generated.resources.ic_image_unknown
import anymc.shared.generated.resources.ic_more_options
import anymc.shared.generated.resources.ic_package
import anymc.shared.generated.resources.ic_plugin
import anymc.shared.generated.resources.ic_profile
import anymc.shared.generated.resources.ic_servers
import anymc.shared.generated.resources.ic_settings
import anymc.shared.generated.resources.ic_skull
import anymc.shared.generated.resources.ic_sprint
import anymc.shared.generated.resources.ic_warning
import anymc.shared.generated.resources.main_console_close
import anymc.shared.generated.resources.main_console_no_logs
import anymc.shared.generated.resources.main_console_title
import anymc.shared.generated.resources.main_error_crash_copied
import anymc.shared.generated.resources.main_error_crash_copy
import anymc.shared.generated.resources.main_error_crash_title
import anymc.shared.generated.resources.main_error_installation_title
import anymc.shared.generated.resources.main_error_no_logs
import anymc.shared.generated.resources.main_sidebar_screenshots
import anymc.shared.generated.resources.main_status_killing
import anymc.shared.generated.resources.main_status_launched
import anymc.shared.generated.resources.main_status_launching
import anymc.shared.generated.resources.main_status_ready
import anymc.shared.generated.resources.main_welcome_default
import anymc.shared.generated.resources.main_welcome_user
import anymc.shared.generated.resources.mainv2_home_screen
import anymc.shared.generated.resources.nbt_cancel
import anymc.shared.generated.resources.notice
import anymc.shared.generated.resources.profile_add_account
import anymc.shared.generated.resources.profile_stats_crouch_distance
import anymc.shared.generated.resources.profile_stats_info
import anymc.shared.generated.resources.profile_stats_longest_world
import anymc.shared.generated.resources.profile_stats_most_collected
import anymc.shared.generated.resources.profile_stats_sprint_distance
import anymc.shared.generated.resources.profile_stats_time_format_h
import anymc.shared.generated.resources.profile_stats_time_format_hm
import anymc.shared.generated.resources.profile_stats_time_format_m
import anymc.shared.generated.resources.profile_stats_time_format_s
import anymc.shared.generated.resources.profile_stats_title
import anymc.shared.generated.resources.profile_stats_total_deaths
import anymc.shared.generated.resources.profile_stats_total_distance
import anymc.shared.generated.resources.profile_stats_total_playtime
import anymc.shared.generated.resources.profile_stats_total_sessions
import anymc.shared.generated.resources.profile_title
import anymc.shared.generated.resources.settings_title
import anymc.shared.generated.resources.status_asset_dl
import anymc.shared.generated.resources.status_asset_fetch
import anymc.shared.generated.resources.status_client_dl
import anymc.shared.generated.resources.status_extract
import anymc.shared.generated.resources.status_info_fetch
import anymc.shared.generated.resources.status_info_write
import anymc.shared.generated.resources.status_java_fetch
import anymc.shared.generated.resources.status_java_install
import anymc.shared.generated.resources.status_library_dl
import anymc.shared.generated.resources.status_modpack_dl
import anymc.shared.generated.resources.versions_dialog_rename_label
import anymc.shared.generated.resources.versions_dialog_rename_title
import anymc.shared.generated.resources.versions_menu_delete
import anymc.shared.generated.resources.versions_menu_nbt
import anymc.shared.generated.resources.versions_menu_open_folder
import anymc.shared.generated.resources.versions_menu_rename
import anymc.shared.generated.resources.versions_menu_settings
import anymc.shared.generated.resources.versions_new_installation
import anymc.shared.generated.resources.versions_page_mods
import anymc.shared.generated.resources.versions_page_resourcepacks
import anymc.shared.generated.resources.versions_page_shaders
import anymc.shared.generated.resources.versions_page_worlds
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.dataclasses.LauncherConfig
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.managers.SkinManager
import com.bartixw.anymc.managers.StateManager
import com.bartixw.anymc.mc3d.Mc3DController
import com.bartixw.anymc.dataclasses.LauncherState
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.managers.StatsManager
import com.bartixw.anymc.managers.WindowManager
import com.bartixw.anymc.minecraft.data.Account
import com.bartixw.anymc.minecraft.data.DownloadStatus
import com.bartixw.anymc.minecraft.data.MinecraftStatus
import com.bartixw.anymc.screens.main.tabs.HomeTab
import com.bartixw.anymc.screens.main.tabs.ModsTab
import com.bartixw.anymc.screens.main.tabs.ProfileTab
import com.bartixw.anymc.screens.main.tabs.ServersTab
import com.bartixw.anymc.screens.main.tabs.WorldsTab
import com.bartixw.anymc.minecraft.data.StatusType
import com.bartixw.anymc.screens.main.tabs.ScreenshotsTab
import com.bartixw.anymc.screens.v2.dialogs.NewInstallationDialog
import com.bartixw.anymc.screens.versions.VersionsViewModel
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McLoadingIndicator
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Dropdown
import com.bartixw.anymc.ui.components.v2.DropdownItem
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.Input
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.SidebarItem
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.LocalTextStyle
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.toClipEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds

enum class MainSection {
    PROFILE, HOME, WORLDS, MODS, SHADERS, RESOURCE_PACKS, SERVERS, SCREENSHOTS
}

@Composable
fun RefreshedMainScreen(navController: NavController) {
    val accountListData by AccountManager.data.collectAsState()
    val activeAccount = accountListData.accounts.find { it.uuid == accountListData.activeAccountUuid }
    val cachedRenderFile = remember { PlatformUtils.userDataDirectory.resolve("last_render.png") }
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isCacheValid by remember { mutableStateOf(false) }
    var showLive by remember { mutableStateOf(false) }
    val frame by Mc3DController.activeFrame.collectAsState()
    val state by StateManager.data.collectAsState()
    val config by ConfigManager.data.collectAsState()
    var hasSavedFrame by remember { mutableStateOf(false) }
    val minecraftStatus by MinecraftManager.minecraftStatus.collectAsState()
    var quickPlayFlyoutVisible by remember { mutableStateOf(false) }
    val downloadStatus by MinecraftManager.downloadStatus.collectAsState()
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var isCrashLogCopied by remember { mutableStateOf(false) }
    val lastCrashLog by MinecraftManager.lastLog.collectAsState()
    val logs by MinecraftManager.consoleLogs.collectAsState()
    var consoleOpen by remember { mutableStateOf(false) }
    var currentSection by remember { mutableStateOf(MainSection.HOME) }

    val viewModel = viewModel {
        VersionsViewModel()
    }

    LaunchedEffect(viewModel.supportsMods) {
        if (!viewModel.supportsMods && currentSection == MainSection.MODS) {
            currentSection = MainSection.HOME
        }
    }

    LaunchedEffect(frame != null) {
        showLive = frame != null
    }

    LaunchedEffect(activeAccount) {
        val account = activeAccount ?: return@LaunchedEffect
        val skinPath = SkinManager.getSkinPath(account)

        isCacheValid = skinPath != null && skinPath == state.lastSkinPath && cachedRenderFile.exists()

        if (isCacheValid) {
            withContext(Dispatchers.IO) {
                try {
                    val img = ImageIO.read(cachedRenderFile)
                    cachedBitmap = img.toComposeImageBitmap()
                } catch (_: Exception) {
                    isCacheValid = false
                }
            }
        }
    }

    LaunchedEffect(showLive) {
        if (showLive && !hasSavedFrame) {
            hasSavedFrame = true
            Mc3DController.saveNextFrame(cachedRenderFile)
            activeAccount?.let { account ->
                val skinPath = SkinManager.getSkinPath(account)
                StateManager.update { it.copy(lastSkinPath = skinPath) }
            }
        }
    }

    LaunchedEffect(minecraftStatus) {
        if (minecraftStatus == MinecraftStatus.LAUNCHED) {
            if (config.showConsoleOnLaunch) {
                consoleOpen = true
            }
            if (config.minimizeOnLaunch) {
                WindowManager.minimize()
            }
        } else if (config.minimizeOnLaunch && (minecraftStatus == MinecraftStatus.READY || minecraftStatus == MinecraftStatus.FAILED)) {
            WindowManager.restore()
        }
    }

    if (downloadStatus.errorCode != null) {
        Dialog(
            onDismissRequest = { MinecraftManager.cleanError() },
            title = { Text(stringResource(Res.string.main_error_installation_title)) },
            buttons = {
                Button(
                    content = { Text(stringResource(Res.string.common_ok)) },
                    type = ButtonType.Primary,
                    onClick = { MinecraftManager.cleanError() }
                )
            }
        ) {
            Text(downloadStatus.errorCode!!)
        }
    }

    if (minecraftStatus == MinecraftStatus.FAILED) {
        val scrollState = rememberScrollState()
        Dialog(
            onDismissRequest = {
                MinecraftManager.clearCrash()

                isCrashLogCopied = false
            },
            title = { Text(stringResource(Res.string.main_error_crash_title)) },
            buttons = {
                Button(
                    content = { Text(
                        if (isCrashLogCopied) stringResource(Res.string.main_error_crash_copied)
                        else stringResource(Res.string.main_error_crash_copy)
                    ) },
                    disabled = isCrashLogCopied,
                    onClick = {
                        coroutineScope.launch {
                            clipboard.setClipEntry((lastCrashLog ?: "Brak").toClipEntry())
                        }

                        isCrashLogCopied = true
                    }
                )

                Button(
                    content = { Text(stringResource(Res.string.common_ok)) },
                    type = ButtonType.Primary,
                    onClick = {
                        MinecraftManager.clearCrash()

                        isCrashLogCopied = false
                    }
                )
            }
        ) {
            Row(
                Modifier.heightIn(max = 300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .background(Color(0xFF1E1E1E))
                        .padding(8.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = lastCrashLog ?: stringResource(Res.string.main_error_no_logs),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MinecraftTheme.colors.textPrimary
                        )
                    )
                }
                VerticalScrollbar(rememberScrollbarAdapter(scrollState))
            }
        }
    }

    if (consoleOpen) Dialog(
        onDismissRequest = {
            consoleOpen = false
        },
        title = { Text(stringResource(Res.string.main_console_title)) },
        buttons = {
            Button(
                content = { Text(stringResource(Res.string.main_console_close)) },
                onClick = {
                    consoleOpen = false
                }
            )
        }
    ) {
        val listState = rememberScrollState()
        val combinedText = remember(logs) { logs.joinToString("\n") }

        LaunchedEffect(combinedText) {
            listState.animateScrollTo(listState.maxValue)
        }

        Row(
            modifier = Modifier.height(450.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF1E1E1E))
                    .verticalScroll(listState)
                    .padding(8.dp)
            ) {
                Text(
                    text = if (logs.isNotEmpty()) combinedText else stringResource(Res.string.main_console_no_logs),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MinecraftTheme.colors.textPrimary,
                        lineHeight = 16.sp
                    )
                )
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 2.dp)
            )
        }
    }

    Row {
        Column(
            modifier = Modifier.width(200.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                SidebarItem(
                    icon = { Icon(Res.drawable.ic_profile, contentDescription = null) },
                    title = { Text(stringResource(Res.string.profile_title)) },
                    description = {},
                    onClick = { currentSection = MainSection.PROFILE },
                    selected = currentSection == MainSection.PROFILE
                )
                SidebarItem(
                    icon = { Icon(Res.drawable.ic_home, contentDescription = null) },
                    title = { Text(stringResource(Res.string.mainv2_home_screen)) },
                    description = {},
                    onClick = { currentSection = MainSection.HOME },
                    selected = currentSection == MainSection.HOME
                )
                SidebarItem(
                    icon = { Icon(Res.drawable.ic_globe, contentDescription = null) },
                    title = { Text(stringResource(Res.string.versions_page_worlds)) },
                    description = {},
                    onClick = { currentSection = MainSection.WORLDS },
                    selected = currentSection == MainSection.WORLDS
                )
                if (viewModel.supportsMods) {
                    SidebarItem(
                        icon = { Icon(Res.drawable.ic_plugin, contentDescription = null) },
                        title = { Text(stringResource(Res.string.versions_page_mods)) },
                        description = {},
                        onClick = { currentSection = MainSection.MODS },
                        selected = currentSection == MainSection.MODS
                    )
                    SidebarItem(
                        icon = { Icon(Res.drawable.ic_glasses, contentDescription = null) },
                        title = { Text(stringResource(Res.string.versions_page_shaders)) },
                        description = {},
                        onClick = { currentSection = MainSection.SHADERS },
                        selected = currentSection == MainSection.SHADERS
                    )
                    SidebarItem(
                        icon = { Icon(Res.drawable.ic_package, contentDescription = null) },
                        title = { Text(stringResource(Res.string.versions_page_resourcepacks)) },
                        description = {},
                        onClick = { currentSection = MainSection.RESOURCE_PACKS },
                        selected = currentSection == MainSection.RESOURCE_PACKS
                    )
                }
                SidebarItem(
                    icon = { Icon(Res.drawable.ic_image, contentDescription = null) },
                    title = { Text(stringResource(Res.string.main_sidebar_screenshots)) },
                    description = {},
                    onClick = { currentSection = MainSection.SCREENSHOTS },
                    selected = currentSection == MainSection.SCREENSHOTS
                )
//                SidebarItem(
//                    icon = { Icon(Res.drawable.ic_servers, contentDescription = null) },
//                    title = { Text("Servers") },
//                    description = {},
//                    onClick = { currentSection = MainSection.SERVERS },
//                    selected = currentSection == MainSection.SERVERS
//                )
            }
            SidebarItem(
                icon = { Icon(Res.drawable.ic_settings, contentDescription = null) },
                title = { Text(stringResource(Res.string.settings_title)) },
                description = {},
                onClick = {
                    navController.navigate("settings")
                },
                selected = false
            )
        }
        Column(Modifier.weight(3f)) {
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                }
            ) { section ->
                when (section) {
                    MainSection.HOME -> HomeTab(
                        activeAccount = activeAccount,
                        isCacheValid = isCacheValid,
                        cachedBitmap = cachedBitmap,
                        showLive = showLive,
                        frame = frame,
                        downloadStatus = downloadStatus,
                        minecraftStatus = minecraftStatus,
                        state = state,
                        config = config,
                        viewModel = viewModel,
                        navController = navController,
                        quickPlayFlyoutVisible = quickPlayFlyoutVisible,
                        onQuickPlayFlyoutToggle = { quickPlayFlyoutVisible = it },
                        onShowConsole = { consoleOpen = true }
                    )

                    MainSection.PROFILE -> ProfileTab(
                        frame = frame
                    )

                    MainSection.WORLDS -> WorldsTab(
                        viewModel = viewModel,
                        navController = navController,
                        minecraftStatus = minecraftStatus
                    )

                    MainSection.MODS -> ModsTab(
                        viewModel = viewModel,
                        navController = navController
                    )

                    MainSection.SHADERS -> ModsTab(
                        viewModel = viewModel,
                        navController = navController,
                        type = "shaders"
                    )

                    MainSection.RESOURCE_PACKS -> ModsTab(
                        viewModel = viewModel,
                        navController = navController,
                        type = "resourcepacks"
                    )

                    MainSection.SERVERS -> ServersTab()

                    MainSection.SCREENSHOTS -> ScreenshotsTab(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
