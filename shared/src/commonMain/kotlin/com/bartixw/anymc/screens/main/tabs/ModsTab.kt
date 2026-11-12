/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.main.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.height
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_cancel
import anymc.shared.generated.resources.common_delete
import anymc.shared.generated.resources.common_error
import anymc.shared.generated.resources.common_update
import anymc.shared.generated.resources.ic_bin
import anymc.shared.generated.resources.ic_file
import anymc.shared.generated.resources.ic_image_unknown
import anymc.shared.generated.resources.ic_modrinth
import anymc.shared.generated.resources.mods_add_from_file
import anymc.shared.generated.resources.mods_add_from_modrinth
import anymc.shared.generated.resources.mods_loader_update_available
import anymc.shared.generated.resources.mods_update_available
import anymc.shared.generated.resources.mods_update_proceed
import anymc.shared.generated.resources.mods_update_restore_tip
import anymc.shared.generated.resources.mods_update_warnings_desc
import anymc.shared.generated.resources.mods_update_warnings_title
import anymc.shared.generated.resources.mods_updating_status
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.ui.components.v2.Dialog
import anymc.shared.generated.resources.mods_count
import anymc.shared.generated.resources.resourcepacks_count
import anymc.shared.generated.resources.shaders_count
import anymc.shared.generated.resources.versions_add_element_title
import anymc.shared.generated.resources.versions_add_from_file
import anymc.shared.generated.resources.versions_add_zip
import anymc.shared.generated.resources.versions_mod_clear_completed
import anymc.shared.generated.resources.versions_mod_downloading
import anymc.shared.generated.resources.versions_mod_status_completed
import anymc.shared.generated.resources.versions_mod_status_failed
import anymc.shared.generated.resources.versions_mod_status_queued
import anymc.shared.generated.resources.versions_mods_empty
import anymc.shared.generated.resources.versions_page_mods
import anymc.shared.generated.resources.versions_page_resourcepacks
import anymc.shared.generated.resources.versions_page_shaders
import anymc.shared.generated.resources.versions_resourcepacks_empty
import anymc.shared.generated.resources.versions_shaders_empty
import anymc.shared.generated.resources.worlds_count
import anymc.shared.generated.resources.worlds_select_installation
import com.bartixw.anymc.managers.ModDownloadManager
import com.bartixw.anymc.screens.modrinth.ModrinthRoute
import com.bartixw.anymc.screens.versions.VersionsViewModel
import com.bartixw.anymc.screens.versions.rememberImageBitmap
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McCheckbox
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Checkbox
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.onModExternalDrag
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ModsTab(
    viewModel: VersionsViewModel,
    navController: NavController,
    type: String = "mods"
) {
    val modsListState = rememberLazyListState()

    val subDir = when (type) {
        "resourcepacks" -> "resourcepacks"
        "shaders" -> "shaderpacks"
        else -> "mods"
    }

    val projectType = when (type) {
        "resourcepacks" -> "resourcepack"
        "shaders" -> "shader"
        else -> "mod"
    }

    val pageTitle = when (type) {
        "resourcepacks" -> stringResource(Res.string.versions_page_resourcepacks)
        "shaders" -> stringResource(Res.string.versions_page_shaders)
        else -> stringResource(Res.string.versions_page_mods)
    }

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

        val uiState = viewModel.uiState
        if (uiState !is VersionsViewModel.UiState.Loaded) {
            LoadingIndicator()
            return@Row
        }

        val updatingInstallations by MinecraftManager.updatingInstallations.collectAsState()
        val isUpdating = updatingInstallations.contains(viewModel.selectedInstallationId)
        val anyUpdating = updatingInstallations.isNotEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onModExternalDrag(
                    onDragStart = { },
                    onDragExit = { },
                    onDrop = { uris ->
                        viewModel.addGenericFromPaths(viewModel.selectedInstallationId!!, uris, subDir)
                    }
                )
                .background(MinecraftTheme.colors.backgroundDirt)
        ) {
            if (isUpdating) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MinecraftTheme.colors.controlPrimaryBackground).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    LoadingIndicator()
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(Res.string.mods_updating_status),
                        style = TextStyle(color = MinecraftTheme.colors.primary)
                    )
                }
            }
            val genericState = when (type) {
                "resourcepacks" -> viewModel.resourcePacksState
                "shaders" -> viewModel.shadersState
                else -> viewModel.modsState
            }

            when (genericState) {
                is VersionsViewModel.ModsState.Loading, is VersionsViewModel.ResourcePacksState.Loading, is VersionsViewModel.ShadersState.Loading -> Box(Modifier.fillMaxSize()) {
                    LoadingIndicator(Modifier.align(Alignment.Center))
                }

                is VersionsViewModel.ModsState.Empty, VersionsViewModel.ResourcePacksState.Empty, VersionsViewModel.ShadersState.Empty -> {
                    Row(Modifier.fillMaxSize()) {
                        Column {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(
                                    when (genericState) {
                                        VersionsViewModel.ResourcePacksState.Empty -> Res.string.versions_resourcepacks_empty
                                        VersionsViewModel.ShadersState.Empty -> Res.string.versions_shaders_empty
                                        else -> Res.string.versions_mods_empty
                                    }
                                ))
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp).fillMaxWidth()
                            ) {
                                val countText = when (type) {
                                    "resourcepacks" -> stringResource(Res.string.resourcepacks_count, 0)
                                    "shaders" -> stringResource(Res.string.shaders_count, 0)
                                    else -> stringResource(Res.string.mods_count, 0)
                                }
                                Text(
                                    text = countText,
                                    style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                                )
                                Row {
                                    Button(
                                        content = { Text(stringResource(Res.string.mods_add_from_file)) },
                                        onClick = {
                                            when (type) {
                                                "resourcepacks" -> viewModel.addResourcePack(viewModel.selectedInstallationId!!)
                                                "shaders" -> viewModel.addShader(viewModel.selectedInstallationId!!)
                                                else -> viewModel.addMod(viewModel.selectedInstallationId!!)
                                            }
                                        },
                                    )
                                    Button(
                                        content = { Text(stringResource(Res.string.mods_add_from_modrinth)) },
                                        onClick = {
                                            val installation =
                                                uiState.installations.find { it.id == viewModel.selectedInstallationId }
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
                                        }
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(modsListState),
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }

                is VersionsViewModel.ModsState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    McText(stringResource(Res.string.common_error, genericState.message ?: ""))
                }
                is VersionsViewModel.ResourcePacksState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    McText(stringResource(Res.string.common_error, genericState.message ?: ""))
                }
                is VersionsViewModel.ShadersState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    McText(stringResource(Res.string.common_error, genericState.message ?: ""))
                }

                else -> {
                    val items = when (genericState) {
                        is VersionsViewModel.ModsState.Loaded -> genericState.mods
                        is VersionsViewModel.ResourcePacksState.Loaded -> genericState.packs
                        is VersionsViewModel.ShadersState.Loaded -> genericState.shaders
                        else -> emptyList()
                    }

                    Row(Modifier.fillMaxSize()) {
                        Column {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                state = modsListState
                            ) {
                                items(
                                    items = items,
                                    key = { it.fileName }
                                ) { item ->
                                    val icon = rememberImageBitmap(item.iconPath)
                                    ListItem(
                                        content = {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row (verticalAlignment = Alignment.CenterVertically) {
                                                    Image(
                                                        bitmap = if (item.iconPath != "NULL") icon ?: imageResource(
                                                            Res.drawable.ic_image_unknown
                                                        ) else imageResource(
                                                            Res.drawable.ic_image_unknown
                                                        ),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(48.dp),
                                                        filterQuality = FilterQuality.None
                                                    )
                                                    Spacer(Modifier.size(8.dp))
                                                    Column {
                                                        Text(item.name, style = MinecraftTheme.typographyPixel)
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                item.version,
                                                                style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                                                            )
                                                            if (item.hasUpdate) {
                                                                Spacer(Modifier.size(8.dp))
                                                                Text(
                                                                    stringResource(Res.string.mods_update_available),
                                                                    style = TextStyle(color = MinecraftTheme.colors.primary)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        resource = Res.drawable.ic_bin,
                                                        contentDescription = stringResource(Res.string.common_delete),
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clickable {
                                                                viewModel.deleteGeneric(
                                                                    viewModel.selectedInstallationId!!,
                                                                    item,
                                                                    subDir
                                                                )
                                                            }
                                                            .padding(4.dp)
                                                    )
                                                    if (type == "mods") {
                                                        Spacer(Modifier.size(8.dp))
                                                        Checkbox(
                                                            checked = item.enabled,
                                                            onCheckedChange = {
                                                                viewModel.toggleGeneric(
                                                                    viewModel.selectedInstallationId!!,
                                                                    item,
                                                                    subDir
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            if (type == "mods") {
                                Text(
                                    text = stringResource(Res.string.mods_update_restore_tip),
                                    style = TextStyle(color = MinecraftTheme.colors.textSecondary),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (type == "mods" && viewModel.hasLoaderUpdate) {
                                Text(
                                    text = stringResource(Res.string.mods_loader_update_available, viewModel.loaderUpdateVersion ?: ""),
                                    style = TextStyle(color = MinecraftTheme.colors.primary),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp).fillMaxWidth()
                            ) {
                                val countText = when (type) {
                                    "resourcepacks" -> stringResource(Res.string.resourcepacks_count, items.size)
                                    "shaders" -> stringResource(Res.string.shaders_count, items.size)
                                    else -> stringResource(Res.string.mods_count, items.size)
                                }
                                Text(
                                    text = countText,
                                    style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                                )
                                Row {
                                    Button(
                                        content = { Text(stringResource(Res.string.mods_add_from_file)) },
                                        onClick = {
                                            when (type) {
                                                "resourcepacks" -> viewModel.addResourcePack(viewModel.selectedInstallationId!!)
                                                "shaders" -> viewModel.addShader(viewModel.selectedInstallationId!!)
                                                else -> viewModel.addMod(viewModel.selectedInstallationId!!)
                                            }
                                        },
                                    )
                                    Button(
                                        content = { Text(stringResource(Res.string.mods_add_from_modrinth)) },
                                        onClick = {
                                            val installation =
                                                uiState.installations.find { it.id == viewModel.selectedInstallationId }
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
                                        }
                                    )
                                    if (type == "mods" && (items.any { it.hasUpdate } || viewModel.hasLoaderUpdate)) Button(
                                        content = { Text(stringResource(Res.string.common_update)) },
                                        type = ButtonType.Primary,
                                        disabled = anyUpdating,
                                        onClick = {
                                            viewModel.selectedInstallationId?.let { installationId ->
                                                viewModel.startUpdateMods(installationId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(modsListState),
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }
        }
    }

    if (viewModel.showUpdateDialog) {
        Dialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = { Text(stringResource(Res.string.mods_update_warnings_title)) },
            buttons = {
                Button(
                    content = { Text(stringResource(Res.string.common_cancel)) },
                    onClick = { viewModel.dismissUpdateDialog() }
                )
                Button(
                    content = { Text(stringResource(Res.string.mods_update_proceed)) },
                    type = ButtonType.Primary,
                    onClick = {
                        viewModel.selectedInstallationId?.let { installationId ->
                            viewModel.performUpdateMods(installationId)
                        }
                    }
                )
            }
        ) {
            Text(
                stringResource(Res.string.mods_update_warnings_desc),
                style = MinecraftTheme.typographyV2
            )
            Spacer(Modifier.height(8.dp))
            viewModel.updateWarnings.forEach { warning ->
                Text(
                    "• $warning",
                    style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
