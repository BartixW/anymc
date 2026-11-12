/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_no
import anymc.shared.generated.resources.common_none
import anymc.shared.generated.resources.common_yes
import anymc.shared.generated.resources.ic_bin
import anymc.shared.generated.resources.ic_boots
import anymc.shared.generated.resources.ic_bricks
import anymc.shared.generated.resources.ic_check
import anymc.shared.generated.resources.ic_clock
import anymc.shared.generated.resources.ic_close
import anymc.shared.generated.resources.ic_door_in_out
import anymc.shared.generated.resources.ic_edit
import anymc.shared.generated.resources.ic_globe
import anymc.shared.generated.resources.ic_image_unknown
import anymc.shared.generated.resources.ic_profile
import anymc.shared.generated.resources.ic_profile_switch
import anymc.shared.generated.resources.ic_profile_add
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
import anymc.shared.generated.resources.profile_title
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.managers.SkinManager
import com.bartixw.anymc.managers.StatsManager
import com.bartixw.anymc.mc3d.Mc3DController
import com.bartixw.anymc.minecraft.data.Account
import com.bartixw.anymc.popups.NewAccountPopup
import com.bartixw.anymc.ui.components.DrawerSide
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McDialog
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McLoadingIndicator
import com.bartixw.anymc.ui.components.McSideDrawer
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.McTopAppBar
import com.bartixw.anymc.ui.theme.MinecraftTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ProfileListScreen(navController: NavController) {
    val frame by Mc3DController.activeFrame.collectAsState()
    val accountListData by AccountManager.data.collectAsState()
    val stats by StatsManager.aggregatedStats.collectAsState()
    val isLoading by StatsManager.isLoading.collectAsState()
    var isMenuOpen by remember { mutableStateOf(false) }
    var deleteAccountDialogUuid by remember { mutableStateOf("") }
    var showNewAccountPopup by remember { mutableStateOf(false) }

    val previewFocusRequester = remember { FocusRequester() }
    val statsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        Mc3DController.currentTarget = Mc3DController.RenderTarget.PLAYER_PREVIEW
        StatsManager.refreshStats()
        if (ConfigManager.data.value.controllerSupport) {
            previewFocusRequester.requestFocus()
        }
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

    NewAccountPopup(
        visible = showNewAccountPopup,
        onDismissRequest = { showNewAccountPopup = false }
    )

    if (deleteAccountDialogUuid.isNotEmpty()) McDialog(
        onDismissRequest = {
            deleteAccountDialogUuid = ""
        },
        title = stringResource(Res.string.profile_delete_account_title),
        buttons = {
            McButton(
                text = stringResource(Res.string.common_no),
                icon = Res.drawable.ic_close,
                onClick = {
                    deleteAccountDialogUuid = ""
                }
            )
            McButton(
                text = stringResource(Res.string.common_yes),
                icon = Res.drawable.ic_check,
                onClick = {
                    AccountManager.removeAccount(deleteAccountDialogUuid)
                    deleteAccountDialogUuid = ""
                }
            )
        }
    ) {
        McText(stringResource(Res.string.profile_delete_account_confirm))
    }

    McSideDrawer(
        isOpen = isMenuOpen,
        onCloseRequest = { isMenuOpen = false },
        side = DrawerSide.Right,
        drawerWidth = 600.dp,
        drawerContent = {
            Column(Modifier.focusGroup()) {
                McTopAppBar(
                    backAction = { isMenuOpen = false },
                    title = stringResource(Res.string.profile_change_account),
                    actions = listOf({
                        McButton(
                            text = stringResource(Res.string.profile_add_account),
                            icon = Res.drawable.ic_profile_add,
                            onClick = {
                                showNewAccountPopup = true
                            }
                        )
                    })
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
        Column(Modifier.fillMaxSize().focusGroup()) {
            McTopAppBar(
                navController = navController,
                title = stringResource(Res.string.profile_title),
                actions = listOf(
                    {
                        McButton(
                            text = stringResource(Res.string.profile_change_account),
                            icon = Res.drawable.ic_profile_switch,
                            onClick = {
                                isMenuOpen = true
                            }
                        )
                    }
                ),
            )
            Row(Modifier.fillMaxSize()) {
                Box (Modifier.aspectRatio(3f / 8f)) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val density = LocalDensity.current
                        val width = with(density) { maxWidth.toPx() }.toInt()
                        val height = with(density) { maxHeight.toPx() }.toInt()

                        LaunchedEffect(width, height) {
                            Mc3DController.resize(width, height)
                        }

                        frame?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .focusRequester(previewFocusRequester)
                                    .focusProperties {
                                        right = statsFocusRequester
                                    }
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
                            McLoadingIndicator(Modifier.align(Alignment.Center))
                        }
                    }

                    Box(Modifier.padding(24.dp).align(Alignment.TopCenter)) {
                        Box (Modifier.background(Color.Black.copy(0.7f))) {
                            McText(
                                text = AccountManager.activeAccount?.username ?: "Player",
                                modifier = Modifier
                                    .padding(8.dp)
                            )
                        }
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .focusRequester(statsFocusRequester)
                        .focusProperties {
                            left = previewFocusRequester
                        }
                ) {
                    McText(stringResource(Res.string.profile_stats_title))
                    Spacer(Modifier.size(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (isLoading) {
                            McLoadingIndicator(Modifier.align(Alignment.Center))
                        } else {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MinecraftTheme.colors.controlBorder),
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
                                        description = stats.longestPlayedWorldName.let { if (it == "Brak") stringResource(Res.string.common_none) else it },
                                        bigDescriptionMode = true
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MinecraftTheme.colors.controlBorder),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    McListItem(
                                        title = stringResource(Res.string.profile_stats_most_collected),
                                        icon = Res.drawable.ic_bricks,
                                        modifier = Modifier.weight(1f).height(96.dp),
                                        description = stats.mostCollectedBlockName.let { if (it == "Brak") stringResource(Res.string.common_none) else it },
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
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MinecraftTheme.colors.controlBorder),
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
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MinecraftTheme.colors.controlBorder),
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
                    McText(
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
