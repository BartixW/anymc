/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.modrinth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import com.bartixw.anymc.minecraft.data.modrinth.*
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.managers.ModDownloadManager
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import org.jetbrains.compose.resources.decodeToImageBitmap
import anymc.shared.generated.resources.*
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthProject
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthSearchHit
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthTeamMember
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthVersion
import com.bartixw.anymc.minecraft.services.ModrinthService
import kotlin.time.Duration.Companion.milliseconds

class ModrinthViewModel(
    val installationId: String,
    val mcVersion: String,
    val loader: String,
    val projectType: String = "mod"
) : ViewModel() {
    private val modrinthService = ModrinthService(MinecraftManager.client)

    var searchQuery by mutableStateOf("")
        private set

    var searchResults by mutableStateOf<List<ModrinthSearchHit>>(emptyList())
        private set

    var isSearching by mutableStateOf(false)
        private set

    var isLoadingMore by mutableStateOf(false)
        private set

    var selectedProject by mutableStateOf<ModrinthProject?>(null)
        private set
    var isLoadingProject by mutableStateOf(false)
        private set

    var teamMembers by mutableStateOf<List<ModrinthTeamMember>>(emptyList())
        private set

    private var offset = 0
    private var totalHits = 0
    private val limit = 20

    var selectedModId by mutableStateOf<String?>(null)
    var modVersions by mutableStateOf<List<ModrinthVersion>>(emptyList())
    var isLoadingVersions by mutableStateOf(false)

    var selectedVersion by mutableStateOf<ModrinthVersion?>(null)
        private set

    val versionContentProjects = mutableStateMapOf<String, ModrinthProject>()

    val imageCache = mutableStateMapOf<String, ImageBitmap?>()

    val categoryGroups = ModrinthCategories.getGroupsFor(projectType)

    var selectedCategory by mutableStateOf<String?>(null)
        private set

    private var searchJob: Job? = null

    init {
        performSearch()
    }

    fun loadImage(url: String) {
        if (imageCache.containsKey(url)) return
        
        imageCache[url] = null // Placeholder/Loading
        viewModelScope.launch {
            try {
                val bytes = MinecraftManager.client.get(url).bodyAsBytes()
                val bitmap = bytes.decodeToImageBitmap()
                imageCache[url] = bitmap
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500.milliseconds)
            offset = 0
            searchResults = emptyList()
            performSearch()
        }
    }

    fun selectCategory(categorySlug: String?) {
        if (selectedCategory == categorySlug) {
            selectedCategory = null
        } else {
            selectedCategory = categorySlug
        }
        offset = 0
        searchResults = emptyList()
        performSearch()
    }

    private fun performSearch() {
        viewModelScope.launch {
            isSearching = true
            try {
                val result = modrinthService.searchMods(
                    query = searchQuery,
                    loaders = if (projectType == "mod") listOf(if (loader == "legacyfabric") "legacy-fabric" else loader) else emptyList(),
                    gameVersions = listOf(mcVersion),
                    limit = limit,
                    offset = offset,
                    index = if (searchQuery.isEmpty()) "downloads" else "relevance",
                    projectType = projectType,
                    category = selectedCategory
                )
                searchResults = result.hits
                totalHits = result.totalHits
                offset += result.hits.size
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSearching = false
            }
        }
    }

    fun loadNextPage() {
        if (isLoadingMore || isSearching || searchResults.size >= totalHits) return

        viewModelScope.launch {
            isLoadingMore = true
            try {
                val result = modrinthService.searchMods(
                    query = searchQuery,
                    loaders = if (projectType == "mod") listOf(if (loader == "legacyfabric") "legacy-fabric" else loader) else emptyList(),
                    gameVersions = listOf(mcVersion),
                    limit = limit,
                    offset = offset,
                    index = if (searchQuery.isEmpty()) "downloads" else "relevance",
                    projectType = projectType,
                    category = selectedCategory
                )
                searchResults = searchResults + result.hits
                totalHits = result.totalHits
                offset += result.hits.size
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun selectMod(projectId: String) {
        selectedModId = projectId
        viewModelScope.launch {
            isLoadingVersions = true
            try {
                modVersions = modrinthService.getProjectVersions(
                    projectId = projectId,
                    loaders = if (projectType == "mod") listOf(if (loader == "legacyfabric") "legacy-fabric" else loader) else emptyList(),
                    gameVersions = listOf(mcVersion)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingVersions = false
            }
        }
    }

    fun selectVersion(version: ModrinthVersion?) {
        selectedVersion = version
        if (version != null) {
            loadVersionDetails(version)
        }
    }

    private fun loadVersionDetails(version: ModrinthVersion) {
        viewModelScope.launch {
            try {
                val dependencyIds = version.dependencies.mapNotNull { it.projectId }
                if (dependencyIds.isNotEmpty()) {
                    val projects = modrinthService.getProjects(dependencyIds)
                    projects.forEach { project ->
                        versionContentProjects[project.id] = project
                        project.iconUrl?.let { loadImage(it) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun downloadVersion(version: ModrinthVersion) {
        ModDownloadManager.downloadMod(installationId, version, if (loader == "legacyfabric") "legacy-fabric" else loader, mcVersion, projectType)
    }

    fun loadProject(projectId: String) {
        selectedProject = null
        teamMembers = emptyList()
        isLoadingProject = true
        viewModelScope.launch {
            try {
                val project = modrinthService.getProject(projectId)
                selectedProject = project
                
                launch {
                    try {
                        teamMembers = modrinthService.getProjectTeam(project.teamId)
                        teamMembers.forEach { member ->
                            member.user.avatarUrl?.let { loadImage(it) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                selectMod(projectId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingProject = false
            }
        }
    }

    fun quickDownload(hit: ModrinthSearchHit) {
        viewModelScope.launch {
            try {
                val versions = modrinthService.getProjectVersions(
                    projectId = hit.projectId,
                    loaders = if (projectType == "mod") listOf(if (loader == "legacyfabric") "legacy-fabric" else loader) else emptyList(),
                    gameVersions = listOf(mcVersion)
                )
                versions.firstOrNull()?.let {
                    downloadVersion(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun downloadLatest() {
        modVersions.firstOrNull()?.let {
            downloadVersion(it)
        }
    }
}
