/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.versions

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bartixw.anymc.minecraft.data.JavaVersion
import com.bartixw.anymc.minecraft.data.AnyMCInstallationData
import com.bartixw.anymc.minecraft.data.Installation
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthVersion
import com.bartixw.anymc.minecraft.utils.sha1
import com.bartixw.anymc.helpers.parseMinecraftWorld
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.managers.ExportManager
import com.bartixw.anymc.managers.ImportManager
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.managers.ModDownloadManager
import com.bartixw.anymc.managers.StateManager
import com.bartixw.anymc.managers.StatsManager
import com.bartixw.anymc.managers.TranslationManager
import com.bartixw.anymc.managers.DetailedWorldStats
import com.bartixw.anymc.utils.DateTimeFormatterUtil
import com.bartixw.anymc.core.utils.PlatformUtils
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.Path


class VersionsViewModel : ViewModel() {
    sealed interface UiState {
        object Loading : UiState
        data class Loaded(val installations: List<Installation>) : UiState
        data class Error(val message: String?) : UiState
    }

    var uiState by mutableStateOf<UiState>(UiState.Loading)
        private set

    var selectedInstallationId by mutableStateOf<String?>(null)
        private set

    val supportsMods by derivedStateOf {
        val installations = (uiState as? UiState.Loaded)?.installations
        val selected = installations?.find { it.id == selectedInstallationId }
        selected?.modLoader != null && selected.modLoader != "vanilla"
    }

//    var worlds by mutableStateOf<List<WorldItem>>(emptyList())
//        private set

    sealed interface WorldsState {
        object Loading : WorldsState
        object Empty : WorldsState
        data class Loaded(val worlds: List<WorldItem>) : WorldsState
        data class Error(val message: String?) : WorldsState
    }

    var screenshotsState by mutableStateOf<ScreenshotsState>(ScreenshotsState.Loading)
        private set

    sealed interface ScreenshotsState {
        object Loading : ScreenshotsState
        object Empty : ScreenshotsState
        data class Loaded(val screenshots: List<File>) : ScreenshotsState
        data class Error(val message: String?) : ScreenshotsState
    }

    var worldsState by mutableStateOf<WorldsState>(WorldsState.Loading)
        private set

    sealed interface ModsState {
        object Loading : ModsState
        object Empty : ModsState
        data class Loaded(val mods: List<ModItem>) : ModsState
        data class Error(val message: String?) : ModsState
    }

    var modsState by mutableStateOf<ModsState>(ModsState.Loading)
        private set

    sealed interface ResourcePacksState {
        object Loading : ResourcePacksState
        object Empty : ResourcePacksState
        data class Loaded(val packs: List<ModItem>) : ResourcePacksState
        data class Error(val message: String?) : ResourcePacksState
    }

    var resourcePacksState by mutableStateOf<ResourcePacksState>(ResourcePacksState.Loading)
        private set

    sealed interface ShadersState {
        object Loading : ShadersState
        object Empty : ShadersState
        data class Loaded(val shaders: List<ModItem>) : ShadersState
        data class Error(val message: String?) : ShadersState
    }

    var shadersState by mutableStateOf<ShadersState>(ShadersState.Loading)
        private set

    sealed interface StatsState {
        object Loading : StatsState
        object Empty : StatsState
        data class Loaded(val stats: List<DetailedWorldStats>) : StatsState
        data class Error(val message: String?) : StatsState
    }

    var statsState by mutableStateOf<StatsState>(StatsState.Loading)
        private set

    data class WorldItem(
        val id: String,
        val name: String,
        val gameType: Int,
        val lastPlayed: Long
    )

    data class ModItem(
        val fileName: String,
        val name: String,
        val version: String,
        val enabled: Boolean,
        val iconPath: String? = "NULL",
        val updateVersion: ModrinthVersion? = null,
        val projectId: String? = null
    ) {
        val hasUpdate: Boolean get() = updateVersion != null
    }

    @Serializable
    private data class FabricModMetadata(
        val id: String = "",
        val name: String? = null,
        val version: String = "",
        val icon: JsonElement? = null
    )

