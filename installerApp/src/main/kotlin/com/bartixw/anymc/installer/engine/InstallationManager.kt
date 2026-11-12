/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.installer.engine

import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.installer.model.GitHubAsset
import com.bartixw.anymc.installer.model.GitHubRelease
import com.bartixw.anymc.installer.model.InstallationState
import com.bartixw.anymc.installer.model.InstallerConfig
import com.bartixw.anymc.installer.services.GitHubReleaseService
import com.bartixw.anymc.installer.system.SystemRegistryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

class InstallationManager(
    val apiHost: String,
    private val releaseService: GitHubReleaseService = GitHubReleaseService(apiHost)
) {

    private val _state = MutableStateFlow<InstallationState>(InstallationState.Idle)
    val state: StateFlow<InstallationState> = _state.asStateFlow()

    suspend fun loadReleases(owner: String = "BartixW", repo: String = "AnyMC"): List<GitHubRelease> = withContext(Dispatchers.IO) {
        _state.value = InstallationState.FetchingReleases
        val releases = releaseService.fetchReleases(owner, repo)
        val selected = releases.firstOrNull()
        _state.value = InstallationState.ReleasesLoaded(releases, selected)
        releases
    }

    suspend fun install(config: InstallerConfig) = withContext(Dispatchers.IO) {
        try {
            val (sourceFile, releaseVersion, isTemporary) = if (config.localZipFile != null) {
                val localFile = config.localZipFile
                if (!localFile.exists()) {
                    throw IllegalStateException("Local file doesn't exist: ${localFile.absolutePath}")
                }
                Triple(localFile, "local-zip", false)
            } else {
                val release = config.selectedRelease
                    ?: throw IllegalStateException("No release to download.")

                val asset = selectAssetForPlatform(release.assets)
                    ?: throw IllegalStateException("Couldn't find a file for your operating system (${PlatformUtils.getCurrentOS()}).")

                val tempDir = File(System.getProperty("java.io.tmpdir"), "anymc_installer_downloads")
                tempDir.mkdirs()
                val downloadedFile = File(tempDir, asset.name)

                // 1. Downloading
                _state.value = InstallationState.Downloading(
                    release = release,
                    assetName = asset.name,
                    progress = 0f,
                    bytesDownloaded = 0L,
                    totalBytes = asset.size
                )

                releaseService.downloadAsset(
                    downloadUrl = asset.browserDownloadUrl,
                    outputFile = downloadedFile
                ) { bytesDownloaded, totalBytes ->
                    val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat() else 0f
                    _state.value = InstallationState.Downloading(
                        release = release,
                        assetName = asset.name,
                        progress = progress,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes
                    )
                }

                Triple(downloadedFile, release.tagName, true)
            }

            // 2. Extracting / Installing
            _state.value = InstallationState.Extracting(sourceFile.name)
            val installDir = config.installDirectory
            installDir.mkdirs()

            unpackOrCopy(sourceFile, installDir)

            val executableFile =
                if (PlatformUtils.getCurrentOS() == "windows")
                    installDir.resolve("AnyMC Launcher.exe")
                else installDir.resolve("bin").resolve("AnyMC Launcher")

                // 3. System App Registration
            if (config.createDesktopShortcut || config.createStartMenuShortcut) {
                _state.value = InstallationState.RegisteringSystem
                SystemRegistryManager.registerApplicationInSystem(
                    appName = "AnyMC Launcher",
                    version = releaseVersion,
                    executableFile = executableFile,
                    createDesktopShortcut = config.createDesktopShortcut,
                    createStartMenuShortcut = config.createStartMenuShortcut,
                    iconFile =
                        if (PlatformUtils.getCurrentOS() == "linux")
                            installDir.resolve("lib").resolve("AnyMC Launcher.png")
                        else
                            installDir.resolve("AnyMC Launcher.exe,0")
                )
            }

            // 4. URI Scheme Registration
            if (config.registerUriScheme) {
                _state.value = InstallationState.RegisteringUriScheme
                SystemRegistryManager.registerUriScheme(
                    scheme = config.uriScheme,
                    appName = "AnyMC Launcher",
                    executableFile = executableFile
                )
            }

            // Clean temp downloaded file if temporary
            if (isTemporary) {
                sourceFile.delete()
            }

            _state.value = InstallationState.Completed(
                installedPath = installDir,
                executablePath = executableFile
            )

        } catch (e: Throwable) {
            e.printStackTrace()
            _state.value = InstallationState.Error(
                message = e.message ?: "Unexpected error during installation.",
                cause = e
            )
        }
    }

    private fun selectAssetForPlatform(assets: List<GitHubAsset>): GitHubAsset? {
        val launcherAssets = assets.filter { !it.name.contains("installer", ignoreCase = true) }
        val os = PlatformUtils.getCurrentOS().lowercase()

        val (keywords, preferredExtensions) = when (os) {
            "windows" -> {
                listOf("win", "windows") to listOf(".zip", ".exe", ".msi")
            }
            "osx" -> {
                listOf("mac", "macos", "osx", "darwin") to listOf(".dmg", ".zip", ".tar.gz")
            }
            else -> {
                listOf("linux") to listOf(".tar.gz", ".zip", ".appimage")
            }
        }

        for (ext in preferredExtensions) {
            val match = launcherAssets.firstOrNull { asset ->
                val nameLower = asset.name.lowercase()
                val matchesOs = keywords.any { nameLower.contains(it) }
                matchesOs && nameLower.endsWith(ext)
            }
            if (match != null) return match
        }

        val osMatch = launcherAssets.firstOrNull { asset ->
            val nameLower = asset.name.lowercase()
            keywords.any { nameLower.contains(it) }
        }
        if (osMatch != null) return osMatch

        return launcherAssets.firstOrNull()
    }

    private fun unpackOrCopy(inputFile: File, targetDir: File): File {
        val fileName = inputFile.name.lowercase()
        return when {
            fileName.endsWith(".zip") -> {
                extractZip(inputFile, targetDir)
                findExecutableInDirectory(targetDir)
            }
            fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") -> {
                extractTarGz(inputFile, targetDir)
                findExecutableInDirectory(targetDir)
            }
            else -> {
                // Copy single binary or jar file directly
                val outputFile = File(targetDir, inputFile.name)
                inputFile.copyTo(outputFile, overwrite = true)
                if (!PlatformUtils.getCurrentOS().contains("win")) {
                    outputFile.setExecutable(true)
                }
                outputFile
            }
        }
    }

    private fun extractZip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { out ->
                        zipIn.copyTo(out)
                    }
                    if (!PlatformUtils.getCurrentOS().contains("win") && (newFile.name.endsWith(".sh") || !newFile.name.contains("."))) {
                        newFile.setExecutable(true)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    private fun extractTarGz(tarGzFile: File, targetDir: File) {
        // Fallback unpack using standard GZIPInputStream and file write or platform tar
        try {
            val process = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", targetDir.absolutePath).start()
            if (process.waitFor() != 0) {
                throw Exception("tar execution failed")
            }
        } catch (e: Exception) {
            // Fallback copy if tar process not available
            val destFile = File(targetDir, tarGzFile.name.removeSuffix(".gz"))
            GZIPInputStream(FileInputStream(tarGzFile)).use { gzipIn ->
                FileOutputStream(destFile).use { out ->
                    gzipIn.copyTo(out)
                }
            }
        }
    }

    private fun findExecutableInDirectory(dir: File): File {
        val os = PlatformUtils.getCurrentOS()
        val expectedExeName = when (os) {
            "windows" -> "AnyMC.exe"
            "osx" -> "AnyMC"
            else -> "AnyMC"
        }

        // Search recursively for AnyMC executable or any .exe/.jar/script
        val found = dir.walkTopDown().firstOrNull { file ->
            file.isFile && (file.name.equals(expectedExeName, ignoreCase = true) || file.name.equals("AnyMC.jar", ignoreCase = true))
        }

        if (found != null) return found

        // Secondary fallback
        val anyExe = dir.walkTopDown().firstOrNull { file ->
            file.isFile && (file.name.endsWith(".exe") || file.name.endsWith(".jar") || file.canExecute())
        }

        return anyExe ?: File(dir, expectedExeName)
    }

    fun close() {
        releaseService.close()
    }
}
