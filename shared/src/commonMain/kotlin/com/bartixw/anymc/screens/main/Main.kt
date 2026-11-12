/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_check
import anymc.shared.generated.resources.ic_chevron_down
import anymc.shared.generated.resources.ic_close
import anymc.shared.generated.resources.ic_console
import anymc.shared.generated.resources.ic_exit
import anymc.shared.generated.resources.ic_plus
import anymc.shared.generated.resources.ic_power
import anymc.shared.generated.resources.ic_profile
import anymc.shared.generated.resources.ic_save
import anymc.shared.generated.resources.ic_settings
import com.bartixw.anymc.minecraft.data.GameMode
import com.bartixw.anymc.minecraft.data.QuickPlayType
import com.bartixw.anymc.minecraft.data.MinecraftStatus
import com.bartixw.anymc.minecraft.data.StatusType
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.managers.StateManager
import com.bartixw.anymc.managers.WindowManager
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import com.bartixw.anymc.managers.SkinManager
import com.bartixw.anymc.mc3d.Mc3DController
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McDialog
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.DateTimeFormatterUtil
import com.bartixw.anymc.utils.toClipEntry
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import anymc.shared.generated.resources.ic_servers
import anymc.shared.generated.resources.common_ok
import anymc.shared.generated.resources.main_console_close
import anymc.shared.generated.resources.main_console_title
import anymc.shared.generated.resources.main_error_crash_copied
import anymc.shared.generated.resources.main_error_crash_copy
import anymc.shared.generated.resources.main_error_crash_title
import anymc.shared.generated.resources.main_error_installation_title
import anymc.shared.generated.resources.main_error_no_logs
import anymc.shared.generated.resources.main_gamemode_adventure
import anymc.shared.generated.resources.main_gamemode_creative
import anymc.shared.generated.resources.main_gamemode_spectator
import anymc.shared.generated.resources.main_gamemode_survival
import anymc.shared.generated.resources.main_last_game_logs
import anymc.shared.generated.resources.main_nav_profile
import anymc.shared.generated.resources.main_nav_versions
import anymc.shared.generated.resources.main_quickplay_info
import anymc.shared.generated.resources.main_quickplay_join
import anymc.shared.generated.resources.main_quickplay_multiplayer
import anymc.shared.generated.resources.main_quickplay_realms
import anymc.shared.generated.resources.main_quickplay_singleplayer
import anymc.shared.generated.resources.main_status_killing
import anymc.shared.generated.resources.main_status_launched
import anymc.shared.generated.resources.main_status_launching
import anymc.shared.generated.resources.main_status_ready
import anymc.shared.generated.resources.main_welcome_default
import anymc.shared.generated.resources.main_welcome_user
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
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.core.utils.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.imageio.ImageIO
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainScreen(navController: NavController) {
    val downloadStatus by MinecraftManager.downloadStatus.collectAsState()
    val minecraftStatus by MinecraftManager.minecraftStatus.collectAsState()
    val lastCrashLog by MinecraftManager.lastLog.collectAsState()
    val accountListData by AccountManager.data.collectAsState()
    val logs by MinecraftManager.consoleLogs.collectAsState()
    val activeAccount = accountListData.accounts.find { it.uuid == accountListData.activeAccountUuid }
    val state by StateManager.data.collectAsState()
    val config by ConfigManager.data.collectAsState()
    val quickPlay =
        if (state.lastSelectedInstanceId != null)
            remember(state.lastSelectedInstanceId, minecraftStatus == MinecraftStatus.READY) {
                MinecraftManager.getQuickPlayData(state.lastSelectedInstanceId!!)
            }
        else null
    var launchMoreOptionsExpanded by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var isCrashLogCopied by remember { mutableStateOf(false) }
    var consoleOpen by remember { mutableStateOf(false) }

    val playButtonFocusRequester = remember { FocusRequester() }
    val mapFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (config.controllerSupport) {
            playButtonFocusRequester.requestFocus()
        }
    }

    val frame by Mc3DController.activeFrame.collectAsState()

    val cachedRenderFile = remember { PlatformUtils.userDataDirectory.resolve("last_render.png") }
    var cachedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isCacheValid by remember { mutableStateOf(false) }

    var showLive by remember { mutableStateOf(false) }
    LaunchedEffect(frame != null) {
        if (frame != null) {
            showLive = true
        } else {
            showLive = false
        }
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

    var hasSavedFrame by remember { mutableStateOf(false) }
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

    LaunchedEffect(Unit) {
        AccountManager.activeAccount?.let { account ->
            val skinPath = SkinManager.getSkinPath(account)
            if (skinPath != null) {
                Mc3DController.updatePlayerSkin(skinPath)
            }
        }

        Mc3DController.currentTarget = Mc3DController.RenderTarget.MAIN_MAP
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
        McDialog(
            onDismissRequest = { MinecraftManager.cleanError() },
            title = stringResource(Res.string.main_error_installation_title)
        ) {
            McText(downloadStatus.errorCode!!)
            Spacer(Modifier.size(16.dp))
            McButton(
                text = stringResource(Res.string.common_ok),
                onClick = { MinecraftManager.cleanError() }
            )
        }
    }

    if (minecraftStatus == MinecraftStatus.FAILED) {
        val scrollState = rememberScrollState()
        McDialog(
            onDismissRequest = {
                MinecraftManager.clearCrash()

                isCrashLogCopied = false
            },
            title = stringResource(Res.string.main_error_crash_title),
            buttons = {
                McButton(
                    text = if (isCrashLogCopied) stringResource(Res.string.main_error_crash_copied) else stringResource(Res.string.main_error_crash_copy),
                    icon = Res.drawable.ic_plus,
                    disabled = isCrashLogCopied,
                    onClick = {
                        coroutineScope.launch {
                            clipboard.setClipEntry((lastCrashLog ?: "Brak").toClipEntry())
                        }

                        isCrashLogCopied = true
                    }
                )

                McButton(
                    text = stringResource(Res.string.common_ok),
                    icon = Res.drawable.ic_check,
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
                    BasicText(
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

    if (consoleOpen) McDialog(
        onDismissRequest = {
            consoleOpen = false
        },
        title = stringResource(Res.string.main_console_title),
        buttons = {
            McButton(
                text = stringResource(Res.string.main_console_close),
                icon = Res.drawable.ic_close,
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
                BasicText(
                    text = combinedText,
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

    Column(
        modifier = Modifier.fillMaxSize().focusGroup()
    ) {
        Box (modifier = Modifier.weight(1f)) {
//            Image(
//                painter = painterResource(Res.drawable.BG),
//                contentDescription = null,
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Crop
//            )
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val width = with(density) { maxWidth.toPx() }.toInt()
                val height = with(density) { maxHeight.toPx() }.toInt()

                LaunchedEffect(width, height) {
                    Mc3DController.resize(width, height)
                }

                val liveAlpha by animateFloatAsState(
                    targetValue = if (showLive) 1f else 0f,
                    animationSpec = tween(500)
                )

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0.5f, 0.65f, 0.9f))
                        .focusRequester(mapFocusRequester)
                        .focusProperties {
                            down = playButtonFocusRequester
                        }
                        .focusable()
                ) {
                    if (isCacheValid && cachedBitmap != null && liveAlpha < 1f) {
                        Image(
                            bitmap = cachedBitmap!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (frame != null && liveAlpha > 0f) {
                        Image(
                            bitmap = frame!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().alpha(liveAlpha)
                        )
                    }
                }
            }
            Box(Modifier.padding(16.dp)) {
                Box (Modifier.background(Color.Black.copy(0.7f))) {
                    McText(
                        text = stringResource(
                            Res.string.main_welcome_user,
                            activeAccount?.username ?: stringResource(Res.string.main_welcome_default)
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                    )
                }
            }
            if (launchMoreOptionsExpanded) Popup(
                alignment = Alignment.BottomEnd,
                onDismissRequest = { launchMoreOptionsExpanded = false },
            ) {
                Column(Modifier.width(IntrinsicSize.Max)) {
                    quickPlay?.let { qpList ->
                        for (item in qpList) {
                            McListItem(
                                title = stringResource(
                                    Res.string.main_quickplay_join,
                                    when (item.type) {
                                        QuickPlayType.Singleplayer -> stringResource(Res.string.main_quickplay_singleplayer)
                                        QuickPlayType.Multiplayer -> stringResource(Res.string.main_quickplay_multiplayer)
                                        QuickPlayType.Realms -> stringResource(Res.string.main_quickplay_realms)
                                    }
                                ),
                                description = stringResource(
                                    Res.string.main_quickplay_info,
                                    item.name,
                                    when (item.gameMode) {
                                        GameMode.Survival -> stringResource(Res.string.main_gamemode_survival)
                                        GameMode.Creative -> stringResource(Res.string.main_gamemode_creative)
                                        GameMode.Adventure -> stringResource(Res.string.main_gamemode_adventure)
                                        GameMode.Spectator -> stringResource(Res.string.main_gamemode_spectator)
                                    },
                                    DateTimeFormatterUtil.formatDateTime(item.lastPlayedTime)
                                ),
                                onClick = {
                                    MinecraftManager.launchBackground(
                                        state.lastSelectedInstanceId!!,
                                        item
                                    )
                                    launchMoreOptionsExpanded = false
                                }
                            )
                        }
                    }
                    McListItem(
                        title = stringResource(Res.string.main_last_game_logs),
                        icon = Res.drawable.ic_console,
                        onClick = {
                            consoleOpen = true
                        }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.height(64.dp).focusGroup()
        ) {
            McButton(
                icon = Res.drawable.ic_settings,
                onClick = {
                    navController.navigate("settings")
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .focusProperties {
                        up = mapFocusRequester
                    }
            )
            McButton(
                icon = Res.drawable.ic_servers,
                onClick = {
                    navController.navigate("servers/list")
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .focusProperties {
                        up = mapFocusRequester
                    },
            )
            McListItem(
                title = stringResource(Res.string.main_nav_profile),
                description = activeAccount?.username,
                icon = Res.drawable.ic_profile,
                onClick = {
                    navController.navigate("profile/list")
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusProperties {
                        up = mapFocusRequester
                    },
                alignment = Alignment.Center
            )
            McListItem(
                title = stringResource(Res.string.main_nav_versions),
                description = state.lastSelectedInstanceString,
                icon = Res.drawable.ic_save,
                onClick = {
                    navController.navigate("versions/list")
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusProperties {
                        up = mapFocusRequester
                    },
                alignment = Alignment.Center
            )
            McButton(
                text = when (minecraftStatus) {
                    MinecraftStatus.LAUNCHING -> stringResource(Res.string.main_status_launching)
                    MinecraftStatus.LAUNCHED -> stringResource(Res.string.main_status_launched)
                    MinecraftStatus.KILLING -> stringResource(Res.string.main_status_killing)
                    else -> stringResource(Res.string.main_status_ready)
                },
                icon =
                    if (minecraftStatus == MinecraftStatus.LAUNCHED
                        || minecraftStatus == MinecraftStatus.KILLING) Res.drawable.ic_exit
                    else Res.drawable.ic_power,
                primary = minecraftStatus != MinecraftStatus.LAUNCHED,
                disabled = minecraftStatus == MinecraftStatus.LAUNCHING ||
                            minecraftStatus == MinecraftStatus.KILLING ||
                            state.lastSelectedInstanceId == null,
                onClick = {
                    if (minecraftStatus == MinecraftStatus.LAUNCHED) {
                        MinecraftManager.killGame()
                    } else {
                        if (state.lastSelectedInstanceId != null)
                            MinecraftManager.launchBackground(state.lastSelectedInstanceId!!)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(playButtonFocusRequester)
                    .focusProperties {
                        up = mapFocusRequester
                    }
            )
            McButton(
                icon = if (minecraftStatus == MinecraftStatus.LAUNCHED)
                    Res.drawable.ic_console
                    else Res.drawable.ic_chevron_down,
                primary = minecraftStatus != MinecraftStatus.LAUNCHED,
                disabled = minecraftStatus == MinecraftStatus.LAUNCHING ||
                        minecraftStatus == MinecraftStatus.KILLING,
                onClick = {
                    if (minecraftStatus == MinecraftStatus.LAUNCHED)
                        consoleOpen = true
                    else
                        launchMoreOptionsExpanded = true
                },
                modifier = Modifier
                    .fillMaxHeight()
                    .focusProperties {
                        up = mapFocusRequester
                    }
            )
        }
        if (downloadStatus.type != StatusType.IDLE) {
            val progressFraction = remember(downloadStatus.current, downloadStatus.total) {
                if (downloadStatus.total > 0) {
                    downloadStatus.current.toFloat() / downloadStatus.total
                } else {
                    0f
                }
            }

            val percentageText = "${(progressFraction * 100).toInt()}%"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(MinecraftTheme.colors.controlPrimaryBackground)
                            .border(1.dp, MinecraftTheme.colors.controlPrimaryBorder)
                    )
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    McText(
                        text = when (downloadStatus.type) {
                            StatusType.INFO_FETCH -> stringResource(Res.string.status_info_fetch)
                            StatusType.INFO_WRITE -> stringResource(Res.string.status_info_write)
                            StatusType.CLIENT_DL -> stringResource(Res.string.status_client_dl)
                            StatusType.LIBRARY_DL ->
                                stringResource(
                                    Res.string.status_library_dl,
                                    downloadStatus.current,
                                    downloadStatus.total,
                                    percentageText
                                )
                            StatusType.ASSET_FETCH -> stringResource(Res.string.status_asset_fetch)
                            StatusType.ASSET_DL ->
                                stringResource(
                                    Res.string.status_asset_dl,
                                    downloadStatus.current,
                                    downloadStatus.total,
                                    percentageText
                                )

                            StatusType.JAVA_FETCH -> stringResource(Res.string.status_java_fetch)
                            StatusType.JAVA_INSTALL ->
                                stringResource(
                                    Res.string.status_java_install,
                                    downloadStatus.current,
                                    downloadStatus.total,
                                    percentageText
                                )
                            StatusType.MODPACK_DL -> stringResource(Res.string.status_modpack_dl)
                            StatusType.EXTRACT -> stringResource(Res.string.status_extract)
                            else -> ""
                        }
                    )
                }
            }
        }
    }
}