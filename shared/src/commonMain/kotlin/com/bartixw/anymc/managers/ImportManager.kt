/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.core.utils.ZipUtils
import com.bartixw.anymc.minecraft.data.AnyMCInstallationData
import com.bartixw.anymc.minecraft.data.ModrinthInstallationMetadata
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File
import java.time.Instant

object ImportManager {
    suspend fun importInstallation(zipFile: File, newName: String? = null) = withContext(Dispatchers.IO) {
        try {
            val ts = Instant.now().epochSecond
            val installationId = "$ts"
            val installationDir = PlatformUtils.userDataDirectory.resolve("installations").resolve(installationId)
            installationDir.mkdirs()

            ZipUtils.unzip(zipFile, installationDir)

            val configFile = installationDir.resolve("AnyMC.json")
            if (!configFile.exists()) {
                throw Exception("Invalid installation zip: AnyMC.json missing")
            }

            var data = MinecraftManager.json.decodeFromString<AnyMCInstallationData>(configFile.readText())
            
            if (newName != null) {
                data = data.copy(name = newName)
                configFile.writeText(MinecraftManager.json.encodeToString(data))
            }

            // 1. Fetch manifest to find version URL
            val manifest = MinecraftManager.getVersionManifest()
            val versionInfo = manifest.versions.find { it.id == data.minecraftVersion }
                ?: throw Exception("Minecraft version ${data.minecraftVersion} not found in manifest")
            val versionUrl = Url(versionInfo.url)

            // 2. Trigger Minecraft installation steps (re-download libraries, assets, jar)
            when (data.loaderType) {
                "fabric", "legacyfabric" -> {
                    MinecraftManager.installFabric(
                        vUrl = versionUrl,
                        loaderVersion = data.loaderVersion ?: "",
                        name = data.name,
                        isLegacy = data.loaderType == "legacyfabric",
                        overrideBaseDir = installationDir
                    )
                }
                "forge", "neoforge" -> {
                    MinecraftManager.installMultiMCModLoader(
                        vUrl = versionUrl,
                        loaderType = data.loaderType!!,
                        loaderVersion = data.loaderVersion ?: "",
                        name = data.name,
                        overrideBaseDir = installationDir
                    )
                }
                else -> {
                    MinecraftManager.installVanilla(
                        url = versionUrl,
                        name = data.name,
                        overrideBaseDir = installationDir
                    )
                }
            }

            // 3. Trigger Modrinth asset downloads
            val metadataFile = installationDir.resolve("modrinth_assets.json")
            if (metadataFile.exists()) {
                val metadata = MinecraftManager.json.decodeFromString<ModrinthInstallationMetadata>(metadataFile.readText())
                metadata.assets.forEach { asset ->
                    ModDownloadManager.downloadModById(
                        installationId = installationId,
                        versionId = asset.versionId,
                        loader = data.loaderType ?: "vanilla",
                        gameVersion = data.minecraftVersion,
                        projectType = asset.projectType
                    )
                }
            }

            MinecraftManager.notifyInstallationsChanged()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
