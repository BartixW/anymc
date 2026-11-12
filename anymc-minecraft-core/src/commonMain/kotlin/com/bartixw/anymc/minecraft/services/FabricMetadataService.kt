/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.services

import com.bartixw.anymc.minecraft.data.FabricGameVersion
import com.bartixw.anymc.minecraft.data.FabricLoaderResponse
import com.bartixw.anymc.minecraft.data.FabricLoaderVersion
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FabricMetadataService(
    private val client: HttpClient,
    private val baseUrl: String = "https://meta.fabricmc.net/v2"
) {

    /**
     * Downloads Fabric supported Minecraft version list
     */
    suspend fun getSupportedGameVersions(): List<FabricGameVersion> = withContext(Dispatchers.IO) {
        try {
            client.get("$baseUrl/versions/game").body()
        } catch (e: Exception) {
            println("Błąd pobierania wersji gier z Fabric API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSupportedMinecraftVersions(): Set<String> = withContext(Dispatchers.IO) {
        try {
            val versions: List<FabricGameVersion> = client.get("$baseUrl/versions/game").body()
            versions.map { it.version }.toSet()
        } catch (e: Exception) {
            println("Błąd pobierania obsługiwanych wersji gier z Fabric API: ${e.message}")
            emptySet()
        }
    }

    /**
     * Downloads all loader versions
     */
    suspend fun getLoaderVersions(): List<FabricLoaderVersion> = withContext(Dispatchers.IO) {
        try {
            client.get("$baseUrl/versions/loader").body()
        } catch (e: Exception) {
            println("Błąd pobierania wersji loadera z Fabric API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getLoaderVersionsForGame(mcVersion: String): List<FabricLoaderVersion> = withContext(Dispatchers.IO) {
        try {
            val response: List<FabricLoaderResponse> = client.get("$baseUrl/versions/loader/$mcVersion").body()

            response.map {
                FabricLoaderVersion(
                    version = it.loader.version,
                    stable = it.loader.stable,
                    maven = it.loader.maven,
                    build = it.loader.build
                )
            }
        } catch (e: Exception) {
            println("Błąd pobierania loaderów dla wersji $mcVersion z Fabric API: ${e.message}")
            emptyList()
        }
    }

    /**
     * Helper which automatically pulls the latest Fabric version
     */
    suspend fun getLatestStableLoader(): FabricLoaderVersion? {
        return getLoaderVersions().firstOrNull { it.stable }
    }
}