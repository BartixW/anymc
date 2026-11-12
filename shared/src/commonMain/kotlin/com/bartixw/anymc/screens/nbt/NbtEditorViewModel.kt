/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.nbt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bartixw.anymc.managers.MinecraftManager
import com.viaversion.nbt.io.NBTIO
import com.viaversion.nbt.tag.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NbtEditorViewModel(val installationId: String) : ViewModel() {

    sealed interface UiState {
        object Loading : UiState
        data class Loaded(val tree: List<FileTreeNode>) : UiState
        data class Error(val message: String) : UiState
    }

    sealed interface FileTreeNode {
        val name: String
        val isExpanded: Boolean
    }

    data class NbtFolderItem(
        override val name: String,
        override val isExpanded: Boolean = false,
        val children: List<FileTreeNode> = emptyList()
    ) : FileTreeNode

    data class NbtFileItem(
        val file: File,
        override val name: String,
        override val isExpanded: Boolean = false,
        val rootNode: NbtNode? = null,
        val isCompressed: Boolean = true,
        val error: String? = null,
        val isLoading: Boolean = false
    ) : FileTreeNode

    data class NbtNode(
        val name: String,
        val tag: Tag,
        val depth: Int,
        val isExpanded: Boolean = false,
        val children: List<NbtNode> = emptyList(),
        val parentTag: Tag? = null
    )

    var uiState by mutableStateOf<UiState>(UiState.Loading)
        private set

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            uiState = UiState.Loading
            try {
                val tree = withContext(Dispatchers.IO) {
                    val baseDir = MinecraftManager.getDirectory(installationId).resolve("gameFiles")
                    val datFiles = mutableListOf<File>()
                    
                    if (baseDir.exists()) {
                        // Scan root
                        baseDir.listFiles()?.filter { it.isFile && it.extension == "dat" }?.forEach {
                            datFiles.add(it)
                        }
                        
                        // Scan saves
                        val savesDir = baseDir.resolve("saves")
                        if (savesDir.exists()) {
                            savesDir.walkTopDown().maxDepth(3).forEach { file ->
                                if (file.isFile && file.extension == "dat") {
                                    datFiles.add(file)
                                }
                            }
                        }
                    }
                    buildFileTree(baseDir, datFiles.distinctBy { it.absolutePath })
                }
                uiState = UiState.Loaded(tree)
            } catch (e: Exception) {
                uiState = UiState.Error(e.toString())
            }
        }
    }

    private fun buildFileTree(baseDir: File, files: List<File>): List<FileTreeNode> {
        val filePaths = files.map { it to it.relativeTo(baseDir).path.split(File.separator) }
        
        fun buildLevel(currentFiles: List<Pair<File, List<String>>>, depth: Int): List<FileTreeNode> {
            val levelNodes = mutableListOf<FileTreeNode>()
            
            // Files at this level
            currentFiles.filter { it.second.size == depth + 1 }.forEach { (file, parts) ->
                levelNodes.add(NbtFileItem(file = file, name = parts.last()))
            }
            
            // Folders at this level
            currentFiles.filter { it.second.size > depth + 1 }
                .groupBy { it.second[depth] }
                .forEach { (folderName, folderFiles) ->
                    levelNodes.add(NbtFolderItem(
                        name = folderName,
                        children = buildLevel(folderFiles, depth + 1)
                    ))
                }
            
            return levelNodes.sortedBy { it.name }
        }
        
        return buildLevel(filePaths, 0)
    }

    fun toggleFolder(folder: NbtFolderItem) {
        val currentState = uiState as? UiState.Loaded ?: return
        val newTree = updateFileTree(currentState.tree) { node ->
            if (node === folder) (node as NbtFolderItem).copy(isExpanded = !node.isExpanded) else node
        }
        uiState = currentState.copy(tree = newTree)
    }

    fun toggleFile(fileItem: NbtFileItem) {
        val currentState = uiState as? UiState.Loaded ?: return

        if (fileItem.rootNode == null && !fileItem.isLoading) {
            // Start loading
            val loadingTree = updateFileTree(currentState.tree) { node ->
                if (node is NbtFileItem && node.file == fileItem.file) {
                    node.copy(isLoading = true, isExpanded = true)
                } else node
            }
            uiState = currentState.copy(tree = loadingTree)

            viewModelScope.launch {
                try {
                    var detectedCompressed = true
                    val rootTag = withContext(Dispatchers.IO) {
                        try {
                            detectedCompressed = true
                            tryRead(fileItem.file, true)
                        } catch (e: Exception) {
                            try {
                                detectedCompressed = false
                                tryRead(fileItem.file, false)
                            } catch (e2: Exception) {
                                throw e2
                            }
                        }
                    }

                    val loadedState = uiState as? UiState.Loaded ?: return@launch
                    val finalTree = updateFileTree(loadedState.tree) { node ->
                        if (node is NbtFileItem && node.file == fileItem.file) {
                            if (rootTag != null) {
                                val rootNode = mapTagToNode("ROOT", rootTag, 0)
                                node.copy(
                                    isLoading = false,
                                    rootNode = rootNode.copy(isExpanded = true),
                                    isCompressed = detectedCompressed,
                                    error = null
                                )
                            } else {
                                node.copy(isLoading = false, error = "Root tag is not a compound tag")
                            }
                        } else node
                    }
                    uiState = loadedState.copy(tree = finalTree)
                } catch (e: Exception) {
                    val loadedState = uiState as? UiState.Loaded ?: return@launch
                    val errorTree = updateFileTree(loadedState.tree) { node ->
                        if (node is NbtFileItem && node.file == fileItem.file) {
                            node.copy(isLoading = false, error = "Error: ${e.message}")
                        } else node
                    }
                    uiState = loadedState.copy(tree = errorTree)
                }
            }
        } else {
            val updatedTree = updateFileTree(currentState.tree) { node ->
                if (node is NbtFileItem && node.file == fileItem.file) {
                    node.copy(isExpanded = !node.isExpanded)
                } else node
            }
            uiState = currentState.copy(tree = updatedTree)
        }
    }

    private fun updateFileTree(tree: List<FileTreeNode>, transform: (FileTreeNode) -> FileTreeNode): List<FileTreeNode> {
        return tree.map { node ->
            val newNode = transform(node)
            if (newNode is NbtFolderItem && newNode.children.isNotEmpty()) {
                newNode.copy(children = updateFileTree(newNode.children, transform))
            } else newNode
        }
    }

    private fun tryRead(file: File, compressed: Boolean): CompoundTag? {
        return try {
            NBTIO.reader().named().read(file.toPath(), compressed) as? CompoundTag
        } catch (e: Exception) {
            // Try unnamed
            NBTIO.reader().read(file.toPath(), compressed) as? CompoundTag
        }
    }

    private fun mapTagToNode(name: String, tag: Tag, depth: Int, parentTag: Tag? = null): NbtNode {
        val children = when (tag) {
            is CompoundTag -> {
                tag.map { (key, value) -> mapTagToNode(key, value, depth + 1, tag) }.sortedBy { it.name }
            }
            is ListTag<*> -> {
                tag.value.mapIndexed { index, value -> mapTagToNode("[$index]", value as Tag, depth + 1, tag) }
            }
            else -> emptyList()
        }
        return NbtNode(name, tag, depth, children = children, parentTag = parentTag)
    }

    fun toggleNode(fileItem: NbtFileItem, nodeToToggle: NbtNode) {
        val currentState = uiState as? UiState.Loaded ?: return
        if (fileItem.rootNode == null) return

        val newRoot = updateNbtNodeInTree(fileItem.rootNode) { node ->
            if (node.tag === nodeToToggle.tag && node.name == nodeToToggle.name) {
                node.copy(isExpanded = !node.isExpanded)
            } else node
        }

        val updatedTree = updateFileTree(currentState.tree) { node ->
            if (node is NbtFileItem && node.file == fileItem.file) {
                node.copy(rootNode = newRoot)
            } else node
        }
        uiState = currentState.copy(tree = updatedTree)
    }

    private fun updateNbtNodeInTree(current: NbtNode, transform: (NbtNode) -> NbtNode): NbtNode {
        val newNode = transform(current)
        if (newNode.children.isEmpty()) return newNode
        
        val newChildren = newNode.children.map { updateNbtNodeInTree(it, transform) }
        return newNode.copy(children = newChildren)
    }

    fun updateTagValue(fileItem: NbtFileItem, nodeToUpdate: NbtNode, newValue: String) {
        try {
            val oldTag = nodeToUpdate.tag
            val newTag: Tag = when (oldTag) {
                is StringTag -> {
                    oldTag.value = newValue
                    oldTag
                }
                is IntTag -> IntTag(newValue.toInt())
                is LongTag -> LongTag(newValue.toLong())
                is FloatTag -> FloatTag(newValue.toFloat())
                is DoubleTag -> DoubleTag(newValue.toDouble())
                is ByteTag -> ByteTag(newValue.toByte())
                is ShortTag -> ShortTag(newValue.toShort())
                else -> return
            }

            if (newTag !== oldTag) {
                val parent = nodeToUpdate.parentTag
                if (parent is CompoundTag) {
                    parent.put(nodeToUpdate.name, newTag)
                } else if (parent is ListTag<*>) {
                    val index = nodeToUpdate.name.removeSurrounding("[", "]").toIntOrNull()
                    if (index != null) {
                        @Suppress("UNCHECKED_CAST")
                        (parent as ListTag<Tag>).set(index, newTag)
                    }
                }
                
                // Refresh the tree for this file
                val currentState = uiState as? UiState.Loaded ?: return
                val rootTag = fileItem.rootNode?.tag ?: return
                val newRootNode = mapTagToNode("ROOT", rootTag, 0)
                
                val updatedTree = updateFileTree(currentState.tree) { node ->
                    if (node is NbtFileItem && node.file == fileItem.file) {
                        node.copy(rootNode = newRootNode.copy(isExpanded = true))
                    } else node
                }
                uiState = currentState.copy(tree = updatedTree)
            }

            saveAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveAll() {
        val currentState = uiState as? UiState.Loaded ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            fun walkTree(nodes: List<FileTreeNode>) {
                nodes.forEach { node ->
                    when (node) {
                        is NbtFileItem -> {
                            val rootNode = node.rootNode
                            if (rootNode != null) {
                                try {
                                    NBTIO.writer().named().write(node.file.toPath(), rootNode.tag, node.isCompressed)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        is NbtFolderItem -> walkTree(node.children)
                    }
                }
            }
            walkTree(currentState.tree)
        }
    }
}