    init {
        selectedInstallationId = StateManager.data.value.lastSelectedInstanceId
        selectedInstallationId?.let { 
            loadWorlds(it, ConfigManager.data.value.devMode)
            loadMods(it)
            loadResourcePacks(it)
            loadShaders(it)
            loadStats(it)
            loadScreenshots(it)
        }

        viewModelScope.launch {
            ModDownloadManager.onDownloadComplete.collect { id ->
                if (id == selectedInstallationId) {
                    loadMods(id)
                    loadResourcePacks(id)
                    loadShaders(id)
                }
            }
        }

        viewModelScope.launch {
            MinecraftManager.installationsVersion.collect {
                loadVersions(showLoading = false)
            }
        }

        viewModelScope.launch {
            ConfigManager.data.map { it.language }.distinctUntilChanged().collect {
                selectedInstallationId?.let { loadStats(it) }
            }
        }
    }

    fun selectInstallation(id: String, name: String) {
        selectedInstallationId = id
        StateManager.update {
            it.copy(
                lastSelectedInstanceId = id,
                lastSelectedInstanceString = name
            )
        }
        loadWorlds(id, ConfigManager.data.value.devMode)
        loadMods(id)
        loadResourcePacks(id)
        loadShaders(id)
        loadStats(id)
    }

