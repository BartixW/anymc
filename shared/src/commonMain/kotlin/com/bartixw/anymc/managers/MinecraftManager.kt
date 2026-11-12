/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.core.HookRegistry
import com.bartixw.anymc.core.Pipeline
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.minecraft.*
import com.bartixw.anymc.minecraft.InstallLogic.getCacheFile
import com.bartixw.anymc.minecraft.data.*
import com.bartixw.anymc.minecraft.services.*
import com.bartixw.anymc.minecraft.steps.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object MinecraftManager {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _installationsVersion = MutableStateFlow(0L)
    val installationsVersion = _installationsVersion.asStateFlow()

    fun notifyInstallationsChanged() {
        managerScope.launch {
            _installationsVersion.update { it + 1 }
        }
    }

    init {
        // UI status updates via hooks
        HookRegistry.hookBefore("anymc:launch/initialize", "anymc:ui/minecraftStatus/initialize") { _minecraftStatus.update { MinecraftStatus.LAUNCHING } }
        HookRegistry.hookBefore("anymc:install/fetchManifest", "anymc:ui/downloadStatus/fetchManifest") { _downloadStatus.update { it.copy(type = StatusType.INFO_FETCH) } }
        HookRegistry.hookBefore("anymc:install/downloadClient", "anymc:ui/downloadStatus/downloadClient") { _downloadStatus.update { it.copy(type = StatusType.CLIENT_DL) } }
        HookRegistry.hookBefore("anymc:install/downloadLibraries", "anymc:ui/downloadStatus/downloadLibraries") { _downloadStatus.update { it.copy(type = StatusType.LIBRARY_DL) } }
        HookRegistry.hookBefore("anymc:install/downloadAssets", "anymc:ui/downloadStatus/downloadAssets") { _downloadStatus.update { it.copy(type = StatusType.ASSET_FETCH) } }
        HookRegistry.hookAfter("anymc:install/ensureJava", "anymc:ui/downloadStatus/ensureJava") { _downloadStatus.update { it.copy(type = StatusType.IDLE) } }
    }

    val json = Json { ignoreUnknownKeys = true; allowStructuredMapKeys = true }
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) { requestTimeoutMillis = 60000; connectTimeoutMillis = 20000; socketTimeoutMillis = 60000 }
        install(HttpRequestRetry) { retryOnExceptionOrServerErrors(maxRetries = 5); exponentialDelay() }
    }

    val fabricMetadataService = FabricMetadataService(client)
    val legacyFabricMetadataService = FabricMetadataService(client, "https://meta.legacyfabric.net/v2")
    val forgeMetadataService = MultiMCMetadataService(client, "net.minecraftforge")
    val neoforgeMetadataService = MultiMCMetadataService(client, "net.neoforged")
    val modrinthService = ModrinthService(client)

    private val _downloadStatus = MutableStateFlow(DownloadStatus(StatusType.IDLE, 0, 0))
    val downloadStatus = _downloadStatus.asStateFlow()

    private var gameProcess: Process? = null
    private val _minecraftStatus = MutableStateFlow(MinecraftStatus.READY)
    val minecraftStatus = _minecraftStatus.asStateFlow()

    private val _lastCrashLog = MutableStateFlow<String?>(null)
    val lastLog: StateFlow<String?> = _lastCrashLog.asStateFlow()

    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    private val _updatingInstallations = MutableStateFlow<Set<String>>(emptySet())
    val updatingInstallations = _updatingInstallations.asStateFlow()

    fun addUpdatingInstallation(id: String) {
        _updatingInstallations.update { it + id }
    }

    fun removeUpdatingInstallation(id: String) {
        _updatingInstallations.update { it - id }
    }

    suspend fun getVersionManifest(): VersionManifest = client.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").body()

    fun startBackgroundVanillaInstall(url: Url, name: String) = managerScope.launch { installVanilla(url, name) }
    fun startBackgroundFabricInstall(vUrl: Url, lv: String, n: String, leg: Boolean = false) = managerScope.launch { installFabric(vUrl, lv, n, leg) }
    fun startBackgroundMultiMCInstall(vUrl: Url, lt: String, lv: String, n: String) = managerScope.launch { installMultiMCModLoader(vUrl, lt, lv, n) }
    fun startBackgroundModrinthModpackInstall(url: Url, name: String) = managerScope.launch { installModrinthModpack(url, name) }

    fun startBackgroundModrinthModpackInstallById(projectId: String, name: String) = managerScope.launch {
        try {
            _downloadStatus.update { it.copy(type = StatusType.INFO_FETCH) }
            val v = modrinthService.getProjectVersions(projectId).firstOrNull() ?: throw Exception("No versions")
            val f = v.files.find { it.primary } ?: v.files.first()
            installModrinthModpack(Url(f.url), name)
        } catch (e: Exception) { _downloadStatus.update { it.copy(type = StatusType.IDLE, errorCode = e.toString()) } }
    }

    suspend fun installVanilla(url: Url, name: String, overrideBaseDir: File? = null) {
        try {
            val context = InstallContext(client, name, url, overrideBaseDir = overrideBaseDir)
            val pipeline = Pipeline<InstallContext>("anymc:install")
            
            pipeline.addStep(InitializeInstallStep())
            pipeline.addStep(FetchManifestStep(client, json))
            pipeline.addStep(DownloadClientStep(client))
            pipeline.addStep(DownloadLibrariesStep(client) { cur, total -> 
                _downloadStatus.update { it.copy(current = cur, total = total) } 
            })
            pipeline.addStep(DownloadAuthlibInjector(client) { checkAuthlibInjector() })
            pipeline.addStep(DownloadAssetsStep(client) { cur, total -> 
                _downloadStatus.update { it.copy(type = StatusType.ASSET_DL, current = cur, total = total) } 
            })
            pipeline.addStep(SetupJavaStep(client) { type, cur, total -> 
                _downloadStatus.update { it.copy(type = type, current = cur, total = total) } 
            })
            pipeline.addStep(WriteMetadataStep(json) { 
                _downloadStatus.update { it.copy(type = StatusType.IDLE) } 
                notifyInstallationsChanged()
            })

            pipeline.execute(context)
        } catch (e: Exception) { _downloadStatus.update { it.copy(type = StatusType.IDLE, errorCode = e.toString()) } }
    }

    suspend fun installFabric(vUrl: Url, loaderVersion: String, name: String, isLegacy: Boolean = false, overrideBaseDir: File? = null) {
        try {
            val context = InstallContext(client, name, vUrl, loaderType = if (isLegacy) "legacyfabric" else "fabric", loaderVersion = loaderVersion, isLegacy = isLegacy, overrideBaseDir = overrideBaseDir)
            val pipeline = Pipeline<InstallContext>("anymc:install")
            
            pipeline.addStep(InitializeInstallStep())
            pipeline.addStep(FetchFabricManifestStep(client, json))
            pipeline.addStep(DownloadClientStep(client))
            pipeline.addStep(DownloadFabricLibrariesStep(client, json) { cur, total -> 
                _downloadStatus.update { it.copy(current = cur, total = total) } 
            })
            pipeline.addStep(DownloadAuthlibInjector(client) { checkAuthlibInjector() })
            pipeline.addStep(DownloadAssetsStep(client) { cur, total -> 
                _downloadStatus.update { it.copy(type = StatusType.ASSET_DL, current = cur, total = total) } 
            })
            pipeline.addStep(SetupJavaStep(client) { type, cur, total -> 
                _downloadStatus.update { it.copy(type = type, current = cur, total = total) } 
            })
            pipeline.addStep(WriteMetadataStep(json) { 
                _downloadStatus.update { it.copy(type = StatusType.IDLE) } 
                notifyInstallationsChanged()
            })

            pipeline.execute(context)
        } catch (e: Exception) { _downloadStatus.update { it.copy(type = StatusType.IDLE, errorCode = e.toString()) } }
    }

    suspend fun installMultiMCModLoader(vUrl: Url, loaderType: String, loaderVersion: String, name: String, overrideBaseDir: File? = null) {
        try {
            val context = InstallContext(client, name, vUrl, loaderType = loaderType, loaderVersion = loaderVersion, overrideBaseDir = overrideBaseDir)
            val pipeline = Pipeline<InstallContext>("anymc:install")
            
            pipeline.addStep(InitializeInstallStep())
            pipeline.addStep(FetchMultiMCManifestStep(client, json))
            pipeline.addStep(DownloadClientStep(client))
            pipeline.addStep(DownloadMultiMCLibrariesStep(client, json) { cur, total -> 
                _downloadStatus.update { it.copy(current = cur, total = total) } 
            })
            pipeline.addStep(DownloadAuthlibInjector(client) { checkAuthlibInjector() })
            pipeline.addStep(DownloadAssetsStep(client) { cur, total -> 
                _downloadStatus.update { it.copy(type = StatusType.ASSET_DL, current = cur, total = total) } 
            })
            pipeline.addStep(SetupJavaStep(client) { type, cur, total -> 
                _downloadStatus.update { it.copy(type = type, current = cur, total = total) } 
            })
            pipeline.addStep(WriteMetadataStep(json) { 
                _downloadStatus.update { it.copy(type = StatusType.IDLE) } 
                notifyInstallationsChanged()
            })

            pipeline.execute(context)
        } catch (e: Exception) { _downloadStatus.update { it.copy(type = StatusType.IDLE, errorCode = e.toString()) } }
    }

    suspend fun installModrinthModpack(url: Url, name: String) {
        try {
            val context = InstallContext(client, name, url)
            val pipeline = Pipeline<InstallContext>("anymc:install")
            
            pipeline.addStep(InitializeInstallStep())
            pipeline.addStep(DownloadModpackStep(client, json, 
                onStatusUpdate = { _downloadStatus.update { s -> s.copy(type = it) } },
                onProgress = { cur, total -> _downloadStatus.update { it.copy(current = cur, total = total) } },
                onManifestReady = { /* maybe update something */ },
                installFabric = { mcUrl, lv, n -> installFabric(mcUrl, lv, n, overrideBaseDir = context.baseDir) },
                installMultiMC = { mcUrl, lt, lv, n -> installMultiMCModLoader(mcUrl, lt, lv, n, overrideBaseDir = context.baseDir) },
                installVanilla = { mcUrl, n -> installVanilla(mcUrl, n, overrideBaseDir = context.baseDir) }
            ))

            pipeline.execute(context)
        } catch (e: Exception) { _downloadStatus.update { it.copy(type = StatusType.IDLE, errorCode = e.toString()) } }
    }

    private fun checkAuthlibInjector(): String? {
        val now = Instant.now()
        val state = StateManager.data.value

        val cachedFilePath = state.authlibInjectorSha1?.let { sha1 ->
            getCacheFile(sha1)
        }

        val isCacheValid = state.authlibInjectorDownloadTimestamp != null &&
                cachedFilePath != null &&
                cachedFilePath.exists() &&
                Instant.ofEpochSecond(state.authlibInjectorDownloadTimestamp.toLong())
                    .plus(6, ChronoUnit.HOURS)
                    .isAfter(now)

        if (isCacheValid) {
            return state.authlibInjectorSha1
        }

        return null
    }

    suspend fun getVersions(): List<Installation> = withContext(Dispatchers.IO) {
        val installationsDir = PlatformUtils.userDataDirectory.resolve("installations")
        val installations = installationsDir.listFiles() ?: return@withContext listOf()

        installations
            .filter { it.isDirectory }
            .map { installationDir ->
                async {
                    try {
                        val configFile = installationDir.resolve("AnyMC.json")
                        if (!configFile.exists()) return@async null

                        val installationData = json.decodeFromString<AnyMCInstallationData>(configFile.readText())

                        Installation(
                            name = installationData.name,
                            version = installationData.minecraftVersion,
                            id = installationDir.name,
                            modLoader = installationData.loaderType ?: "vanilla"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    fun getQuickPlayData(id: String): List<QuickPlay>? {
        val baseDir = getDirectory(id)
        val quickPlayFile = baseDir.resolve("quickplay.json")
        return if (quickPlayFile.exists()) {
            json.decodeFromString<List<QuickPlay>>(quickPlayFile.readText())
        } else null
    }

    fun deleteVersion(id: String): Boolean {
        val installation = getDirectory(id)
        val deleted = if (installation.exists()) installation.deleteRecursively() else false
        if (deleted) {
            notifyInstallationsChanged()
        }
        return deleted
    }

    fun renameInstallation(id: String, newName: String): Boolean {
        return try {
            val installationFile = getDirectory(id).resolve("AnyMC.json")
            if (!installationFile.exists()) return false

            val data = json.decodeFromString<AnyMCInstallationData>(installationFile.readText())
            val updatedData = data.copy(name = newName)
            installationFile.writeText(json.encodeToString(updatedData))
            
            notifyInstallationsChanged()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDirectory(id: String): File {
        return PlatformUtils.userDataDirectory.resolve("installations").resolve(id)
    }

    fun hasBackup(id: String): Boolean {
        val backupDir = getDirectory(id).resolve("gameFiles").resolve("mods_backup_latest")
        return backupDir.exists() && backupDir.isDirectory && (backupDir.list()?.isNotEmpty() == true)
    }

    fun launchBackground(id: String, quickPlay: QuickPlay? = null) { managerScope.launch { launch(id, quickPlay) } }

    suspend fun launch(id: String, quickPlay: QuickPlay? = null) = withContext(Dispatchers.IO) {
        if (_updatingInstallations.value.contains(id)) {
            println("Cannot launch installation $id while it is updating!")
            return@withContext
        }
        try {
            val resolvedAccount = resolveAccountData()
            val context = LaunchContext(id, quickPlay, baseDir = getDirectory(id), accountData = resolvedAccount, 
                activeFeatures = LaunchLogic.buildActiveFeatures(resolvedAccount.isDemo, quickPlay),
                maxRamMb = ConfigManager.data.value.maxRamUsage.toIntOrNull() ?: 4096,
                width = ConfigManager.data.value.windowWidth,
                height = ConfigManager.data.value.windowHeight)
            
            val pipeline = Pipeline<LaunchContext>("anymc:launch")
            
            pipeline.addStep(InitializeLaunchStep())
            pipeline.addStep(LoadConfigStep(json))
            pipeline.addStep(LoadModLoaderStep(json))
            pipeline.addStep(EnsureJavaRuntimeStep(client) { type, cur, total ->
                _downloadStatus.update { it.copy(type = type, current = cur, total = total) }
            })
            pipeline.addStep(EnsureCompatStep(client))
            pipeline.addStep(BuildClasspathStep())
            pipeline.addStep(ExtractNativesStep())
            pipeline.addStep(BuildCommandStep())
            pipeline.addStep(ExecuteLaunchStep { bd, fc, os, jv, ph, uuid ->
                executeGameProcess(bd, fc, os, jv, ph, uuid)
            })

            pipeline.execute(context)
        } catch (e: Exception) { handleLaunchFailure(e.message ?: "Launch failed", e) }
    }

    private fun handleLaunchFailure(reason: String, exception: Exception? = null) {
        _minecraftStatus.update { MinecraftStatus.FAILED }
        _lastCrashLog.update { exception?.stackTraceToString() ?: reason }
        exception?.printStackTrace()
    }

    private fun resolveAccountData(): ResolvedAccount {
        val active = AccountManager.activeAccount
        val all = AccountManager.data.value.accounts
        val hasLicensed = all.any { it is Account.Microsoft && !it.isDemo }
        return ResolvedAccount(
            name = active?.username ?: "Player",
            uuid = active?.uuid ?: "00000000-0000-0000-0000-000000000000",
            token = when (active) { is Account.Microsoft -> active.accessToken; is Account.ElyBy -> active.accessToken; else -> "0" },
            type = when (active) { is Account.Microsoft -> "msa"; is Account.ElyBy -> "mojang"; else -> "offline" },
            isDemo = if (active is Account.Microsoft) active.isDemo else !hasLicensed,
            xuid = (active as? Account.Microsoft)?.xuid
        )
    }

    private suspend fun executeGameProcess(
        baseDir: File,
        finalCommand: List<String>,
        currentOS: String,
        javaVersion: JavaVersion?,
        placeholders: Map<String, String>,
        playerUuid: String
    ) = coroutineScope {
        println("Launching: ${finalCommand.joinToString(" ")}")
        baseDir.resolve("gameFiles").mkdirs()
        val pb = ProcessBuilder(finalCommand).apply { directory(baseDir.absoluteFile); redirectErrorStream(true) }
        if (currentOS == "linux") {
            val preloads = mutableListOf<String>()
            LaunchLogic.provisionZlibCompat()?.let {
                println("[COMPAT]: Preloading zlib shim: $it")
                preloads.add(it)
            }

            if (preloads.isNotEmpty()) {
                val existingPreload = pb.environment()["LD_PRELOAD"]
                val finalPreload = (preloads + listOfNotNull(existingPreload)).joinToString(File.pathSeparator)
                pb.environment()["LD_PRELOAD"] = finalPreload
                println("[COMPAT]: Final LD_PRELOAD=$finalPreload")
            }

            when (ConfigManager.data.value.gpuOffload) {
                "nvidia" -> {
                    pb.environment()["__NV_PRIME_RENDER_OFFLOAD"] = "1"
                    pb.environment()["__GLX_VENDOR_LIBRARY_NAME"] = "nvidia"
                }
                "amd" -> pb.environment()["DRI_PRIME"] = "1"
            }
            val ldPaths = mutableListOf<String>(); javaVersion?.let {
            val javaExe = finalCommand.firstOrNull() ?: "java"; File(javaExe).parentFile?.parentFile?.resolve("lib")?.takeIf { it.exists() }?.let { lib ->
            ldPaths.addAll(listOf(lib.absolutePath, lib.resolve("server").absolutePath, lib.resolve("jli").absolutePath))
        }
        }
            ldPaths.add(baseDir.resolve("natives").absolutePath)
            pb.environment()["LD_LIBRARY_PATH"] = (ldPaths + listOfNotNull(pb.environment()["LD_LIBRARY_PATH"])).joinToString(File.pathSeparator)
        }
        val startTime = Instant.now()
        val process = pb.start().also { gameProcess = it }
        _minecraftStatus.update { MinecraftStatus.LAUNCHED }
        _lastCrashLog.update { null }

        var crashLogPath: String? = null
        val crashPattern = Pattern.compile("Crash report saved to:.*?(/\\S+|\\w:[\\\\/]\\S+)")

        val logReaderJob = managerScope.launch(Dispatchers.IO) {
            process.inputStream.bufferedReader().use { r ->
                var l: String?
                while (r.readLine().also { l = it } != null) {
                    val currentLine = l!!
                    _consoleLogs.update { (it + currentLine).takeLast(500) }
                    println(currentLine)

                    if (currentLine.contains("#@!@#")) {
                        val matcher = crashPattern.matcher(currentLine)
                        if (matcher.find()) {
                            crashLogPath = matcher.group(1)?.trim()
                        }
                    }
                }
            }
        }

        val finishedImmediately = withContext(Dispatchers.IO) {
            process.waitFor(2, TimeUnit.SECONDS)
        }

        if (finishedImmediately) {
            val exitValue = process.exitValue()
            gameProcess = null
            logReaderJob.join()

            val endTime = Instant.now()
            val playTimeSeconds = ChronoUnit.SECONDS.between(startTime, endTime)
            if (playTimeSeconds > 5) {
                StatsManager.addTrackedPlayTime(StatsManager.formatUuid(playerUuid), playTimeSeconds)
            }

            val crashContent = crashLogPath?.let { runCatching { File(it).readText() }.getOrNull() }

            if (crashContent != null || exitValue != 0) {
                val finalLog = crashContent ?: _consoleLogs.value.joinToString("\n")
                _minecraftStatus.update { MinecraftStatus.FAILED }
                _lastCrashLog.update { finalLog }
            } else {
                _minecraftStatus.update { MinecraftStatus.READY }
            }
        } else {
            managerScope.launch(Dispatchers.IO) {
                val exitValue = process.waitFor()
                gameProcess = null
                logReaderJob.join()

                val endTime = Instant.now()
                val playTimeSeconds = ChronoUnit.SECONDS.between(startTime, endTime)
                if (playTimeSeconds > 5) {
                    StatsManager.addTrackedPlayTime(StatsManager.formatUuid(playerUuid), playTimeSeconds)
                }

                val crashContent = crashLogPath?.let { runCatching { File(it).readText() }.getOrNull() }

                if (crashContent != null || exitValue != 0) {
                    val finalLog = crashContent ?: _consoleLogs.value.joinToString("\n")
                    _minecraftStatus.update { MinecraftStatus.FAILED }
                    _lastCrashLog.update { finalLog }
                } else {
                    _minecraftStatus.update { MinecraftStatus.READY }
                }
            }
        }
    }

    fun killGame() { gameProcess?.destroy(); _minecraftStatus.update { MinecraftStatus.KILLING } }
    suspend fun getAvailableJavaRuntimes(): List<JavaVersion> = withContext(Dispatchers.IO) {
        try {
            val platformKey = getPlatformKey()
            val response = client.get("https://piston-meta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json")
            val bodyText = response.bodyAsText().trim().removePrefix("\uFEFF")
            val rootJson = json.parseToJsonElement(bodyText).jsonObject

            val platformObj = rootJson[platformKey]?.jsonObject ?: return@withContext emptyList()
            val components = platformObj.keys
            
            components.mapNotNull { component ->
                val versions = platformObj[component]?.jsonArray ?: return@mapNotNull null
                val firstVersion = versions.firstOrNull()?.jsonObject ?: return@mapNotNull null
                
                // Version info can be in 'version' object or sometimes directly if structure differs
                val versionObj = firstVersion["version"]?.jsonObject
                val name = versionObj?.get("name")?.jsonPrimitive?.content
                
                val majorVersion = if (name != null) {
                    name.substringBefore(".").toIntOrNull() ?: 8
                } else {
                    // Fallback: some components have implicit versions
                    when (component) {
                        "java-runtime-alpha" -> 16
                        "java-runtime-beta", "java-runtime-gamma" -> 17
                        "java-runtime-delta" -> 21
                        "java-runtime-epsilon" -> 25
                        "jre-legacy" -> 8
                        else -> 8
                    }
                }
                
                JavaVersion(component, majorVersion)
            }.sortedBy { it.majorVersion }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun getPlatformKey(): String {
        val os = PlatformUtils.getCurrentOS()
        val arch = System.getProperty("os.arch")
        return when (os) {
            "windows" -> if (arch.contains("64")) "windows-x64" else "windows-x86"
            "osx" -> if (arch.contains("aarch64")) "mac-os-arm64" else "mac-os"
            else -> if (arch.contains("64")) "linux" else "linux-i386"
        }
    }
    fun cleanError() { _downloadStatus.update { it.copy(errorCode = null) } }
    fun clearCrash() { _minecraftStatus.update { MinecraftStatus.READY }; _lastCrashLog.update { "" } }
    fun clearConsole() { _consoleLogs.update { emptyList() } }

    // Simulation methods
    fun updateDownloadStatus(status: DownloadStatus) {
        _downloadStatus.update { status }
    }

    fun updateMinecraftStatus(status: MinecraftStatus) {
        _minecraftStatus.update { status }
    }
}
