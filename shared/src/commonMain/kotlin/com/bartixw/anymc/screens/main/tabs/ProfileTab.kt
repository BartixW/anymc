/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.main.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_back
import anymc.shared.generated.resources.common_no
import anymc.shared.generated.resources.common_none
import anymc.shared.generated.resources.common_ok
import anymc.shared.generated.resources.common_yes
import anymc.shared.generated.resources.ic_bin
import anymc.shared.generated.resources.ic_boots
import anymc.shared.generated.resources.ic_bricks
import anymc.shared.generated.resources.ic_check
import anymc.shared.generated.resources.ic_chevron_left
import anymc.shared.generated.resources.ic_clock
import anymc.shared.generated.resources.ic_close
import anymc.shared.generated.resources.ic_door_in_out
import anymc.shared.generated.resources.ic_globe
import anymc.shared.generated.resources.ic_image_unknown
import anymc.shared.generated.resources.ic_profile
import anymc.shared.generated.resources.ic_account_add_v2
import anymc.shared.generated.resources.ic_skull
import anymc.shared.generated.resources.ic_sprint
import anymc.shared.generated.resources.profile_add_account
import anymc.shared.generated.resources.profile_change_account
import anymc.shared.generated.resources.profile_delete_account_confirm
import anymc.shared.generated.resources.profile_delete_account_title
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
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.SkinManager
import com.bartixw.anymc.managers.StatsManager
import com.bartixw.anymc.mc3d.Mc3DController
import com.bartixw.anymc.minecraft.data.Account
import com.bartixw.anymc.ui.components.DrawerSide
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McDialog
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McSideDrawer
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.McTopAppBar
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.components.v2.TopAppBar
import com.bartixw.anymc.screens.v2.dialogs.NewAccountDialog
import com.bartixw.anymc.ui.theme.MinecraftTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.awt.Button
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ProfileTab(
    frame: ImageBitmap?
) {
    val accountListData by AccountManager.data.collectAsState()
    var isNewAccountDialogVisible by remember { mutableStateOf(false) }
    var isAccountListMenuVisible by remember { mutableStateOf(false) }
    var deleteAccountDialogUuid by remember { mutableStateOf("") }
    val stats by StatsManager.aggregatedStats.collectAsState()
    val isLoading by StatsManager.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        Mc3DController.currentTarget = Mc3DController.RenderTarget.PLAYER_PREVIEW
        StatsManager.refreshStats()
    }

    LaunchedEffect(accountListData.activeAccountUuid) {
        StatsManager.refreshStats()
        AccountManager.activeAccount?.let { account ->
            val skinPath = SkinManager.getSkinPath(account)
            if (skinPath != null) {
                Mc3DController.updatePlayerSkin(skinPath)
            }
        }

        withContext(Dispatchers.Default) {
            delay(100.milliseconds)
            Mc3DController.playOnceThen("wave", "breathe")
        }
    }

    Mc3DController.setupTheme()

    if (isNewAccountDialogVisible) NewAccountDialog(
        onDismissRequest = {
            isNewAccountDialogVisible = false
        }
    )

    if (deleteAccountDialogUuid.isNotEmpty()) Dialog(
        onDismissRequest = {
            deleteAccountDialogUuid = ""
        },
        title = { Text(stringResource(Res.string.profile_delete_account_title)) },
        buttons = {
            Button(
                content = { Text(stringResource(Res.string.common_no)) },
                onClick = {
                    deleteAccountDialogUuid = ""
                }
            )
            Button(
                content = { Text(stringResource(Res.string.common_yes)) },
                type = ButtonType.Destructive,
                onClick = {
                    AccountManager.removeAccount(deleteAccountDialogUuid)
                    deleteAccountDialogUuid = ""
                }
            )
        }
    ) {
        Text(stringResource(Res.string.profile_delete_account_confirm))
    }

    McSideDrawer(
        isOpen = isAccountListMenuVisible,
        onCloseRequest = { isAccountListMenuVisible = false },
        side = DrawerSide.Right,
        drawerWidth = 600.dp,
        drawerContent = {
            Column(Modifier.focusGroup()) {
                TopAppBar(
                    backAction = { isAccountListMenuVisible = false },
                    title = { Text(stringResource(Res.string.profile_change_account)) },
                    actions = {
                        Box(
                            modifier = Modifier
                                .clickable(onClick = {
                                    isNewAccountDialogVisible = true
                                })
                        ) {
                            Icon(
                                resource = Res.drawable.ic_account_add_v2,
                                contentDescription = stringResource(Res.string.common_back),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                )

                val listState = rememberLazyListState()
                Row(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        items(accountListData.accounts) { account ->
                            val isActive = accountListData.activeAccountUuid == account.uuid

                            Row(
                                Modifier.height(64.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                McListItem(
                                    title = account.username,
                                    description = when (account) {
                                        is Account.Microsoft -> if (account.isDemo) "Microsoft (Demo)" else "Microsoft"
                                        is Account.Offline -> "Offline"
                                        is Account.ElyBy -> "Ely.by"
                                    },
                                    icon = Res.drawable.ic_profile,
                                    onClick = { AccountManager.setActiveAccount(account.uuid) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    selected = isActive
                                )

                                McButton(
                                    text = null,
                                    icon = Res.drawable.ic_bin,
                                    onClick = { deleteAccountDialogUuid = account.uuid },
                                    modifier = Modifier.fillMaxHeight()
                                )
                            }
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(listState),
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.aspectRatio(3f / 8f)) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val width = with(density) { maxWidth.toPx() }.toInt()
                    val height = with(density) { maxHeight.toPx() }.toInt()

                    LaunchedEffect(width, height) {
                        Mc3DController.resize(width, height)
                    }

                    frame?.let { frame ->
                        Image(
                            bitmap = frame,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .focusable()
                                .onKeyEvent {
                                    when (it.key) {
                                        Key.A -> {
                                            Mc3DController.rotatePlayer(-5f, 0f)
                                            true
                                        }

                                        Key.D -> {
                                            Mc3DController.rotatePlayer(5f, 0f)
                                            true
                                        }

                                        else -> false
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        Mc3DController.rotatePlayer(-dragAmount.x, 0f)
                                    }
                                }
                        )
                    } ?: Box(Modifier.fillMaxSize()) {
                        LoadingIndicator(Modifier.align(Alignment.Center))
                    }
                }

                Box(Modifier.padding(24.dp).align(Alignment.TopCenter)) {
                    Box(Modifier.background(Color.Black.copy(0.7f))) {
                        Text(
                            text = AccountManager.activeAccount?.username ?: "Gracz",
                            modifier = Modifier
                                .padding(8.dp),
                            style = MinecraftTheme.typographyPixel
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(IntrinsicSize.Max)
                        .padding(bottom = 24.dp)
                ) {
                    Button(
                        content = { Text("Zmień skina") },
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {}
                    )
                    Button(
                        content = { Text("Zmień konto") },
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            isAccountListMenuVisible = true
                        }
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(stringResource(Res.string.profile_stats_title))
                Spacer(Modifier.size(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isLoading) {
                        LoadingIndicator(Modifier.align(Alignment.Center))
                    } else {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .border(1.dp, MinecraftTheme.colors.controlBorder),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                McListItem(
                                    title = stringResource(Res.string.profile_stats_total_playtime),
                                    icon = Res.drawable.ic_clock,
                                    modifier = Modifier.weight(1f).height(96.dp),
                                    description = formatPlayTime(stats.totalPlayTimeSeconds),
                                    bigDescriptionMode = true
                                )
                                McListItem(
                                    title = stringResource(Res.string.profile_stats_longest_world),
                                    icon = Res.drawable.ic_globe,
                                    modifier = Modifier.weight(1f).height(96.dp),
                                    description = stats.longestPlayedWorldName.let {
                                        if (it == "Brak") stringResource(
                                            Res.string.common_none
                                        ) else it
                                    },
                                    bigDescriptionMode = true
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .border(1.dp, MinecraftTheme.colors.controlBorder),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                McListItem(
                                    title = stringResource(Res.string.profile_stats_most_collected),
                                    icon = Res.drawable.ic_bricks,
                                    modifier = Modifier.weight(1f).height(96.dp),
                                    description = stats.mostCollectedBlockName.let {
                                        if (it == "Brak") stringResource(
                                            Res.string.common_none
                                        ) else it
                                    },
                                    bigDescriptionMode = true
                                )
                                McListItem(
                                    title = stringResource(Res.string.profile_stats_total_deaths),
                                    icon = Res.drawable.ic_skull,
                                    modifier = Modifier.weight(1f).height(96.dp),
                                    description = stats.totalDeaths.toString(),
                                    bigDescriptionMode = true
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .border(1.dp, MinecraftTheme.colors.controlBorder),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                McListItem(
                                    title = stringResource(Res.string.profile_stats_total_distance),
                                    icon = Res.drawable.ic_boots,
                                    modifier = Modifier.weight(1f).height(96.dp),
                                    description = stats.totalDistanceBlocks.toString(),
                                    bigDescriptionMode = true
                                )
                                McListItem(
                                    title = stringResource(Res.string.profile_stats_total_sessions),
                                    icon = Res.drawable.ic_door_in_out,
                                    modifier = Modifier.weight(1f).height(96.dp),
                                    description = stats.totalSessions.toString(),
                                    bigDescriptionMode = true
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .border(1.dp, MinecraftTheme.colors.controlBorder),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                McListItem(
                                    title = stringResource(Res.string.profile_stats_crouch_distance),
                                    icon = Res.drawable.ic_image_unknown,
                                    modifier = Modifier.weight(1f).height(96.dp),
                                    description = stats.crouchDistanceBlocks.toString(),
                                    bigDescriptionMode = true
                                )
                                McListItem(
                                    title = stringResource(Res.string.profile_stats_sprint_distance),
                                    icon = Res.drawable.ic_sprint,
                                    modifier = Modifier.weight(1f).height(96.dp),
                                    description = stats.sprintDistanceBlocks.toString(),
                                    bigDescriptionMode = true
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(Res.string.profile_stats_info),
                    style = TextStyle(
                        color = MinecraftTheme.colors.textSecondary,
                        fontSize = 12.sp
                    ),
                )
            }
        }
    }
}

@Composable
private fun formatPlayTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(Res.string.profile_stats_time_format_hm, hours, minutes)
        hours > 0 -> stringResource(Res.string.profile_stats_time_format_h, hours)
        minutes > 0 -> stringResource(Res.string.profile_stats_time_format_m, minutes)
        else -> stringResource(Res.string.profile_stats_time_format_s, seconds)
    }
}
