/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.main.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_error
import anymc.shared.generated.resources.common_ok
import anymc.shared.generated.resources.ic_chevron_up
import anymc.shared.generated.resources.ic_console
import anymc.shared.generated.resources.ic_more_options
import anymc.shared.generated.resources.ic_warning
import anymc.shared.generated.resources.main_gamemode_adventure
import anymc.shared.generated.resources.main_gamemode_creative
import anymc.shared.generated.resources.main_gamemode_spectator
import anymc.shared.generated.resources.main_gamemode_survival
import anymc.shared.generated.resources.main_last_game_logs
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
import anymc.shared.generated.resources.nbt_cancel
import anymc.shared.generated.resources.notice
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
import anymc.shared.generated.resources.versions_error_deletion_failed
import anymc.shared.generated.resources.versions_error_deletion_failed_desc
import anymc.shared.generated.resources.versions_error_empty_name
import anymc.shared.generated.resources.versions_menu_delete
import anymc.shared.generated.resources.versions_menu_export
import anymc.shared.generated.resources.versions_menu_nbt
import anymc.shared.generated.resources.versions_menu_open_folder
import anymc.shared.generated.resources.versions_menu_rename
import anymc.shared.generated.resources.versions_menu_restore_backup
import anymc.shared.generated.resources.versions_menu_settings
import anymc.shared.generated.resources.versions_new_installation
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.dataclasses.LauncherConfig
import com.bartixw.anymc.dataclasses.LauncherState
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.managers.SkinManager
import com.bartixw.anymc.mc3d.Mc3DController
import com.bartixw.anymc.minecraft.data.Account
import com.bartixw.anymc.minecraft.data.DownloadStatus
import com.bartixw.anymc.minecraft.data.GameMode
import com.bartixw.anymc.minecraft.data.JavaVersion
import com.bartixw.anymc.minecraft.data.MinecraftStatus
import com.bartixw.anymc.minecraft.data.QuickPlayType
import com.bartixw.anymc.minecraft.data.StatusType
import com.bartixw.anymc.screens.v2.dialogs.InstallationSettingsDialog
import com.bartixw.anymc.screens.v2.dialogs.NewInstallationDialog
import com.bartixw.anymc.screens.versions.VersionsViewModel
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Dropdown
import com.bartixw.anymc.ui.components.v2.DropdownItem
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.Input
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.DateTimeFormatterUtil
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeTab(
    activeAccount: Account?,
    isCacheValid: Boolean,
    cachedBitmap: ImageBitmap?,
    showLive: Boolean,
    frame: ImageBitmap?,
    downloadStatus: DownloadStatus,
    minecraftStatus: MinecraftStatus,
    state: LauncherState,
    viewModel: VersionsViewModel,
    navController: NavController,
    quickPlayFlyoutVisible: Boolean,
    onQuickPlayFlyoutToggle: (Boolean) -> Unit,
    onShowConsole: () -> Unit,
    config: LauncherConfig
) {
    var newInstallationDialogVisible by remember { mutableStateOf(false) }
    var settingsInstallationId by remember { mutableStateOf<String?>(null) }
    var settingsJvmArgs by remember { mutableStateOf<List<String>>(emptyList()) }
    var settingsRemovedArgs by remember { mutableStateOf<List<String>>(emptyList()) }
    var settingsJavaVersion by remember { mutableStateOf<JavaVersion?>(null) }

    LaunchedEffect(settingsInstallationId) {
        val id = settingsInstallationId
        if (id != null) {
            viewModel.getInstallationData(id) { data ->
                settingsJvmArgs = data?.jvmArgs ?: emptyList()
                settingsRemovedArgs = data?.removedArgs ?: emptyList()
                settingsJavaVersion = data?.javaVersion
            }
        }
    }

    if (settingsInstallationId != null) {
        InstallationSettingsDialog(
            initialJvmArgs = settingsJvmArgs,
            initialRemovedArgs = settingsRemovedArgs,
            initialJavaVersion = settingsJavaVersion,
            onDismissRequest = { settingsInstallationId = null },
            onSave = { jvmArgs, removedArgs, javaVersion ->
                viewModel.updateInstallationSettings(
                    settingsInstallationId!!,
                    jvmArgs,
                    removedArgs,
                    javaVersion
                ) {
                    settingsInstallationId = null
                }
            },
            viewModel = viewModel
        )
    }

    val quickPlay =
        if (state.lastSelectedInstanceId != null)
            remember(state.lastSelectedInstanceId, minecraftStatus == MinecraftStatus.READY) {
                MinecraftManager.getQuickPlayData(state.lastSelectedInstanceId)
            }
        else null

    if (newInstallationDialogVisible) NewInstallationDialog(
        navController = navController,
        onDismissRequest = { newInstallationDialogVisible = false })

    LaunchedEffect(Unit) {
        AccountManager.activeAccount?.let { account ->
            val skinPath = SkinManager.getSkinPath(account)
            if (skinPath != null) {
                Mc3DController.updatePlayerSkin(skinPath)
            }
        }

        Mc3DController.currentTarget = Mc3DController.RenderTarget.MAIN_MAP
    }

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = 64.dp)) {
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

            if (liveAlpha != 1f && !isCacheValid) {
                LoadingIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (isCacheValid && cachedBitmap != null && liveAlpha < 1f) {
                Image(
                    bitmap = cachedBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (frame != null && liveAlpha > 0f) {
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(liveAlpha)
                )
            }
        }
        Box(Modifier.padding(16.dp)) {
            Box(Modifier.background(Color.Black.copy(0.7f))) {
                Text(
                    text = stringResource(
                        Res.string.main_welcome_user,
                        activeAccount?.username ?: stringResource(Res.string.main_welcome_default)
                    ),
                    style = MinecraftTheme.typographyPixel,
                    modifier = Modifier.padding(8.dp)
                )
            }
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
                Modifier
                    .offset(y = (-64).dp)
                    .padding(8.dp)
                    .height(IntrinsicSize.Min)
                    .width(300.dp)
                    .border(2.dp, MinecraftTheme.colors.dialogBorder)
                    .background(MinecraftTheme.colors.dialogBackground)
                    .align(Alignment.BottomEnd)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .background(MinecraftTheme.colors.genericPrimary)
                )
                Text(
                    when (downloadStatus.type) {
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
                    },
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    style = MinecraftTheme.typographyV2.merge(fontSize = 14.sp)
                )
            }
        }
        
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-42).dp)
        ) {
            Button(
                content = {
                    Text(
                        when (minecraftStatus) {
                            MinecraftStatus.LAUNCHING -> stringResource(Res.string.main_status_launching)
                            MinecraftStatus.LAUNCHED -> stringResource(Res.string.main_status_launched)
                            MinecraftStatus.KILLING -> stringResource(Res.string.main_status_killing)
                            else -> stringResource(Res.string.main_status_ready).uppercase()
                        }
                    )
                },
                type = when (minecraftStatus) {
                    MinecraftStatus.READY, MinecraftStatus.FAILED -> ButtonType.Hero
                    MinecraftStatus.LAUNCHED -> ButtonType.Destructive
                    else -> ButtonType.Secondary
                },
                disabled = minecraftStatus == MinecraftStatus.LAUNCHING ||
                        minecraftStatus == MinecraftStatus.KILLING ||
                        state.lastSelectedInstanceId == null,
                onClick = {
                    if (minecraftStatus == MinecraftStatus.LAUNCHED) {
                        MinecraftManager.killGame()
                    } else {
                        state.lastSelectedInstanceId?.let {
                            MinecraftManager.launchBackground(it)
                        }
                    }
                },
                modifier = Modifier.width(256.dp)
            )

            var buttonSize by remember { mutableStateOf(IntSize.Zero) }
            Box(Modifier.onGloballyPositioned { buttonSize = it.size }) {
                Button(
                    content = { Icon(Res.drawable.ic_chevron_up, contentDescription = null) },
                    type = when (minecraftStatus) {
                        MinecraftStatus.READY, MinecraftStatus.FAILED -> ButtonType.Primary
                        MinecraftStatus.LAUNCHED -> ButtonType.Destructive
                        else -> ButtonType.Secondary
                    },
                    onClick = { onQuickPlayFlyoutToggle(!quickPlayFlyoutVisible) },
                    modifier = Modifier.width(IntrinsicSize.Min)
                )

                if (quickPlayFlyoutVisible) Popup(
                    alignment = Alignment.BottomEnd,
                    offset = IntOffset(0, -buttonSize.height),
                    onDismissRequest = { onQuickPlayFlyoutToggle(false) },
                ) {
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .padding(bottom = 8.dp)
                            .background(MinecraftTheme.colors.dropdownBackground)
                            .border(2.dp, MinecraftTheme.colors.dropdownBorder)
                            .padding(2.dp)
                    ) {
                        Column {
                            quickPlay?.let { qpList ->
                                for (item in qpList) {
                                    DropdownItem(
                                        isSelected = false,
                                        onClick = {
                                            MinecraftManager.launchBackground(
                                                state.lastSelectedInstanceId!!,
                                                item
                                            )
                                            onQuickPlayFlyoutToggle(false)
                                        },
                                        content = {
                                            Column {
                                                Text(
                                                    text = stringResource(
                                                        Res.string.main_quickplay_join,
                                                        when (item.type) {
                                                            QuickPlayType.Singleplayer -> stringResource(Res.string.main_quickplay_singleplayer)
                                                            QuickPlayType.Multiplayer -> stringResource(Res.string.main_quickplay_multiplayer)
                                                            QuickPlayType.Realms -> stringResource(Res.string.main_quickplay_realms)
                                                        }
                                                    )
                                                )
                                                Text(
                                                    text = stringResource(
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
                                                    style = TextStyle(
                                                        fontSize = 12.sp,
                                                        color = MinecraftTheme.colors.textSecondary
                                                    )
                                                )
                                            }
                                        },
                                        actions = {}
                                    )
                                }
                            }
                            DropdownItem(
                                isSelected = false,
                                onClick = {
                                    onShowConsole()
                                    onQuickPlayFlyoutToggle(false)
                                },
                                content = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Res.drawable.ic_console,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(Res.string.main_last_game_logs))
                                    }
                                },
                                actions = {}
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when (val uiState = viewModel.uiState) {
                is VersionsViewModel.UiState.Loading -> LoadingIndicator()
                is VersionsViewModel.UiState.Error -> {
                    Row {
                        Icon(Res.drawable.ic_warning, contentDescription = null)
                        Text(stringResource(Res.string.common_error))
                    }
                }
                is VersionsViewModel.UiState.Loaded -> {
                    val installations = uiState.installations.map { it.id }.toMutableList()
                    installations.add("-1")

                    val newInstallationString = stringResource(Res.string.versions_new_installation)

                    Dropdown(
                        options = installations,
                        selectedOption = viewModel.selectedInstallationId,
                        onOptionSelected = { id ->
                            if (id != "-1" && id != null) {
                                uiState.installations.find { it.id == id }?.let {
                                    viewModel.selectInstallation(id, it.name)
                                }
                            } else {
                                newInstallationDialogVisible = true
                            }
                        },
                        labelProvider = { id ->
                            if (id != "-1" && id != null) {
                                uiState.installations.find { it.id == id }?.name ?: ""
                            } else {
                                newInstallationString
                            }
                        },
                        descriptionProvider = { id ->
                            if (id != "-1" && id != null) {
                                uiState.installations.find { it.id == id }?.let { installation ->
                                    "${installation.modLoader.replaceFirstChar { it.uppercase() }} ${installation.version}"
                                }
                            } else null
                        },
                        itemActions = { id ->
                            if (id == "-1" || id == null) return@Dropdown

                            var showPopup by remember { mutableStateOf(false) }

                            Box(Modifier.clickable { showPopup = true }) {
                                Icon(
                                    resource = Res.drawable.ic_more_options,
                                    contentDescription = "Options",
                                    modifier = Modifier.size(24.dp).padding(4.dp)
                                )
                            }

                            if (showPopup) Popup(
                                alignment = Alignment.TopStart,
                                offset = IntOffset(0, 0),
                                onDismissRequest = { showPopup = false }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(300.dp)
                                        .background(MinecraftTheme.colors.dropdownBackground)
                                        .border(2.dp, MinecraftTheme.colors.dropdownBorder)
                                        .padding(2.dp)
                                ) {
                                    Column {
                                        if (MinecraftManager.hasBackup(id)) {
                                            DropdownItem(
                                                isSelected = false,
                                                onClick = {
                                                    showPopup = false
                                                    viewModel.restoreBackup(id)
                                                },
                                                content = {
                                                    Text(stringResource(Res.string.versions_menu_restore_backup))
                                                },
                                                actions = {}
                                            )
                                        }
                                        var showRenameDialog by remember { mutableStateOf(false) }
                                        DropdownItem(
                                            isSelected = false,
                                            onClick = {
                                                showRenameDialog = true
                                            },
                                            content = {
                                                Text(stringResource(Res.string.versions_menu_rename))
                                            },
                                            actions = {
                                                if (showRenameDialog) {
                                                    var newName by remember { mutableStateOf(uiState.installations.find { it.id == id }?.name ?: "") }
                                                    var isLoading by remember { mutableStateOf(false) }
                                                    var showEmptyNameError by remember { mutableStateOf(false) }

                                                    LaunchedEffect(newName) {
                                                        showEmptyNameError = false
                                                    }

                                                    Dialog(
                                                        onDismissRequest = {
                                                            showRenameDialog = false
                                                            showPopup = false
                                                        },
                                                        title = { Text(stringResource(Res.string.versions_dialog_rename_title)) },
                                                        buttons = {
                                                            Button(
                                                                content = { Text(stringResource(Res.string.nbt_cancel)) },
                                                                onClick = {
                                                                    showRenameDialog = false
                                                                    showPopup = false
                                                                }
                                                            )
                                                            Button(
                                                                content = {
                                                                    if (isLoading) LoadingIndicator(Modifier.size(16.dp)) else Text(
                                                                        stringResource(Res.string.common_ok)
                                                                    )
                                                                },
                                                                disabled = isLoading,
                                                                type = ButtonType.Primary,
                                                                onClick = {
                                                                    isLoading = true
                                                                    if (newName.isEmpty()) {
                                                                        showEmptyNameError = true
                                                                        isLoading = false
                                                                        return@Button
                                                                    }
                                                                    viewModel.renameInstallation(
                                                                        id,
                                                                        newName
                                                                    ) {
                                                                        showRenameDialog = false
                                                                        showPopup = false
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    ) {
                                                        Text(stringResource(Res.string.versions_dialog_rename_label))
                                                        Input(
                                                            value = newName,
                                                            onValueChange = { newName = it },
                                                            disabled = isLoading,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                        if (showEmptyNameError) Text(
                                                            text = stringResource(Res.string.versions_error_empty_name),
                                                            style = TextStyle(color = MinecraftTheme.colors.genericDestructive)
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                        DropdownItem(
                                            isSelected = false,
                                            onClick = {
                                                showPopup = false
                                                settingsInstallationId = id
                                            },
                                            content = {
                                                Text(stringResource(Res.string.versions_menu_settings))
                                            },
                                            actions = {}
                                        )
                                        if (config.devMode) DropdownItem(
                                            isSelected = false,
                                            onClick = {
                                                navController.navigate(com.bartixw.anymc.screens.nbt.NbtEditorRoute(id))
                                                showPopup = false
                                            },
                                            content = {
                                                Text(stringResource(Res.string.versions_menu_nbt))
                                            },
                                            actions = {}
                                        )
                                        DropdownItem(
                                            isSelected = false,
                                            onClick = {
                                                showPopup = false
                                                viewModel.exportInstallation(id)
                                            },
                                            content = {
                                                Text(stringResource(Res.string.versions_menu_export))
                                            },
                                            actions = {}
                                        )
                                        DropdownItem(
                                            isSelected = false,
                                            onClick = {
                                                showPopup = false

                                                val installationDir =
                                                    MinecraftManager.getDirectory(id)
                                                val osOpenCmd =
                                                    when (PlatformUtils.getCurrentOS()) {
                                                        "windows" -> "explorer"
                                                        "osx" -> "open"
                                                        else -> "xdg-open"
                                                    }
                                                ProcessBuilder(
                                                    osOpenCmd,
                                                    installationDir.absolutePath
                                                ).start()
                                            },
                                            content = {
                                                Text(stringResource(Res.string.versions_menu_open_folder))
                                            },
                                            actions = {}
                                        )
                                        var deletionFailed by remember { mutableStateOf(false) }
                                        DropdownItem(
                                            isSelected = false,
                                            onClick = {
                                                showPopup = false
                                                viewModel.deleteInstallation(
                                                    id = id,
                                                    onDeleted = { },
                                                    onError = {
                                                        deletionFailed = true
                                                    }
                                                )
                                            },
                                            content = {
                                                Text(
                                                    text = stringResource(Res.string.versions_menu_delete),
                                                    style = MinecraftTheme.typographyV2.copy(color = MinecraftTheme.colors.genericDestructive)
                                                )
                                            },
                                            actions = {
                                                if (deletionFailed) Dialog(
                                                    onDismissRequest = {
                                                        deletionFailed = false
                                                    },
                                                    title = { Text(stringResource(Res.string.versions_error_deletion_failed)) },
                                                    buttons = {
                                                        Button(
                                                            content = { Text(stringResource(Res.string.common_ok)) },
                                                            type = ButtonType.Primary,
                                                            onClick = { deletionFailed = false }
                                                        )
                                                    }
                                                ) {
                                                    Text(stringResource(Res.string.versions_error_deletion_failed_desc))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.widthIn(max = 340.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(Res.string.notice),
                style = TextStyle(
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.End
                ),
                modifier = Modifier.widthIn(max = 250.dp)
            )
        }
    }
}
