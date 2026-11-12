/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.installer.services

import com.bartixw.anymc.installer.model.GitHubRelease
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class GitHubReleaseService(
    val apiHost: String
) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            })
        }
    }

    suspend fun fetchReleases(owner: String, repo: String): List<GitHubRelease> = withContext(Dispatchers.IO) {
        val url = when {
            apiHost.contains("://") -> "$apiHost/repos/$owner/$repo/releases"
            apiHost.contains("localhost") || apiHost.contains("127.0.0.1") || apiHost.contains("0.0.0.0") || apiHost.startsWith("192.168.") || apiHost.startsWith("10.") ->
                "http://$apiHost/repos/$owner/$repo/releases"
            else -> "https://$apiHost/repos/$owner/$repo/releases"
        }
        try {
            val responseText = client.get(url) {
                header(HttpHeaders.UserAgent, "AnyMC-Installer")
                header(HttpHeaders.Accept, "application/vnd.github.v3+json")
            }.bodyAsText()

            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            }

            val releases = try {
                json.decodeFromString<List<GitHubRelease>>(responseText)
            } catch (_: Exception) {
                try {
                    val single = json.decodeFromString<GitHubRelease>(responseText)
                    listOf(single)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            releases.filter { !it.prerelease }
                .ifEmpty { releases }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun downloadAsset(
        downloadUrl: String,
        outputFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        client.prepareGet(downloadUrl) {
            header(HttpHeaders.UserAgent, "AnyMC-Installer")
        }.execute { response ->
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Failed to download asset: HTTP ${response.status.value}")
            }
            val totalBytes = response.contentLength() ?: -1L
            val channel: ByteReadChannel = response.bodyAsChannel()
            var bytesDownloaded = 0L

            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(8192)
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    outputStream.write(buffer, 0, read)
                    bytesDownloaded += read
                    onProgress(bytesDownloaded, totalBytes)
                }
            }
        }
        outputFile
    }

    fun close() {
        client.close()
    }
}