    fun loadVersions(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) uiState = UiState.Loading
            try {
                val installations = withContext(Dispatchers.IO) {
                    MinecraftManager.getVersions()
                }
                uiState = UiState.Loaded(installations)
            } catch (e: Exception) {
                e.printStackTrace()
                uiState = UiState.Error(e.toString())
            }
        }
    }

    fun loadWorlds(installationId: String, showIds: Boolean) {
        viewModelScope.launch {
            worldsState = WorldsState.Loading
            try {
                val saves = withContext(Dispatchers.IO) {
                    coroutineScope {
                        val savesDir = MinecraftManager.getDirectory(installationId)
                            .resolve("gameFiles")
                            .resolve("saves")

                        if (!savesDir.exists() || !savesDir.isDirectory) {
                            return@coroutineScope null
                        }

                        savesDir.listFiles()
                            ?.filter { it.isDirectory }
                            ?.map { file ->
                                async {
                                    parseMinecraftWorld(file)?.let { info ->
                                        WorldItem(
                                            id = file.name,
                                            name = info.name,
                                            gameType = info.gameType,
                                            lastPlayed = info.lastPlayed
                                        )
                                    }
                                }
                            }
                            ?.awaitAll()
                            ?.filterNotNull()
                    }
                }

                worldsState = if (saves.isNullOrEmpty()) {
                    WorldsState.Empty
                } else {
                    WorldsState.Loaded(saves)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                worldsState = WorldsState.Error(e.toString())
            }
        }
    }

    fun loadMods(installationId: String) {
        viewModelScope.launch {
            modsState = ModsState.Loading
            try {
                val modsList = withContext(Dispatchers.IO) {
                    val modsDir = MinecraftManager.getDirectory(installationId)
                        .resolve("gameFiles")
                        .resolve("mods")

                    if (!modsDir.exists() || !modsDir.isDirectory) {
                        return@withContext emptyList()
                    }

                    val iconCacheDir = PlatformUtils.sharedStorageDirectory
                        .resolve("cache")
                        .resolve("modicons")
                    iconCacheDir.mkdirs()

                    modsDir.listFiles()
                        ?.filter { it.isFile && (it.name.endsWith(".jar") || it.name.endsWith(".jar.disabled")) }
                        ?.map { file ->
                            val enabled = file.name.endsWith(".jar")
                            var modName = file.name
                            var modVersion = ""
                            var iconPath: String? = null

                            try {
                                java.util.zip.ZipFile(file).use { zipFile ->
                                    val metadataEntry = zipFile.getEntry("fabric.mod.json")
                                    if (metadataEntry != null) {
                                        val metadata = MinecraftManager.json.decodeFromString<FabricModMetadata>(
                                            zipFile.getInputStream(metadataEntry).bufferedReader().use { it.readText() }
                                        )
                                        modName = metadata.name ?: metadata.id
                                        modVersion = metadata.version

                                        val iconJson = metadata.icon
                                        val iconPathInJar = when (iconJson) {
                                            null -> null
                                            is kotlinx.serialization.json.JsonPrimitive -> iconJson.content
                                            is kotlinx.serialization.json.JsonObject -> {
                                                iconJson.values.firstOrNull()?.jsonPrimitive?.content
                                            }

                                            else -> null
                                        }

                                        if (iconPathInJar != null) {
                                            val cacheFile = iconCacheDir.resolve("${metadata.id}-${metadata.version}.png")
                                            if (!cacheFile.exists()) {
                                                val iconEntry = zipFile.getEntry(iconPathInJar)
                                                    ?: zipFile.getEntry(iconPathInJar.removePrefix("/"))

                                                if (iconEntry != null) {
                                                    zipFile.getInputStream(iconEntry).use { input ->
                                                        cacheFile.writeBytes(input.readAllBytes())
                                                    }
                                                }
                                            }
                                            if (cacheFile.exists()) {
                                                iconPath = cacheFile.absolutePath
                                            }
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                // Ignore parsing errors, use filename
                            }

                            ModItem(
                                fileName = file.name,
                                name = modName,
                                version = modVersion,
                                enabled = enabled,
                                iconPath = iconPath
                            )
                        } ?: emptyList()
                }

                if (modsList.isEmpty()) {
                    modsState = ModsState.Empty
                } else {
                    val sortedList = modsList.sortedBy { it.name.lowercase() }
                    modsState = ModsState.Loaded(sortedList)
                    checkModUpdates(installationId, sortedList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                modsState = ModsState.Error(e.toString())
            }
        }
    }

    fun loadScreenshots(installationId: String) {
        val screenshotsDir = MinecraftManager.getDirectory(installationId)
            .resolve("gameFiles")
            .resolve("screenshots")

        try {
            val files = screenshotsDir.listFiles() ?: return

            if (files.isEmpty()) {
                screenshotsState = ScreenshotsState.Empty
                return
            }

            screenshotsState = ScreenshotsState.Loaded(files.toList())
        } catch(e: Exception) {
            screenshotsState = ScreenshotsState.Error(e.toString())
        }
    }

    private var checkUpdatesJob: Job? = null

    private fun checkModUpdates(installationId: String, currentMods: List<ModItem>) {
        checkUpdatesJob?.cancel()
        checkUpdatesJob = viewModelScope.launch {
            try {
                val installation = (uiState as? UiState.Loaded)?.installations?.find { it.id == installationId }
                    ?: return@launch

                val rawLoader = installation.modLoader
                if (rawLoader == "vanilla") return@launch

                val loader = if (rawLoader == "legacyfabric") "legacy-fabric" else rawLoader
                val gameVersion = installation.version

                val modsDir = MinecraftManager.getDirectory(installationId)
                    .resolve("gameFiles")
                    .resolve("mods")

                if (!modsDir.exists()) return@launch

                val metadata = ModDownloadManager.getModrinthMetadata(installationId)
                val metadataByRelPath = metadata.assets.associateBy { it.relativePath }

                val modHashMap = withContext(Dispatchers.IO) {
                    currentMods.mapNotNull { mod ->
                        val file = modsDir.resolve(mod.fileName)
                        if (file.exists()) {
                            try {
                                val hash = file.sha1()
                                hash to mod
                            } catch (_: Exception) { null }
                        } else null
                    }
                }

                if (modHashMap.isEmpty()) return@launch

                val hashes = modHashMap.map { it.first }
                val latestByHash = try {
                    MinecraftManager.modrinthService.getLatestVersionsFromHashes(
                        hashes = hashes,
                        algorithm = "sha1",
                        loaders = listOf(loader),
                        gameVersions = listOf(gameVersion)
                    )
                } catch (e: Exception) {
                    emptyMap()
                }

                val updatedMods = currentMods.map { mod ->
                    val hash = modHashMap.find { it.second.fileName == mod.fileName }?.first
                    val relPath = "gameFiles/mods/${mod.fileName}"
                    val assetMeta = metadataByRelPath[relPath]

                    var latestVersion: ModrinthVersion? = null

                    if (hash != null && latestByHash.containsKey(hash)) {
                        val candidate = latestByHash[hash]
                        if (candidate != null) {
                            val isSameHash = candidate.files.any { it.hashes["sha1"] == hash }
                            if (!isSameHash) {
                                latestVersion = candidate
                            }
                        }
                    }

                    if (latestVersion == null && assetMeta != null) {
                        try {
                            val versions = MinecraftManager.modrinthService.getProjectVersions(
                                projectId = assetMeta.projectId,
                                loaders = listOf(loader),
                                gameVersions = listOf(gameVersion)
                            )
                            val top = versions.firstOrNull()
                            if (top != null && top.id != assetMeta.versionId) {
                                latestVersion = top
                            }
                        } catch (_: Exception) {}
                    }

                    if (latestVersion != null) {
                        mod.copy(
                            updateVersion = latestVersion,
                            projectId = latestVersion.projectId
                        )
                    } else {
                        mod.copy(
                            projectId = assetMeta?.projectId
                        )
                    }
                }

                if (modsState is ModsState.Loaded && selectedInstallationId == installationId) {
                    modsState = ModsState.Loaded(updatedMods)
                }

                // Check for mod loader updates (e.g. Fabric Loader)
                try {
                    val configFile = MinecraftManager.getDirectory(installationId).resolve("AnyMC.json")
                    if (configFile.exists()) {
                        val installationData = MinecraftManager.json.decodeFromString<AnyMCInstallationData>(configFile.readText())
                        val loaderType = installationData.loaderType
                        val currentLoaderVer = installationData.loaderVersion

                        if (loaderType == "fabric" || loaderType == "legacyfabric") {
                            val service = if (loaderType == "legacyfabric") MinecraftManager.legacyFabricMetadataService else MinecraftManager.fabricMetadataService
                            val loaderVersions = service.getLoaderVersionsForGame(gameVersion)
                            val latestLoader = loaderVersions.firstOrNull { it.stable } ?: loaderVersions.firstOrNull()
                            if (latestLoader != null && currentLoaderVer != null && latestLoader.version != currentLoaderVer) {
                                loaderUpdateVersion = latestLoader.version
                            }
                        }
                    }
                } catch (_: Exception) {}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var loaderUpdateVersion by mutableStateOf<String?>(null)
        private set
    val hasLoaderUpdate: Boolean get() = loaderUpdateVersion != null

    var updateWarnings by mutableStateOf<List<String>>(emptyList())
        private set

    var showUpdateDialog by mutableStateOf(false)
        private set

    fun dismissUpdateDialog() {
        showUpdateDialog = false
        updateWarnings = emptyList()
    }

    fun updateMods(installationId: String) {
        startUpdateMods(installationId)
    }

    fun startUpdateMods(installationId: String) {
        if (MinecraftManager.updatingInstallations.value.isNotEmpty()) return

        val currentState = modsState as? ModsState.Loaded
        val modsToUpdate = currentState?.mods?.filter { it.hasUpdate } ?: emptyList()
        if (modsToUpdate.isEmpty() && !hasLoaderUpdate) return

        val warnings = mutableListOf<String>()
        val installedProjectIds = currentState?.mods?.mapNotNull { it.projectId }?.toSet() ?: emptySet()

        modsToUpdate.forEach { mod ->
            mod.updateVersion?.dependencies?.forEach { dep ->
                if (dep.dependencyType == "incompatible") {
                    val incompatibleProjId = dep.projectId
                    if (incompatibleProjId != null && installedProjectIds.contains(incompatibleProjId)) {
                        val conflictingMod = currentState?.mods?.find { it.projectId == incompatibleProjId }
                        warnings.add("New version of '${mod.name}' is marked as incompatible with installed mod '${conflictingMod?.name ?: incompatibleProjId}'.")
                    }
                }
            }
        }

        if (warnings.isNotEmpty()) {
            updateWarnings = warnings
            showUpdateDialog = true
        } else {
            performUpdateMods(installationId)
        }
    }

    fun performUpdateMods(installationId: String) {
        showUpdateDialog = false
        updateWarnings = emptyList()

        val currentState = modsState as? ModsState.Loaded
        val modsToUpdate = currentState?.mods?.filter { it.hasUpdate } ?: emptyList()
        if (modsToUpdate.isEmpty() && !hasLoaderUpdate) return

        val installation = (uiState as? UiState.Loaded)?.installations?.find { it.id == installationId } ?: return
        val rawLoader = installation.modLoader
        val loader = if (rawLoader == "legacyfabric") "legacy-fabric" else rawLoader
        val gameVersion = installation.version
        val targetLoaderVer = loaderUpdateVersion

        MinecraftManager.addUpdatingInstallation(installationId)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val installationDir = MinecraftManager.getDirectory(installationId)
                    val modsDir = installationDir
                        .resolve("gameFiles")
                        .resolve("mods")

                    // Update mod loader if an update is available
                    if (targetLoaderVer != null && (rawLoader == "fabric" || rawLoader == "legacyfabric")) {
                        try {
                            val manifest = MinecraftManager.getVersionManifest()
                            val mcVersionEntry = manifest.versions.find { it.id == gameVersion }
                            if (mcVersionEntry != null) {
                                val mcUrl = io.ktor.http.Url(mcVersionEntry.url)
                                MinecraftManager.installFabric(
                                    mcUrl,
                                    targetLoaderVer,
                                    installation.name,
                                    isLegacy = (rawLoader == "legacyfabric"),
                                    overrideBaseDir = installationDir
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (modsDir.exists()) {
                        val backupDir = installationDir
                            .resolve("gameFiles")
                            .resolve("mods_backup_latest")
                        try {
                            if (backupDir.exists()) backupDir.deleteRecursively()
                            backupDir.mkdirs()
                            modsDir.listFiles()
                                ?.filter { it.isFile && (it.name.endsWith(".jar") || it.name.endsWith(".jar.disabled")) }
                                ?.forEach { file -> file.copyTo(backupDir.resolve(file.name), overwrite = true) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    modsToUpdate.forEach { mod ->
                        val oldFile = modsDir.resolve(mod.fileName)
                        if (oldFile.exists()) {
                            oldFile.delete()
                        }
                        mod.updateVersion?.let { newVersion ->
                            ModDownloadManager.downloadMod(
                                installationId = installationId,
                                version = newVersion,
                                loader = loader,
                                gameVersion = gameVersion,
                                projectType = "mod",
                                isDisabled = !mod.enabled
                            )
                        }
                    }
                }

                // Wait until all download tasks for this installation are completed
                while (true) {
                    kotlinx.coroutines.delay(500)
                    val tasks = ModDownloadManager.tasks.value.filter { it.key.startsWith("$installationId-") }
                    val anyActive = tasks.values.any { it.status == ModDownloadManager.Status.QUEUED || it.status == ModDownloadManager.Status.DOWNLOADING }
                    if (!anyActive) break
                }

                loaderUpdateVersion = null
            } finally {
                MinecraftManager.removeUpdatingInstallation(installationId)
                loadMods(installationId)
            }
        }
    }

    fun restoreBackup(installationId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val installationDir = MinecraftManager.getDirectory(installationId)
                    val modsDir = installationDir.resolve("gameFiles").resolve("mods")
                    val backupDir = installationDir.resolve("gameFiles").resolve("mods_backup_latest")

                    if (backupDir.exists() && backupDir.isDirectory) {
                        modsDir.mkdirs()
                        modsDir.listFiles()?.forEach { it.delete() }
                        backupDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                file.copyTo(modsDir.resolve(file.name), overwrite = true)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadMods(installationId)
        }
    }

    fun loadResourcePacks(installationId: String) {
        viewModelScope.launch {
            resourcePacksState = ResourcePacksState.Loading
            try {
                val packsList = withContext(Dispatchers.IO) {
                    val packsDir = MinecraftManager.getDirectory(installationId)
                        .resolve("gameFiles")
                        .resolve("resourcepacks")

                    if (!packsDir.exists() || !packsDir.isDirectory) {
                        return@withContext emptyList()
                    }

                    val iconCacheDir = PlatformUtils.sharedStorageDirectory
                        .resolve("cache")
                        .resolve("packicons")
                    iconCacheDir.mkdirs()

                    packsDir.listFiles()
                        ?.filter { it.isFile && (it.name.endsWith(".zip") || it.name.endsWith(".zip.disabled")) }
                        ?.map { file ->
                            var iconPath: String? = null
                            try {
                                java.util.zip.ZipFile(file).use { zipFile ->
                                    val iconEntry = zipFile.getEntry("pack.png")
                                    if (iconEntry != null) {
                                        val cacheFile = iconCacheDir.resolve("${file.name}.png")
                                        if (!cacheFile.exists()) {
                                            zipFile.getInputStream(iconEntry).use { input ->
                                                cacheFile.writeBytes(input.readAllBytes())
                                            }
                                        }
                                        if (cacheFile.exists()) {
                                            iconPath = cacheFile.absolutePath
                                        }
                                    }
                                }
                            } catch (_: Exception) {}

                            ModItem(
                                fileName = file.name,
                                name = file.name.removeSuffix(".zip").removeSuffix(".zip.disabled"),
                                version = "",
                                enabled = !file.name.endsWith(".disabled"),
                                iconPath = iconPath
                            )
                        } ?: emptyList()
                }
                resourcePacksState = if (packsList.isEmpty()) ResourcePacksState.Empty
                else ResourcePacksState.Loaded(packsList.sortedBy { it.name.lowercase() })
            } catch (e: Exception) {
                e.printStackTrace()
                resourcePacksState = ResourcePacksState.Error(e.toString())
            }
        }
    }

    fun loadShaders(installationId: String) {
        viewModelScope.launch {
            shadersState = ShadersState.Loading
            try {
                val shadersList = withContext(Dispatchers.IO) {
                    val shadersDir = MinecraftManager.getDirectory(installationId)
                        .resolve("gameFiles")
                        .resolve("shaderpacks")

                    if (!shadersDir.exists() || !shadersDir.isDirectory) {
                        return@withContext emptyList()
                    }

                    shadersDir.listFiles()
                        ?.filter { it.isFile && (it.name.endsWith(".zip") || it.name.endsWith(".zip.disabled")) }
                        ?.map { file ->
                            ModItem(
                                fileName = file.name,
                                name = file.name.removeSuffix(".zip").removeSuffix(".zip.disabled"),
                                version = "",
                                enabled = !file.name.endsWith(".disabled"),
                                iconPath = "NULL"
                            )
                        } ?: emptyList()
                }
                shadersState = if (shadersList.isEmpty()) ShadersState.Empty
                else ShadersState.Loaded(shadersList.sortedBy { it.name.lowercase() })
            } catch (e: Exception) {
                e.printStackTrace()
                shadersState = ShadersState.Error(e.toString())
            }
        }
    }

    fun loadStats(installationId: String) {
        viewModelScope.launch {
            statsState = StatsState.Loading
            try {
                val activeAccount = AccountManager.activeAccount
                if (activeAccount == null) {
                    statsState = StatsState.Empty
                    return@launch
                }

                val stats = withContext(Dispatchers.IO) {
                    val instDir = MinecraftManager.getDirectory(installationId)
                    TranslationManager.loadTranslationsFromInstallation(instDir)
                    StatsManager.getDetailedStatsForInstallation(installationId, activeAccount.uuid)
                }

                statsState = if (stats.isEmpty()) {
                    StatsState.Empty
                } else {
                    StatsState.Loaded(stats)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                statsState = StatsState.Error(e.toString())
            }
        }
    }

    fun deleteGeneric(installationId: String, item: ModItem, subDir: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val dir = MinecraftManager.getDirectory(installationId)
                        .resolve("gameFiles")
                        .resolve(subDir)
                    val file = dir.resolve(item.fileName)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            when (subDir) {
                "mods" -> loadMods(installationId)
                "resourcepacks" -> loadResourcePacks(installationId)
                "shaderpacks" -> loadShaders(installationId)
            }
        }
    }

    fun toggleGeneric(installationId: String, item: ModItem, subDir: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val dir = MinecraftManager.getDirectory(installationId)
                        .resolve("gameFiles")
                        .resolve(subDir)
                    val oldFile = dir.resolve(item.fileName)
                    val newFileName = if (item.enabled) {
                        "${item.fileName}.disabled"
                    } else {
                        item.fileName.removeSuffix(".disabled")
                    }
                    val newFile = dir.resolve(newFileName)
                    if (oldFile.exists()) oldFile.renameTo(newFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            when (subDir) {
                "mods" -> loadMods(installationId)
                "resourcepacks" -> loadResourcePacks(installationId)
                "shaderpacks" -> loadShaders(installationId)
            }
        }
    }

    fun addMod(installationId: String) {
        addGeneric(installationId, "mods")
    }

    fun addResourcePack(installationId: String) {
        addGeneric(installationId, "resourcepacks")
    }

    fun addShader(installationId: String) {
        addGeneric(installationId, "shaderpacks")
    }

    private fun addGeneric(installationId: String, subDir: String) {
        viewModelScope.launch {
            val file = FileKit.pickFile(
                type = PickerType.File(extensions = listOf("jar", "zip")),
                mode = PickerMode.Single
            ) ?: return@launch

            val path = file.path ?: return@launch
            copyGenericToInstallation(installationId, listOf(File(path)), subDir)
        }
    }

    fun addGenericFromPaths(installationId: String, uris: List<String>, subDir: String) {
        viewModelScope.launch {
            val files = uris.mapNotNull { uri ->
                try {
                    val file = if (uri.startsWith("file:")) {
                        File(java.net.URI(uri))
                    } else {
                        File(uri)
                    }
                    file.takeIf { it.exists() && (it.name.endsWith(".jar") || it.name.endsWith(".zip")) }
                } catch (e: Exception) {
                    null
                }
            }
            if (files.isNotEmpty()) {
                copyGenericToInstallation(installationId, files, subDir)
            }
        }
    }

    fun addModsFromPaths(installationId: String, uris: List<String>) {
        addGenericFromPaths(installationId, uris, "mods")
    }

    private suspend fun copyGenericToInstallation(installationId: String, files: List<File>, subDir: String) {
        withContext(Dispatchers.IO) {
            try {
                val targetDir = MinecraftManager.getDirectory(installationId)
                    .resolve("gameFiles")
                    .resolve(subDir)
                targetDir.mkdirs()

                files.forEach { sourceFile ->
                    val targetFile = targetDir.resolve(sourceFile.name)
                    sourceFile.copyTo(targetFile, overwrite = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        when (subDir) {
            "mods" -> loadMods(installationId)
            "resourcepacks" -> loadResourcePacks(installationId)
            "shaderpacks" -> loadShaders(installationId)
        }
    }

    fun deleteMod(installationId: String, mod: ModItem) {
        deleteGeneric(installationId, mod, "mods")
    }

    fun toggleMod(installationId: String, mod: ModItem) {
        toggleGeneric(installationId, mod, "mods")
    }

    fun loadWorldFromZip(installationId: String) {
        viewModelScope.launch {
            val fileKitFile = FileKit.pickFile(
                type = PickerType.File(extensions = listOf("zip")),
                mode = PickerMode.Single
            ) ?: return@launch

            val path = fileKitFile.path ?: return@launch

            withContext(Dispatchers.IO) {
                try {
                    val zipNameWithoutExtension = File(path).nameWithoutExtension
                    val savesDir = PlatformUtils.userDataDirectory
                        .resolve("installations")
                        .resolve(installationId)
                        .resolve("gameFiles")
                        .resolve("saves")

                    var targetWorldDir = savesDir.resolve(zipNameWithoutExtension)

                    if (targetWorldDir.exists()) {
                        targetWorldDir = savesDir.resolve("$zipNameWithoutExtension-${Instant.now().epochSecond}")
                    }

                    Files.newInputStream(Path(path)).use { fileStream ->
                        ZipInputStream(fileStream).use { zipInputStream ->
                            var zipEntry: ZipEntry? = zipInputStream.nextEntry

                            while (zipEntry != null) {
                                val outputFile = targetWorldDir.resolve(zipEntry.name)

                                if (zipEntry.isDirectory) {
                                    outputFile.mkdirs()
                                } else {
                                    outputFile.parentFile?.mkdirs()

                                    val bytes = zipInputStream.readAllBytes()
                                    outputFile.writeBytes(bytes)
                                }

                                zipInputStream.closeEntry()
                                zipEntry = zipInputStream.nextEntry
                            }
                        }
                    }

                    loadWorlds(installationId, ConfigManager.data.value.devMode)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteInstallation(id: String, onDeleted: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    MinecraftManager.deleteVersion(id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            if (success) {
                val currentState = uiState
                if (currentState is UiState.Loaded) {
                    val currentList = currentState.installations

                    val deletedIndex = currentList.indexOfFirst { it.id == id }

                    val updatedList = currentList.filter { it.id != id }
                    uiState = UiState.Loaded(updatedList)

                    if (selectedInstallationId == id) {
                        if (updatedList.isNotEmpty() && deletedIndex != -1) {
                            val newSelectedIndex = deletedIndex.coerceAtMost(updatedList.lastIndex)
                            val newSelectedId = updatedList[newSelectedIndex].id
                            val newSelectedName = updatedList[newSelectedIndex].name

                            selectInstallation(
                                id = newSelectedId,
                                name = newSelectedName
                            )
                        } else {
                            selectedInstallationId = null
                            worldsState = WorldsState.Empty
                            StateManager.update {
                                it.copy(
                                    lastSelectedInstanceId = null,
                                    lastSelectedInstanceString = null
                                )
                            }
                        }
                    }
                }

                onDeleted()
            } else {
                onError()
            }
        }
    }

    fun renameInstallation(id: String, newName: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                MinecraftManager.renameInstallation(id, newName)
            }

            if (success) {
                if (StateManager.data.value.lastSelectedInstanceId == id) {
                    StateManager.update {
                        it.copy(lastSelectedInstanceString = newName)
                    }
                }
            }
            onComplete()
        }
    }

    fun getInstallationData(id: String, onLoaded: (AnyMCInstallationData?) -> Unit) {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                try {
                    val installationFile = PlatformUtils.userDataDirectory
                        .resolve("installations")
                        .resolve(id)
                        .resolve("AnyMC.json")

                    if (!installationFile.exists()) return@withContext null

                    MinecraftManager.json.decodeFromString<AnyMCInstallationData>(
                        installationFile.readText()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            onLoaded(data)
        }
    }

    fun updateInstallationSettings(
        id: String,
        jvmArgs: List<String>,
        removedArgs: List<String>,
        javaVersion: JavaVersion?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val installationFile = PlatformUtils.userDataDirectory
                        .resolve("installations")
                        .resolve(id)
                        .resolve("AnyMC.json")

                    if (!installationFile.exists()) return@withContext

                    val data = MinecraftManager.json.decodeFromString<AnyMCInstallationData>(
                        installationFile.readText()
                    )

                    val updatedData = data.copy(jvmArgs = jvmArgs, removedArgs = removedArgs, javaVersion = javaVersion)
                    val json = MinecraftManager.json.encodeToString(updatedData)
                    installationFile.writeText(json)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onComplete()
        }
    }

    fun exportInstallation(id: String) {
        viewModelScope.launch {
            val installation = (uiState as? UiState.Loaded)?.installations?.find { it.id == id } ?: return@launch
            
            val bytes = withContext(Dispatchers.IO) {
                try {
                    val tempZip = File.createTempFile("anymc_export_", ".zip")
                    ExportManager.exportInstallation(id, tempZip)
                    val bytes = tempZip.readBytes()
                    tempZip.delete()
                    bytes
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            
            if (bytes != null) {
                FileKit.saveFile(
                    bytes = bytes,
                    baseName = "AnyMC-${installation.name}",
                    extension = "zip"
                )
            }
        }
    }

    fun importInstallation(file: File, newName: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = ImportManager.importInstallation(file, newName)
            onComplete(success)
        }
    }

    fun getAvailableJavaRuntimes(onLoaded: (List<JavaVersion>) -> Unit) {
        viewModelScope.launch {
            val runtimes = MinecraftManager.getAvailableJavaRuntimes()
            onLoaded(runtimes)
        }
    }
}
