/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.versions

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.managers.*
import com.bartixw.anymc.minecraft.data.*
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.ui.components.*
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.*
import com.bartixw.anymc.managers.ModDownloadManager
import com.bartixw.anymc.screens.modrinth.ModrinthRoute
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.time.LocalDateTime

@Composable
fun rememberImageBitmap(path: String?): ImageBitmap? {
    return remember(path) {
        if (path == null) return@remember null
        try {
            val file = File(path)
            if (file.exists()) {
                file.readBytes().decodeToImageBitmap()
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VersionsScreen(navController: NavController) {
    val viewModel = viewModel {
        VersionsViewModel()
    }

    val config by ConfigManager.data.collectAsState()

    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (config.controllerSupport) {
            firstItemFocusRequester.requestFocus()
        }
    }

    val installationsListState = rememberLazyListState()
    val worldsListState = rememberLazyListState()
    val modsListState = rememberLazyListState()

    var editNameInstallationId by remember { mutableStateOf<String?>(null) }
    var editNameCurrentName by remember { mutableStateOf<String?>(null) }
    var settingsInstallationId by remember { mutableStateOf<String?>(null) }
    var currentJvmArgs by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentRemovedArgs by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentJavaVersion by remember { mutableStateOf<JavaVersion?>(null) }
    val minecraftStatus by MinecraftManager.minecraftStatus.collectAsState()
    val updatingInstallations by MinecraftManager.updatingInstallations.collectAsState()

    LaunchedEffect(settingsInstallationId) {
        val id = settingsInstallationId
        if (id != null) {
            viewModel.getInstallationData(id) { data ->
                currentJvmArgs = data?.jvmArgs ?: emptyList()
                currentRemovedArgs = data?.removedArgs ?: emptyList()
                currentJavaVersion = data?.javaVersion
            }
        }
    }

    if (editNameInstallationId != null) {
        RenameInstallationDialog(
            onDismissRequest = { editNameInstallationId = null },
            onRename = { newName ->
                viewModel.renameInstallation(
                    id = editNameInstallationId!!,
                    newName = newName,
                    onComplete = { editNameInstallationId = null }
                )
            },
            currentName = editNameCurrentName!!
        )
    }

    if (settingsInstallationId != null) {
        InstallationSettingsDialog(
            initialJvmArgs = currentJvmArgs,
            initialRemovedArgs = currentRemovedArgs,
            initialJavaVersion = currentJavaVersion,
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

    Column(modifier = Modifier.fillMaxSize()) {
        McTopAppBar(
            navController = navController,
            title = stringResource(Res.string.versions_title),
            actions = listOf(
                {
                    McButton(
                        text = stringResource(Res.string.versions_new_installation),
                        icon = Res.drawable.ic_plus,
                        onClick = {
                            navController.navigate("versions/create/type")
                        },
                        modifier = Modifier.focusRequester(firstItemFocusRequester)
                    )
                }
            )
        )
        Row {
            when (val uiState = viewModel.uiState) {
                is VersionsViewModel.UiState.Loading ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        McLoadingIndicator()
                    }
                is VersionsViewModel.UiState.Loaded -> {
                    Row(modifier = Modifier.weight(1.5f)) {
                        LazyColumn(
                            state = installationsListState,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            items(
                                items = uiState.installations,
                                key = { it.id }
                            ) { installation ->
                                InstallationListItem(
                                    installation = installation,
                                    isSelected = viewModel.selectedInstallationId == installation.id,
                                    minecraftStatus = minecraftStatus,
                                    isUpdating = updatingInstallations.contains(installation.id),
                                    onSelect = {
                                        viewModel.selectInstallation(installation.id, installation.name)
                                    },
                                    onDelete = {
                                        viewModel.deleteInstallation(
                                            id = installation.id,
                                            onDeleted = { },
                                            onError = { }
                                        )
                                    },
                                    onRename = {
                                        editNameCurrentName = installation.name
                                        editNameInstallationId = installation.id
                                    },
                                    onOpenFolder = {
                                        val installationDir = MinecraftManager.getDirectory(installation.id)
                                        val osOpenCmd = when (PlatformUtils.getCurrentOS()) {
                                            "windows" -> "explorer"
                                            "osx" -> "open"
                                            else -> "xdg-open"
                                        }
                                        ProcessBuilder(osOpenCmd, installationDir.absolutePath).start()
                                    },
                                    onSettings = { settingsInstallationId = installation.id },
                                    onNbtEditor = {
                                        navController.navigate(com.bartixw.anymc.screens.nbt.NbtEditorRoute(installation.id))
                                    },
                                    onRestoreBackup = {
                                        viewModel.restoreBackup(installation.id)
                                    },
                                    devMode = config.devMode
                                )
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(installationsListState),
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                    
                    Column(modifier = Modifier.weight(3f)) {
                        val pages = remember {
                            mapOf(
                                "worlds" to Res.string.versions_page_worlds,
                                "mods" to Res.string.versions_page_mods,
                                "shaders" to Res.string.versions_page_shaders,
                                "texturepacks" to Res.string.versions_page_resourcepacks,
                                "stats" to Res.string.versions_page_stats
                            )
                        }

                        val pageIcons = remember {
                            mapOf(
                                "worlds" to Res.drawable.ic_globe,
                                "mods" to Res.drawable.ic_plugin,
                                "shaders" to Res.drawable.ic_glasses,
                                "texturepacks" to Res.drawable.ic_package,
                                "stats" to Res.drawable.ic_settings
                            )
                        }

                        val pagesExist = remember(viewModel.selectedInstallationId, uiState.installations) {
                            val selected = uiState.installations.find { it.id == viewModel.selectedInstallationId }
                            mapOf(
                                "worlds" to true,
                                "mods" to (selected?.modLoader != "vanilla"),
                                "shaders" to (selected?.modLoader != "vanilla"),
                                "texturepacks" to true,
                                "stats" to true
                            )
                        }

                        var selectedPage by remember { mutableStateOf("worlds") }
                        val scrollState = rememberScrollState()

                        LaunchedEffect(pagesExist) {
                            if (pagesExist[selectedPage] != true) {
                                selectedPage = "worlds"
                            }
                        }

                        Row (
                            Modifier.horizontalScroll(scrollState).height(IntrinsicSize.Min)
                        ) {
                            pagesExist.filter { it.value }.forEach { (page, _) ->
                                Box(
                                    Modifier
                                        .background(MinecraftTheme.colors.controlBackground)
                                        .width(IntrinsicSize.Min)
                                        .fillMaxHeight()
                                ) {
                                    McButton(
                                        text = stringResource(pages[page]!!),
                                        icon = pageIcons[page],
                                        onClick = {
                                            selectedPage = page
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (selectedPage == page) Box(
                                        Modifier.align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .padding(
                                                start = 4.dp,
                                                end = 4.dp,
                                                bottom = 4.dp
                                            )
                                    ) {
                                        Box(
                                            Modifier
                                                .background(MinecraftTheme.colors.primary)
                                                .fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalScrollbar(rememberScrollbarAdapter(scrollState))
                        
                        if (selectedPage == "worlds" && viewModel.selectedInstallationId != null) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                when (val worldsState = viewModel.worldsState) {
                                    is VersionsViewModel.WorldsState.Loading -> McLoadingIndicator()
                                    VersionsViewModel.WorldsState.Empty -> {
                                        val state = rememberLazyListState()
                                        Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            LazyColumn(
                                                state = state,
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            ) {
                                                item(key = "add_zip") {
                                                    McListItem(
                                                        title = stringResource(Res.string.versions_add_zip),
                                                        icon = Res.drawable.ic_zip_plus,
                                                        onClick = { viewModel.loadWorldFromZip(viewModel.selectedInstallationId!!) }
                                                    )
                                                }
                                                item {
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        McText(stringResource(Res.string.versions_empty_worlds))
                                                    }
                                                }
                                            }
                                            VerticalScrollbar(
                                                adapter = rememberScrollbarAdapter(state),
                                                modifier = Modifier.fillMaxHeight()
                                            )
                                        }
                                    }
                                    is VersionsViewModel.WorldsState.Error -> McText(stringResource(Res.string.common_error, worldsState.message ?: ""))
                                    is VersionsViewModel.WorldsState.Loaded -> {
                                        LazyColumn(
                                            state = worldsListState,
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        ) {
                                            item(key = "add_zip") {
                                                McListItem(
                                                    title = stringResource(Res.string.versions_add_zip),
                                                    icon = Res.drawable.ic_zip_plus,
                                                    onClick = { viewModel.loadWorldFromZip(viewModel.selectedInstallationId!!) }
                                                )
                                            }
                                            items(
                                                items = worldsState.worlds,
                                                key = { it.id }
                                            ) { worldItem ->
                                                WorldListItem(
                                                    worldItem = worldItem,
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
                                                    }
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

                        if (selectedPage == "mods" && viewModel.selectedInstallationId != null) {
                            GenericProjectPage(
                                viewModel = viewModel,
                                installationId = viewModel.selectedInstallationId!!,
                                state = viewModel.modsState,
                                subDir = "mods",
                                projectType = "mod",
                                emptyText = stringResource(Res.string.versions_page_mods), // reuse label if none found
                                errorTextPrefix = stringResource(Res.string.common_error, ""),
                                uiState = uiState,
                                navController = navController,
                                listState = modsListState,
                                onAddMod = { viewModel.addMod(it) }
                            )
                        }

                        if (selectedPage == "shaders" && viewModel.selectedInstallationId != null) {
                            GenericProjectPage(
                                viewModel = viewModel,
                                installationId = viewModel.selectedInstallationId!!,
                                state = viewModel.shadersState,
                                subDir = "shaderpacks",
                                projectType = "shader",
                                emptyText = stringResource(Res.string.versions_page_shaders),
                                errorTextPrefix = stringResource(Res.string.common_error, ""),
                                uiState = uiState,
                                navController = navController,
                                listState = rememberLazyListState(),
                                onAddMod = { viewModel.addShader(it) }
                            )
                        }

                        if (selectedPage == "texturepacks" && viewModel.selectedInstallationId != null) {
                            GenericProjectPage(
                                viewModel = viewModel,
                                installationId = viewModel.selectedInstallationId!!,
                                state = viewModel.resourcePacksState,
                                subDir = "resourcepacks",
                                projectType = "resourcepack",
                                emptyText = stringResource(Res.string.versions_page_resourcepacks),
                                errorTextPrefix = stringResource(Res.string.common_error, ""),
                                uiState = uiState,
                                navController = navController,
                                listState = rememberLazyListState(),
                                onAddMod = { viewModel.addResourcePack(it) }
                            )
                        }
                        if (selectedPage == "stats" && viewModel.selectedInstallationId != null) {
                            StatsPage(viewModel.statsState)
                        }
                    }
                }
                is VersionsViewModel.UiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        McText(stringResource(Res.string.common_error, uiState.message ?: ""))
                        Spacer(Modifier.size(16.dp))
                        McButton(
                            text = stringResource(Res.string.common_retry),
                            icon = Res.drawable.ic_reload,
                            onClick = { viewModel.loadVersions() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsPage(state: VersionsViewModel.StatsState) {
    when (state) {
        is VersionsViewModel.StatsState.Loading -> {
            Box(Modifier.fillMaxSize()) {
                McLoadingIndicator(Modifier.align(Alignment.Center))
            }
        }
        is VersionsViewModel.StatsState.Empty -> {
            Box(Modifier.fillMaxSize()) {
                McText(stringResource(Res.string.versions_stats_empty), Modifier.align(Alignment.Center))
            }
        }
        is VersionsViewModel.StatsState.Error -> {
            Box(Modifier.fillMaxSize()) {
                McText(stringResource(Res.string.common_error, state.message ?: ""), Modifier.align(Alignment.Center), style = TextStyle(color = Color.Red))
            }
        }
        is VersionsViewModel.StatsState.Loaded -> {
            val listState = rememberLazyListState()
            Row(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    state = listState,
                ) {
                    items(state.stats) { worldStats ->
                        WorldStatsItem(worldStats)
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun WorldStatsItem(stats: DetailedWorldStats) {
    @Suppress("UNUSED_VARIABLE")
    val translationData by TranslationManager.data.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MinecraftTheme.colors.controlBackground.copy(alpha = 0.5f))
            .border(2.dp, MinecraftTheme.colors.controlBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
        ) {
            McListItem(
                title = stats.worldName,
                description = "Czas gry: ${formatPlayTime(stats.playTimeSeconds)} | Śmierci: ${stats.deaths}",
                modifier = Modifier.weight(1f)
            )
            McButton(
                onClick = { expanded = !expanded },
                icon = if (expanded) Res.drawable.ic_chevron_up else Res.drawable.ic_chevron_down,
                modifier = Modifier.fillMaxHeight()
            )
        }

        if (expanded) {
            Column (Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                stats.categories.forEach { (category, items) ->
                    if (items.isNotEmpty()) {
                        val translatedCategory = translateCategory(category)
                        McText(translatedCategory, style = TextStyle(
                            fontSize = 16.sp,
                            color = MinecraftTheme.colors.textPrimary),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Spacer(Modifier.height(4.dp))

                        items.toList().forEachIndexed { index, (id, count) ->
                            val translatedName = TranslationManager.translate(id)
                                .takeIf { it != id }
                                ?: TranslationManager.getTranslatedBlockName(id)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) Color.Transparent else Color.Black.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                McText(translatedName, style = TextStyle(fontSize = 14.sp))
                                McText(formatStatValue(id, count), style = TextStyle(fontSize = 14.sp))
                            }
                        }
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

@Composable
private fun GenericProjectPage(
    viewModel: VersionsViewModel,
    installationId: String,
    state: Any,
    subDir: String,
    projectType: String,
    emptyText: String,
    errorTextPrefix: String,
    uiState: VersionsViewModel.UiState.Loaded,
    navController: NavController,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAddMod: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .onModExternalDrag(
                onDragStart = { },
                onDragExit = { },
                onDrop = { uris ->
                    viewModel.addGenericFromPaths(installationId, uris, subDir)
                }
            )
            .background(MinecraftTheme.colors.backgroundDirt),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        when (state) {
            is VersionsViewModel.ModsState.Loading, is VersionsViewModel.ResourcePacksState.Loading, is VersionsViewModel.ShadersState.Loading -> McLoadingIndicator()
            is VersionsViewModel.ModsState.Empty, VersionsViewModel.ResourcePacksState.Empty, VersionsViewModel.ShadersState.Empty -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    state = listState
                ) {
                    item(key = "add_header") {
                        AddModHeader(viewModel, uiState, navController, projectType, onAddMod)
                    }
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            McText(emptyText)
                        }
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier.fillMaxHeight()
                )
            }
            is VersionsViewModel.ModsState.Error -> McText("$errorTextPrefix\n${state.message}")
            is VersionsViewModel.ResourcePacksState.Error -> McText("$errorTextPrefix\n${state.message}")
            is VersionsViewModel.ShadersState.Error -> McText("$errorTextPrefix\n${state.message}")
            else -> {
                val items = when (state) {
                    is VersionsViewModel.ModsState.Loaded -> state.mods
                    is VersionsViewModel.ResourcePacksState.Loaded -> state.packs
                    is VersionsViewModel.ShadersState.Loaded -> state.shaders
                    else -> emptyList()
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    state = listState
                ) {
                    item(key = "add_header") {
                        AddModHeader(viewModel, uiState, navController, projectType, onAddMod)
                    }
                    items(
                        items = items,
                        key = { it.fileName }
                    ) { item ->
                        val icon = rememberImageBitmap(item.iconPath)
                        McListItem(
                            title = item.name,
                            description = item.version,
                            iconBitmap = if (item.iconPath != "NULL") icon ?: imageResource(Res.drawable.ic_image_unknown) else null,
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    McButton(
                                        icon = Res.drawable.ic_bin,
                                        onClick = { viewModel.deleteGeneric(installationId, item, subDir) }
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    McCheckbox(
                                        checked = item.enabled,
                                        onCheckedChange = {
                                            viewModel.toggleGeneric(installationId, item, subDir)
                                        }
                                    )
                                }
                            }
                        )
                    }

                    item {
                        val downloadTasks by ModDownloadManager.tasks.collectAsState()
                        val currentTasks = downloadTasks.filter { it.key.startsWith("$installationId-") }

                        if (currentTasks.isNotEmpty()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                McText(stringResource(Res.string.versions_mod_downloading))
                                currentTasks.forEach { (_, task) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        McText(task.fileName, modifier = Modifier.weight(1f))
                                        when (task.status) {
                                            ModDownloadManager.Status.QUEUED -> McText(stringResource(Res.string.versions_mod_status_queued))
                                            ModDownloadManager.Status.DOWNLOADING -> McText("${(task.progress * 100).toInt()}%")
                                            ModDownloadManager.Status.COMPLETED -> McText(stringResource(Res.string.versions_mod_status_completed), style = TextStyle(color = MinecraftTheme.colors.primary))
                                            ModDownloadManager.Status.FAILED -> McText(stringResource(Res.string.versions_mod_status_failed), style = TextStyle(color = Color.Red))
                                        }
                                    }
                                }
                                McButton(
                                    text = stringResource(Res.string.versions_mod_clear_completed),
                                    onClick = { ModDownloadManager.clearCompletedTasks() }
                                )
                            }
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
}

@Composable
private fun AddModHeader(
    viewModel: VersionsViewModel,
    uiState: VersionsViewModel.UiState.Loaded,
    navController: NavController,
    projectType: String,
    onAddFromFile: (String) -> Unit
) {
    val title = when (projectType) {
        "mod" -> stringResource(Res.string.versions_page_mods)
        "resourcepack" -> stringResource(Res.string.versions_page_resourcepacks)
        "shader" -> stringResource(Res.string.versions_page_shaders)
        else -> "element"
    }
    Column(
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        McText(stringResource(Res.string.versions_add_element_title, title))
        Row {
            McListItem(
                title = stringResource(Res.string.versions_add_from_file),
                icon = Res.drawable.ic_file,
                onClick = { onAddFromFile(viewModel.selectedInstallationId!!) },
                modifier = Modifier.weight(1f)
            )
            McListItem(
                title = "Modrinth",
                icon = Res.drawable.ic_modrinth,
                onClick = {
                    val installation = uiState.installations.find { it.id == viewModel.selectedInstallationId }
                    if (installation != null) {
                        navController.navigate(
                            ModrinthRoute(
                                installationId = installation.id,
                                mcVersion = installation.version,
                                loader = installation.modLoader,
                                projectType = projectType
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
