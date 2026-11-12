/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.main.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_close
import anymc.shared.generated.resources.common_error
import anymc.shared.generated.resources.common_play
import anymc.shared.generated.resources.ic_clock
import anymc.shared.generated.resources.ic_heart
import anymc.shared.generated.resources.ic_hourglass
import anymc.shared.generated.resources.ic_skull
import anymc.shared.generated.resources.ic_zip_plus
import anymc.shared.generated.resources.profile_stats_time_format_h
import anymc.shared.generated.resources.profile_stats_time_format_hm
import anymc.shared.generated.resources.profile_stats_time_format_m
import anymc.shared.generated.resources.profile_stats_time_format_s
import anymc.shared.generated.resources.profile_stats_total_deaths
import anymc.shared.generated.resources.stat_cat_broken
import anymc.shared.generated.resources.stat_cat_crafted
import anymc.shared.generated.resources.stat_cat_custom
import anymc.shared.generated.resources.stat_cat_dropped
import anymc.shared.generated.resources.stat_cat_killed
import anymc.shared.generated.resources.stat_cat_killed_by
import anymc.shared.generated.resources.stat_cat_mined
import anymc.shared.generated.resources.stat_cat_picked_up
import anymc.shared.generated.resources.stat_cat_used
import anymc.shared.generated.resources.stat_value_hp
import anymc.shared.generated.resources.stat_value_km
import anymc.shared.generated.resources.stat_value_m
import anymc.shared.generated.resources.versions_add_zip
import anymc.shared.generated.resources.versions_empty_worlds
import anymc.shared.generated.resources.versions_gamemode_adventure
import anymc.shared.generated.resources.versions_gamemode_creative
import anymc.shared.generated.resources.versions_gamemode_spectator
import anymc.shared.generated.resources.versions_gamemode_survival
import anymc.shared.generated.resources.versions_gamemode_unknown
import anymc.shared.generated.resources.versions_page_stats
import anymc.shared.generated.resources.versions_stats_empty
import anymc.shared.generated.resources.versions_world_deaths_label
import anymc.shared.generated.resources.versions_world_gamemode_label
import anymc.shared.generated.resources.versions_world_last_played_label
import anymc.shared.generated.resources.versions_world_play_time_label
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.managers.DetailedWorldStats
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.managers.TranslationManager
import com.bartixw.anymc.minecraft.data.GameMode
import com.bartixw.anymc.minecraft.data.MinecraftStatus
import anymc.shared.generated.resources.worlds_count
import anymc.shared.generated.resources.worlds_select_installation
import com.bartixw.anymc.minecraft.data.QuickPlay
import com.bartixw.anymc.minecraft.data.QuickPlayType
import com.bartixw.anymc.screens.versions.VersionsViewModel
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McText
//import com.bartixw.anymc.screens.versions.WorldListItem
import com.bartixw.anymc.screens.versions.rememberImageBitmap
import com.bartixw.anymc.ui.components.McLoadingIndicator
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.DateTimeFormatterUtil
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun WorldsTab(
    viewModel: VersionsViewModel,
    navController: NavController,
    minecraftStatus: MinecraftStatus
) {
    val config by ConfigManager.data.collectAsState()
    val worldsListState = rememberLazyListState()
    var statsWorldId by remember { mutableStateOf("") }

    if (statsWorldId.isNotEmpty()) WorldStatsDialog(
        state = viewModel.statsState,
        worldId = statsWorldId,
        onDismissRequest = { statsWorldId = "" }
    )

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (viewModel.selectedInstallationId == null) {
            Text(
                text = stringResource(Res.string.worlds_select_installation),
                style = MinecraftTheme.typographyV2
            )
            return@Row
        }

        when (val worldsState = viewModel.worldsState) {
            is VersionsViewModel.WorldsState.Loading -> Box(Modifier.fillMaxSize()) {
                LoadingIndicator(Modifier.align(Alignment.Center))
            }

            VersionsViewModel.WorldsState.Empty -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(Res.string.versions_empty_worlds))
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(Res.string.worlds_count, 0),
                                style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                            )
                            Button(
                                content = { Text(stringResource(Res.string.versions_add_zip)) },
                                onClick = { viewModel.loadWorldFromZip(viewModel.selectedInstallationId!!) }
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(worldsListState),
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }

            is VersionsViewModel.WorldsState.Error -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(
                        Res.string.common_error,
                        worldsState.message ?: ""
                    )
                )
            }

            is VersionsViewModel.WorldsState.Loaded -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 350.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = worldsState.worlds,
                                key = { it.id }
                            ) { worldItem ->
                                val stats = when (val state = viewModel.statsState) {
                                    is VersionsViewModel.StatsState.Loaded ->
                                        state.stats.find { it.worldId == worldItem.id }
                                    else -> null
                                }

                                WorldListItem(
                                    worldItem = worldItem,
                                    stats = stats,
                                    iconPath = MinecraftManager.getDirectory(viewModel.selectedInstallationId!!)
                                        .resolve("gameFiles")
                                        .resolve("saves")
                                        .resolve(worldItem.id)
                                        .resolve("icon.png")
                                        .absolutePath,
                                    minecraftStatus = minecraftStatus,
                                    showIds = config.devMode,
                                    onLaunch = {
                                        navController.navigate("main") {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        MinecraftManager.launchBackground(
                                            viewModel.selectedInstallationId!!,
                                            QuickPlay(
                                                type = QuickPlayType.Singleplayer,
                                                id = worldItem.id,
                                                name = "",
                                                lastPlayedTime = LocalDateTime.MIN,
                                                gameMode = GameMode.Survival
                                            )
                                        )
                                    },
                                    onStatsShow = {
                                        statsWorldId = worldItem.id
                                    }
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(Res.string.worlds_count, worldsState.worlds.size),
                                style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                            )
                            Button(
                                content = { Text(stringResource(Res.string.versions_add_zip)) },
                                onClick = { viewModel.loadWorldFromZip(viewModel.selectedInstallationId!!) }
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(worldsListState),
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun WorldListItem(
    worldItem: VersionsViewModel.WorldItem,
    stats: DetailedWorldStats?,
    iconPath: String,
    minecraftStatus: MinecraftStatus,
    showIds: Boolean = false,
    onLaunch: () -> Unit,
    onStatsShow: () -> Unit
) {
    val gameModeStr = when (worldItem.gameType) {
        0 -> stringResource(Res.string.versions_gamemode_survival)
        1 -> stringResource(Res.string.versions_gamemode_creative)
        2 -> stringResource(Res.string.versions_gamemode_adventure)
        3 -> stringResource(Res.string.versions_gamemode_spectator)
        else -> stringResource(Res.string.versions_gamemode_unknown)
    }
    val bitmap = rememberImageBitmap(iconPath)

    val lastPlayedStr = DateTimeFormatterUtil.formatDateTime(
        Instant.ofEpochMilli(worldItem.lastPlayed)
            .atZone(ZoneId.systemDefault())
    )

    val deathsStr = stats?.deaths?.toString()

    val playtimeStr = if (stats != null) formatPlayTime(stats.playTimeSeconds) else null

    Column(
        Modifier
            .height(IntrinsicSize.Max)
            .border(2.dp, MinecraftTheme.colors.dialogBorder)
            .background(MinecraftTheme.colors.dialogBackground)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (bitmap != null) Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = worldItem.name,
                style = MinecraftTheme.typographyPixel
            )
        }
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Res.drawable.ic_heart, null, tint = MinecraftTheme.colors.textSecondary)
                    Text(stringResource(Res.string.versions_world_gamemode_label, gameModeStr), style = TextStyle(color = MinecraftTheme.colors.textSecondary))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Res.drawable.ic_clock, null, tint = MinecraftTheme.colors.textSecondary)
                    Text(stringResource(Res.string.versions_world_last_played_label, if (stats != null) "\n" + lastPlayedStr else lastPlayedStr), style = TextStyle(color = MinecraftTheme.colors.textSecondary))
                }
            }
            if (stats != null) Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Res.drawable.ic_hourglass, null, tint = MinecraftTheme.colors.textSecondary)
                    Text(stringResource(Res.string.versions_world_play_time_label, playtimeStr!!), style = TextStyle(color = MinecraftTheme.colors.textSecondary))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Res.drawable.ic_skull, null, tint = MinecraftTheme.colors.textSecondary)
                    Text(stringResource(Res.string.versions_world_deaths_label, deathsStr!!), style = TextStyle(color = MinecraftTheme.colors.textSecondary))
                }
            }
        }
        Spacer(Modifier.size(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            if (stats != null) Button(
                content = { Text(stringResource(Res.string.versions_page_stats)) },
                onClick = onStatsShow
            )

            Button(
                content = { Text(stringResource(Res.string.common_play).uppercase()) },
                type = ButtonType.Hero,
                disabled = minecraftStatus != MinecraftStatus.READY,
                onClick = onLaunch
            )
        }
    }
}

