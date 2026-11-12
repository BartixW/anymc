/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.updater

import com.bartixw.anymc.core.utils.PlatformUtils
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class UpdateManager(
    private val client: HttpClient,
    private val owner: String = "BartixW",
    private val repo: String = "AnyMC",
    private val currentVersion: String = "1.0.0",
    private val apiHost: String = "api.github.com"
) {

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = when {
                apiHost.contains("://") -> "$apiHost/repos/$owner/$repo/releases/latest"
                apiHost.contains("localhost") || apiHost.contains("127.0.0.1") || apiHost.contains("0.0.0.0") || apiHost.startsWith("192.168.") || apiHost.startsWith("10.") ->
                    "http://$apiHost/repos/$owner/$repo/releases/latest"
                else -> "https://$apiHost/repos/$owner/$repo/releases/latest"
            }
            val responseText = client.get(url) {
                header(HttpHeaders.UserAgent, "AnyMC-Updater")
            }.bodyAsText()

            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            }

            val release = try {
                val list = json.decodeFromString<List<GitHubRelease>>(responseText)
                list.firstOrNull()
            } catch (_: Exception) {
                try {
                    json.decodeFromString<GitHubRelease>(responseText)
                } catch (_: Exception) {
                    null
                }
            } ?: return@withContext null

            val latestTag = release.tagName
            if (isNewerVersion(currentVersion, latestTag)) {
                val asset = selectPlatformAsset(release.assets) ?: return@withContext null

                return@withContext UpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = latestTag,
                    releaseNotes = release.body,
                    downloadUrl = asset.browserDownloadUrl,
                    assetName = asset.name
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun selectPlatformAsset(assets: List<GitHubAsset>): GitHubAsset? {
        val launcherAssets = assets.filter { !it.name.contains("installer", ignoreCase = true) }
        val os = PlatformUtils.getCurrentOS().lowercase()

        val (keywords, preferredExtensions) = when (os) {
            "windows" -> listOf("win", "windows") to listOf(".zip", ".exe", ".msi")
            "osx" -> listOf("mac", "macos", "osx", "darwin") to listOf(".dmg", ".zip", ".tar.gz")
            else -> listOf("linux") to listOf(".tar.gz", ".zip", ".appimage")
        }

        for (ext in preferredExtensions) {
            val match = launcherAssets.firstOrNull { asset ->
                val nameLower = asset.name.lowercase()
                keywords.any { nameLower.contains(it) } && nameLower.endsWith(ext)
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

    fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.removePrefix("v").trim()
        val cleanLatest = latest.removePrefix("v").trim()

        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
