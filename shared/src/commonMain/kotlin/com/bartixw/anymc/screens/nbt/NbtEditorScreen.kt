/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.nbt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.ui.components.*
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.viaversion.nbt.tag.*
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
data class NbtEditorRoute(val installationId: String)

@Composable
fun NbtEditorScreen(navController: NavController, installationId: String) {
    val viewModel = viewModel { NbtEditorViewModel(installationId) }
    
    var editingFileItem by remember { mutableStateOf<NbtEditorViewModel.NbtFileItem?>(null) }
    var editingNode by remember { mutableStateOf<NbtEditorViewModel.NbtNode?>(null) }
    var editValue by remember { mutableStateOf("") }

    if (editingNode != null && editingFileItem != null) {
        McDialog(
            title = stringResource(Res.string.nbt_edit_value, editingNode!!.name),
            onDismissRequest = { 
                editingNode = null
                editingFileItem = null
            },
            buttons = {
                McButton(
                    text = stringResource(Res.string.nbt_save),
                    onClick = {
                        viewModel.updateTagValue(editingFileItem!!, editingNode!!, editValue)
                        editingNode = null
                        editingFileItem = null
                    }
                )
                Spacer(Modifier.width(8.dp))
                McButton(
                    text = stringResource(Res.string.nbt_cancel),
                    onClick = { 
                        editingNode = null
                        editingFileItem = null
                    }
                )
            }
        ) {
            McTextField(
                value = editValue,
                onValueChange = { editValue = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        McTopAppBar(
            navController = navController,
            title = stringResource(Res.string.nbt_title),
            actions = emptyList()
        )
        
        when (val state = viewModel.uiState) {
            is NbtEditorViewModel.UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    McLoadingIndicator()
                }
            }
            is NbtEditorViewModel.UiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    McText(state.message)
                }
            }
            is NbtEditorViewModel.UiState.Loaded -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    renderFileTree(this, state.tree, 0, viewModel) { fileItem, node ->
                        editingFileItem = fileItem
                        editingNode = node
                        editValue = node.tag.value.toString()
                    }
                }
            }
        }
    }
}

private fun renderFileTree(
    scope: androidx.compose.foundation.lazy.LazyListScope,
    nodes: List<NbtEditorViewModel.FileTreeNode>,
    depth: Int,
    viewModel: NbtEditorViewModel,
    onEdit: (NbtEditorViewModel.NbtFileItem, NbtEditorViewModel.NbtNode) -> Unit
) {
    nodes.forEach { node ->
        when (node) {
            is NbtEditorViewModel.NbtFolderItem -> {
                scope.item(key = "folder_${node.name}_$depth") {
                    McListItem(
                        title = node.name,
                        icon = Res.drawable.ic_directory,
                        onClick = { viewModel.toggleFolder(node) },
                        modifier = Modifier.padding(start = (depth * 16).dp)
                    )
                }
                if (node.isExpanded) {
                    renderFileTree(scope, node.children, depth + 1, viewModel, onEdit)
                }
            }
            is NbtEditorViewModel.NbtFileItem -> {
                scope.item(key = node.file.absolutePath) {
                    McListItem(
                        title = node.name,
                        icon = Res.drawable.ic_dev,
                        onClick = { viewModel.toggleFile(node) },
                        modifier = Modifier.padding(start = (depth * 16).dp),
                        trailingContent = {
                            if (node.isLoading) {
                                McLoadingIndicator(modifier = Modifier.size(20.dp))
                            }
                        }
                    )
                }
                if (node.isExpanded) {
                    if (node.error != null) {
                        scope.item(key = "${node.file.absolutePath}_error") {
                            McText(
                                text = node.error!!,
                                style = TextStyle(color = MinecraftTheme.colors.primary, fontSize = 12.sp),
                                modifier = Modifier.padding(start = (depth * 16 + 32).dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                    } else if (node.rootNode != null) {
                        renderNbtNode(scope, node, node.rootNode!!, depth + 1, viewModel, onEdit)
                    }
                }
            }
        }
    }
}

private fun renderNbtNode(
    scope: androidx.compose.foundation.lazy.LazyListScope,
    fileItem: NbtEditorViewModel.NbtFileItem,
    node: NbtEditorViewModel.NbtNode,
    depth: Int,
    viewModel: NbtEditorViewModel,
    onEdit: (NbtEditorViewModel.NbtFileItem, NbtEditorViewModel.NbtNode) -> Unit
) {
    scope.item(key = "${fileItem.file.absolutePath}_${node.parentTag.hashCode()}_${node.name}") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp)
                .clickable {
                    if (node.children.isNotEmpty()) {
                        viewModel.toggleNode(fileItem, node)
                    } else if (node.tag !is CompoundTag && node.tag !is ListTag<*>) {
                        onEdit(fileItem, node)
                    }
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (node.tag) {
                is CompoundTag -> Res.drawable.ic_directory
                is ListTag<*> -> Res.drawable.ic_package
                else -> Res.drawable.ic_dev
            }
            McIcon(
                resource = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MinecraftTheme.colors.textPrimary
            )
            Spacer(Modifier.width(8.dp))
            McText(
                text = "${node.name}: ",
                style = TextStyle(fontSize = 14.sp)
            )
            McText(
                text = if (node.tag is CompoundTag || node.tag is ListTag<*>) "(${node.children.size})" else node.tag.value.toString(),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = MinecraftTheme.colors.textSecondary
                )
            )
        }
    }
    if (node.isExpanded) {
        node.children.forEach { child ->
            renderNbtNode(scope, fileItem, child, depth + 1, viewModel, onEdit)
        }
    }
}
