/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.core.utils.ZipUtils
import com.bartixw.anymc.minecraft.data.AnyMCInstallationData
import com.bartixw.anymc.minecraft.data.ModrinthInstallationMetadata
import java.io.File

object ExportManager {
    fun exportInstallation(installationId: String, targetZipFile: File) {
        val installationDir = MinecraftManager.getDirectory(installationId)
        val configFile = installationDir.resolve("AnyMC.json")
        val installationData = if (configFile.exists()) {
            MinecraftManager.json.decodeFromString<AnyMCInstallationData>(configFile.readText())
        } else null

        val modrinthMetadata = ModDownloadManager.getModrinthMetadata(installationId)
        
        val modrinthPaths = modrinthMetadata.assets.map { it.relativePath }.toSet()
        val versionJarName = installationData?.let { "${it.minecraftVersion}.jar" }

        val excludedFolders = setOf("libraries", "assets", "natives", "runtime")
        val excludedFiles = setOf("modpack.mrpack")

        ZipUtils.zip(installationDir, targetZipFile) { file ->
            val relativePath = file.absolutePath.substring(installationDir.absolutePath.length + 1)
            
            // Exclude common MC folders at the root of installation
            if (file.isDirectory) {
                val folderName = file.name
                if (file.parentFile == installationDir && excludedFolders.contains(folderName)) {
                    return@zip false
                }
            }
            
            // Exclude files tracked by Modrinth
            if (file.isFile && modrinthPaths.contains(relativePath)) {
                return@zip false
            }

            if (file.isFile && excludedFiles.contains(relativePath)) {
                return@zip false
            }

            // Exclude version jar
            if (file.isFile && file.parentFile == installationDir && file.name == versionJarName) {
                return@zip false
            }
            
            // Exclude the target zip itself if it happens to be in the same dir
            if (file.absolutePath == targetZipFile.absolutePath) {
                return@zip false
            }
            
            true
        }
    }
}
