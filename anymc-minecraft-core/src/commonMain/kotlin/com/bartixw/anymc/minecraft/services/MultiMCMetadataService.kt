/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.services

import com.bartixw.anymc.minecraft.data.MultiMCIndex
import com.bartixw.anymc.minecraft.data.MultiMCIndexVersion
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MultiMCMetadataService(
    private val client: HttpClient,
    private val uid: String
) {
    private val baseUrl = "https://meta.multimc.org/v1"

    suspend fun getVersionsForGame(mcVersion: String): List<MultiMCIndexVersion> = withContext(Dispatchers.IO) {
        try {
            val index: MultiMCIndex = client.get("$baseUrl/$uid/index.json").body()
            index.versions.filter { version ->
                version.requires.any { it.uid == "net.minecraft" && it.equals == mcVersion }
            }
        } catch (e: Exception) {
            println("Błąd pobierania wersji $uid z MultiMC API: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSupportedMinecraftVersions(): Set<String> = withContext(Dispatchers.IO) {
        try {
            val index: MultiMCIndex = client.get("$baseUrl/$uid/index.json").body()
            index.versions.flatMap { version ->
                version.requires.filter { it.uid == "net.minecraft" }.mapNotNull { it.equals }
            }.toSet()
        } catch (e: Exception) {
            println("Błąd pobierania obsługiwanych wersji $uid z MultiMC API: ${e.message}")
            emptySet()
        }
    }
}
