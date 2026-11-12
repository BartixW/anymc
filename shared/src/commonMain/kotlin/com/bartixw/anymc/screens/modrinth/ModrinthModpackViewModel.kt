/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.modrinth

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthProject
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthSearchHit
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthTeamMember
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthVersion
import com.bartixw.anymc.minecraft.services.ModrinthService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.Url
import kotlinx.coroutines.*
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.time.Duration.Companion.milliseconds

class ModrinthModpackViewModel : ViewModel() {
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

    var modpackVersions by mutableStateOf<List<ModrinthVersion>>(emptyList())
        private set

    var selectedVersion by mutableStateOf<ModrinthVersion?>(null)
        private set

    val versionContentProjects = mutableStateMapOf<String, ModrinthProject>()

    var isLoadingVersions by mutableStateOf(false)
        private set

    private var offset = 0
    private var totalHits = 0
    private val limit = 20

    val imageCache = mutableStateMapOf<String, ImageBitmap?>()

    val categoryGroups = ModrinthCategories.getGroupsFor("modpack")

    var selectedCategory by mutableStateOf<String?>(null)
        private set

    private var searchJob: Job? = null

    init {
        performSearch()
    }

    fun loadImage(url: String) {
        if (imageCache.containsKey(url)) return
        imageCache[url] = null
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
                    limit = limit,
                    offset = offset,
                    index = if (searchQuery.isEmpty()) "downloads" else "relevance",
                    projectType = "modpack",
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
                    limit = limit,
                    offset = offset,
                    index = if (searchQuery.isEmpty()) "downloads" else "relevance",
                    projectType = "modpack",
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

    fun installModpack(hit: ModrinthSearchHit) {
        MinecraftManager.startBackgroundModrinthModpackInstallById(
            projectId = hit.projectId,
            name = hit.title
        )
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
                
                loadVersions(projectId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingProject = false
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

    fun loadVersions(projectId: String) {
        isLoadingVersions = true
        viewModelScope.launch {
            try {
                modpackVersions = modrinthService.getProjectVersions(projectId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingVersions = false
            }
        }
    }

    fun installVersion(version: ModrinthVersion, title: String) {
        val file = version.files.find { it.primary } ?: version.files.first()
        MinecraftManager.startBackgroundModrinthModpackInstall(
            url = Url(file.url),
            name = title
        )
    }
}
