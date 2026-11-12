/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.utils

import com.bartixw.anymc.minecraft.data.ModrinthAssetMetadata
import com.bartixw.anymc.minecraft.data.ModrinthInstallationMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object MetadataUtils {
    private val json = Json { ignoreUnknownKeys = true }

    fun trackModrinthMetadata(installationDir: File, asset: ModrinthAssetMetadata) {
        try {
            val metadataFile = installationDir.resolve("modrinth_assets.json")
            val metadata = if (metadataFile.exists()) {
                json.decodeFromString<ModrinthInstallationMetadata>(metadataFile.readText())
            } else {
                ModrinthInstallationMetadata()
            }
            
            val updatedAssets = metadata.assets.toMutableList()
            updatedAssets.removeAll { it.relativePath == asset.relativePath }
            updatedAssets.add(asset)
            
            val updatedMetadata = metadata.copy(assets = updatedAssets)
            metadataFile.writeText(json.encodeToString(updatedMetadata))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