@Composable
fun WorldStatsDialog(state: VersionsViewModel.StatsState, worldId: String, onDismissRequest: () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.versions_page_stats)) },
        buttons = {
            Button(
                content = { Text(stringResource(Res.string.common_close)) },
                onClick = onDismissRequest
            )
        }
    ) {
        when (state) {
            is VersionsViewModel.StatsState.Loading -> {
                Box(Modifier.fillMaxSize()) {
                    LoadingIndicator(Modifier.align(Alignment.Center))
                }
            }
            is VersionsViewModel.StatsState.Empty -> {
                Box(Modifier.fillMaxSize()) {
                    Text(stringResource(Res.string.versions_stats_empty), Modifier.align(Alignment.Center))
                }
            }
            is VersionsViewModel.StatsState.Error -> {
                Box(Modifier.fillMaxSize()) {
                    Text(stringResource(Res.string.common_error, state.message ?: ""), Modifier.align(Alignment.Center), style = TextStyle(color = Color.Red))
                }
            }
            is VersionsViewModel.StatsState.Loaded -> {
                val scrollState = rememberScrollState()
                Row(Modifier.fillMaxWidth()) {
                    val worldStats = state.stats.find { it.worldId == worldId }
                    Column(Modifier.verticalScroll(scrollState)) {
                        if (worldStats != null) WorldStatsItem(worldStats) else Text(stringResource(Res.string.versions_stats_empty))
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun WorldStatsItem(stats: DetailedWorldStats) {
    @Suppress("UNUSED_VARIABLE")
    val translationData by TranslationManager.data.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        stats.categories.toList().forEachIndexed { count, (category, items) ->
            if (items.isNotEmpty()) {
                val translatedCategory = translateCategory(category)
                Text(translatedCategory, style = TextStyle(
                    fontSize = 16.sp,
                    color = MinecraftTheme.colors.textPrimary),
                    modifier = if (count != 0) Modifier.padding(top = 16.dp) else Modifier
                )
                Spacer(Modifier.height(4.dp))

                items.toList().forEachIndexed { index, (id, count) ->
                    val translatedName = TranslationManager.translate(id)
                        .takeIf { it != id }
                        ?: TranslationManager.getTranslatedBlockName(id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 1) Color.Transparent else Color.Black.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(translatedName, style = TextStyle(fontSize = 14.sp))
                        Text(formatStatValue(id, count), style = TextStyle(fontSize = 14.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun translateCategory(category: String): String {
    return when (category) {
        "minecraft:mined" -> stringResource(Res.string.stat_cat_mined)
        "minecraft:used" -> stringResource(Res.string.stat_cat_used)
        "minecraft:broken" -> stringResource(Res.string.stat_cat_broken)
        "minecraft:crafted" -> stringResource(Res.string.stat_cat_crafted)
        "minecraft:dropped" -> stringResource(Res.string.stat_cat_dropped)
        "minecraft:picked_up" -> stringResource(Res.string.stat_cat_picked_up)
        "minecraft:killed" -> stringResource(Res.string.stat_cat_killed)
        "minecraft:killed_by" -> stringResource(Res.string.stat_cat_killed_by)
        "minecraft:custom" -> stringResource(Res.string.stat_cat_custom)
        else -> category.substringAfter(":")
    }
}

@Composable
private fun formatStatValue(id: String, value: Int): String {
    val timeKeys = listOf(
        "minecraft:time_since_rest",
        "minecraft:time_since_death",
        "minecraft:total_world_time",
        "minecraft:sneak_time",
        "minecraft:play_time",
        "minecraft:play_one_minute",
        "stat.playOneMinute",
        "stat.timeSinceRest",
        "stat.sneakTime"
    )

    val cmKeys = listOf(
        "_one_cm",
        "OneCm"
    )

    return when {
        timeKeys.any { id == it } -> formatPlayTime(value.toLong() / 20)
        cmKeys.any { id.endsWith(it) } -> {
            val meters = value / 100
            if (meters >= 1000) {
                val km = meters / 1000.0
                stringResource(Res.string.stat_value_km, km.toString().take(4))
            } else stringResource(Res.string.stat_value_m, meters)
        }
        id == "minecraft:damage_dealt" || id == "minecraft:damage_taken" || id == "stat.damageDealt" || id == "stat.damageTaken" -> {
            val hearts = value / 2.0
            stringResource(Res.string.stat_value_hp, hearts)
        }
        else -> {
            // Add thousands separator for large numbers
            value.toString().reversed().chunked(3).joinToString(" ").reversed()
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