/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.modrinth

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.*
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthSearchHit
import com.bartixw.anymc.ui.components.*
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.Checkbox
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.Input
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.components.v2.TopAppBar
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ModrinthModpackScreen(
    navController: NavController
) {
    val viewModel = viewModel<ModrinthModpackViewModel>(
        key = "ModrinthModpackList",
        factory = viewModelFactory {
            initializer { ModrinthModpackViewModel() }
        }
    )

    val listState = rememberLazyListState()
    val categoryListState = rememberLazyListState()

    var selectedHitForVersions by remember { mutableStateOf<ModrinthSearchHit?>(null) }

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextPage()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MinecraftTheme.colors.backgroundDirt)) {
        TopAppBar(
            navController = navController,
            title = { Text(stringResource(Res.string.modrinth_title_modpack)) },
            actions = { }
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            Column(
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight()
                    .background(MinecraftTheme.colors.backgroundDirt)
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Row {
                    LazyColumn(modifier = Modifier.weight(1f), state = categoryListState) {
                        viewModel.categoryGroups.forEach { group ->
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(group.name),
                                        style = MinecraftTheme.typographyPixel.copy(fontSize = 20.sp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            items(group.categories) { category ->
                                val isSelected = viewModel.selectedCategory == category.slug
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectCategory(category.slug) }
                                        .background(
                                            if (isSelected) MinecraftTheme.colors.surfaceHighlight.copy(
                                                alpha = 0.3f
                                            ) else Color.Transparent
                                        )
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        resource = category.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MinecraftTheme.colors.primary else MinecraftTheme.colors.textPrimary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(category.name),
                                        style = MinecraftTheme.typographyPixel.copy(color = if (isSelected) MinecraftTheme.colors.primary else MinecraftTheme.colors.textPrimary)
                                    )
                                }
                            }

                            item {
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(categoryListState),
                        modifier = Modifier.width(6.dp).padding(start = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Input(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = stringResource(Res.string.modrinth_search_placeholder_modpacks),
                        modifier = Modifier.weight(1f)
                    )
                    if (viewModel.isSearching) {
                        Spacer(Modifier.size(16.dp))
                        LoadingIndicator()
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    state = listState
                ) {
                    itemsIndexed(viewModel.searchResults) { index, hit ->
                        val icon = hit.iconUrl?.let { url ->
                            LaunchedEffect(url) {
                                viewModel.loadImage(url)
                            }
                            viewModel.imageCache[url]
                        }

                        ListItem(
                            baseModifier = Modifier.clickable {
                                navController.navigate(ModrinthModpackDetailRoute(hit.projectId))
                            }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row (
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Image(
                                        bitmap = icon ?: imageResource(Res.drawable.ic_image_unknown),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        filterQuality = FilterQuality.None
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Column {
                                        Text(hit.title, style = MinecraftTheme.typographyPixel)
                                        Text(
                                            hit.description,
                                            style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(IntrinsicSize.Max)
                                ) {
                                    Icon(
                                        resource = Res.drawable.ic_download,
                                        contentDescription = stringResource(Res.string.modrinth_btn_download),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                viewModel.installModpack(hit)
                                                navController.navigate("main") {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                            .padding(4.dp)
                                    )
                                    Icon(
                                        resource = Res.drawable.ic_chevron_down,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                selectedHitForVersions = hit
                                                viewModel.loadVersions(hit.projectId)
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (viewModel.isLoadingMore) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                McLoadingIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedHitForVersions != null) {
        val hit = selectedHitForVersions!!
        Dialog(
            title = { Text(stringResource(Res.string.modrinth_install_version_title, hit.title)) },
            buttons = {
                Button(
                    // TODO: rename nbt_cancel to common_cancel
                    content = { Text(stringResource(Res.string.nbt_cancel)) },
                    onClick = { selectedHitForVersions = null }
                )
            },
            onDismissRequest = { selectedHitForVersions = null }
        ) {
            if (viewModel.isLoadingVersions) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(viewModel.modpackVersions) { version ->
                        ListItem(
                            baseModifier = Modifier.clickable {
                                viewModel.installVersion(version, hit.title)
                                selectedHitForVersions = null
                                navController.navigate("main") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                                .fillMaxWidth()
                        ) {
                            Row (verticalAlignment = Alignment.CenterVertically) {
                                Icon(Res.drawable.ic_download, null, Modifier.size(24.dp))
                                Spacer(Modifier.size(8.dp))
                                Column {
                                    Text(version.versionNumber, style = MinecraftTheme.typographyPixel)
                                    Text(
                                        "MC: ${version.gameVersions.joinToString(", ")} | ${version.loaders.joinToString(", ")}",
                                        style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
