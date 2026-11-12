/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.minecraft.data.ModrinthAssetMetadata
import com.bartixw.anymc.minecraft.data.ModrinthInstallationMetadata
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthVersion
import com.bartixw.anymc.minecraft.services.ModrinthService
import com.bartixw.anymc.minecraft.utils.MetadataUtils
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object ModDownloadManager {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val modrinthService = ModrinthService(MinecraftManager.client)

    data class DownloadTask(
        val id: String,
        val fileName: String,
        val progress: Float = 0f,
        val status: Status = Status.QUEUED,
        val error: String? = null
    )

    enum class Status {
        QUEUED,
        DOWNLOADING,
        COMPLETED,
        FAILED
    }

    private val _tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val tasks = _tasks.asStateFlow()

    private val _onDownloadComplete = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val onDownloadComplete = _onDownloadComplete.asSharedFlow()

    fun downloadMod(
        installationId: String,
        version: ModrinthVersion,
        loader: String,
        gameVersion: String,
        projectType: String = "mod",
        isDisabled: Boolean = false
    ) {
        managerScope.launch {
            resolveAndDownload(installationId, version, loader, gameVersion, projectType = projectType, isDisabled = isDisabled)
        }
    }

    fun downloadModById(
        installationId: String,
        versionId: String,
        loader: String,
        gameVersion: String,
        projectType: String = "mod"
    ) {
        managerScope.launch {
            try {
                val version = modrinthService.getVersion(versionId)
                resolveAndDownload(installationId, version, loader, gameVersion, projectType = projectType)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun resolveAndDownload(
        installationId: String,
        version: ModrinthVersion,
        loader: String,
        gameVersion: String,
        downloadedIds: MutableSet<String> = mutableSetOf(),
        projectType: String = "mod",
        isDisabled: Boolean = false
    ) {
        if (downloadedIds.contains(version.projectId)) return
        downloadedIds.add(version.projectId)

        // Start download of current mod
        val file = version.files.find { it.primary } ?: version.files.firstOrNull()
        if (file != null) {
            managerScope.launch {
                performDownload(installationId, file.url, file.filename, version.projectId, projectType, version.id, isDisabled)
            }
        }

        // Resolve dependencies
        version.dependencies.forEach { dependency ->
            if (dependency.dependencyType == "required") {
                val depVersionId = dependency.versionId
                val depProjectId = dependency.projectId
                val depVersion = when {
                    depVersionId != null -> {
                        try {
                            modrinthService.getVersion(depVersionId!!)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    depProjectId != null -> {
                        try {
                            var versions = modrinthService.getProjectVersions(
                                depProjectId!!,
                                if (projectType == "mod") listOf(loader) else emptyList(),
                                listOf(gameVersion)
                            )
                            if (versions.isEmpty()) {
                                // Fallback: try without strict gameVersion filter for multi-version libraries
                                versions = modrinthService.getProjectVersions(
                                    depProjectId!!,
                                    if (projectType == "mod") listOf(loader) else emptyList(),
                                    emptyList()
                                )
                            }
                            versions.firstOrNull()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    else -> null
                }

                depVersion?.let {
                    resolveAndDownload(installationId, it, loader, gameVersion, downloadedIds, projectType, isDisabled = false)
                }
            }
        }
    }

    private suspend fun performDownload(
        installationId: String,
        url: String,
        fileName: String,
        projectId: String,
        projectType: String = "mod",
        versionId: String? = null,
        isDisabled: Boolean = false
    ) {
        val targetFileName = if (isDisabled && !fileName.endsWith(".disabled")) "$fileName.disabled" else fileName
        val taskId = "$installationId-$projectId"
        _tasks.update { it + (taskId to DownloadTask(taskId, targetFileName, status = Status.DOWNLOADING)) }

        try {
            val subDir = when (projectType) {
                "resourcepack" -> "resourcepacks"
                "shader" -> "shaderpacks"
                else -> "mods"
            }

            val installationDir = MinecraftManager.getDirectory(installationId)
            val targetDir = installationDir
                .resolve("gameFiles")
                .resolve(subDir)
            targetDir.mkdirs()

            val targetFile = targetDir.resolve(targetFileName)
            
            val response = MinecraftManager.client.get(url)
            val body = response.bodyAsBytes()
            
            withContext(Dispatchers.IO) {
                // Delete older version file for the same project if present
                val currentMetadata = getModrinthMetadata(installationId)
                val existingAsset = currentMetadata.assets.find { it.projectId == projectId && it.projectType == projectType }
                if (existingAsset != null) {
                    val oldFile = installationDir.resolve(existingAsset.relativePath)
                    if (oldFile.exists() && oldFile.name != targetFileName) {
                        oldFile.delete()
                    }
                }

                targetFile.writeBytes(body)
                
                // Track metadata
                if (versionId != null) {
                    MetadataUtils.trackModrinthMetadata(installationDir, ModrinthAssetMetadata(
                        filename = targetFileName,
                        versionId = versionId,
                        projectId = projectId,
                        projectType = projectType,
                        relativePath = "gameFiles/$subDir/$targetFileName"
                    ))
                }
            }

            _tasks.update { it + (taskId to it[taskId]!!.copy(status = Status.COMPLETED, progress = 1f)) }
            _onDownloadComplete.emit(installationId)
        } catch (e: Exception) {
            e.printStackTrace()
            _tasks.update { it + (taskId to it[taskId]!!.copy(status = Status.FAILED, error = e.message)) }
        }
    }

    fun getModrinthMetadata(installationId: String): ModrinthInstallationMetadata {
        return try {
            val metadataFile = MinecraftManager.getDirectory(installationId).resolve("modrinth_assets.json")
            if (metadataFile.exists()) {
                MinecraftManager.json.decodeFromString(metadataFile.readText())
            } else {
                ModrinthInstallationMetadata()
            }
        } catch (e: Exception) {
            ModrinthInstallationMetadata()
        }
    }

    fun clearCompletedTasks() {
        _tasks.update { tasks ->
            tasks.filter { it.value.status != Status.COMPLETED && it.value.status != Status.FAILED }
        }
    }
}
