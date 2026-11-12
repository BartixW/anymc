/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft

import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.core.utils.PlatformUtils.getCurrentOS
import com.bartixw.anymc.core.utils.ZipUtils
import com.bartixw.anymc.minecraft.data.*
import com.bartixw.anymc.minecraft.utils.LaunchUtils
import com.bartixw.anymc.minecraft.utils.sha1
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.collections.copy

object InstallLogic {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun downloadFile(
        client: HttpClient,
        url: String,
        destination: Path,
        expectedSha1: String? = null,
        forceCopy: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext null

        val suffix = if (destination.toString().endsWith(".jar")) ".jar" else ""
        var cacheFile = if (expectedSha1 != null) getCacheFile(
            expectedSha1,
            suffix
        ) else null

        if (cacheFile == null || !cacheFile.exists() || cacheFile.sha1() != expectedSha1) {
            val response = client.get(url)

            if (response.status.value !in 200..299) {
                throw Exception("Request responded with error code ${response.status.value}")
            }

            withContext(Dispatchers.IO) {
                val networkStream = response.bodyAsChannel().toInputStream()

                if (expectedSha1 != null) {
                    val cachePath = cacheFile!!.toPath()

                    Files.createDirectories(cachePath.parent)

                    networkStream.use { input ->
                        Files.newOutputStream(cachePath).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    val digest = MessageDigest.getInstance("SHA-1")
                    val memoryBuffer = ByteArrayOutputStream()

                    DigestInputStream(networkStream, digest).use { digestStream ->
                        digestStream.copyTo(memoryBuffer)
                    }

                    val computedSha1Bytes = digest.digest()
                    val computedSha1Hex = computedSha1Bytes.joinToString("") { "%02x".format(it) }

                    cacheFile = getCacheFile(
                        computedSha1Hex,
                        suffix
                    )
                    val cachePath = cacheFile.toPath()

                    Files.createDirectories(cachePath.parent)

                    Files.write(cachePath, memoryBuffer.toByteArray())
                }
            }
        }

        withContext(Dispatchers.IO) {
            linkOrCopy(
                cacheFile!!.toPath(),
                destination,
                forceCopy
            )
        }

        return@withContext cacheFile!!.name.substringBeforeLast(".")
    }

    fun getCacheFile(sha1: String, suffix: String = ""): File {
        val subFolder = sha1.substring(0, 2)
        val storageDir = PlatformUtils.sharedStorageDirectory.resolve("storage")
        val folder = storageDir.resolve(subFolder)

        val fileWithSuffix = folder.resolve("$sha1$suffix")
        val fileNoSuffix = folder.resolve(sha1)

        if (fileWithSuffix.exists()) return fileWithSuffix

        if (fileNoSuffix.exists()) {
            if (suffix.isNotEmpty()) {
                fileNoSuffix.renameTo(fileWithSuffix)
                return fileWithSuffix
            }
            return fileNoSuffix
        }

        return fileWithSuffix
    }

    suspend fun linkFileFromCache(sha1: String, path: Path, forceCopy: Boolean = false) {
        val suffix = if (path.toString().endsWith(".jar")) ".jar" else ""
        val cacheFile = getCacheFile(sha1, suffix)

        withContext(Dispatchers.IO) {
            linkOrCopy(cacheFile.toPath(), path, forceCopy)
        }
    }

    private fun linkOrCopy(source: Path, target: Path, forceCopy: Boolean = false) {
        Files.createDirectories(target.parent)
        Files.deleteIfExists(target)

        if (forceCopy) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            return
        }

        try {
            // Try hard link first (best for JRE on Linux)
            Files.createLink(target, source.toAbsolutePath())
        } catch (e: Exception) {
            try {
                // Fallback to symbolic link
                Files.createSymbolicLink(target, source.toAbsolutePath())
            } catch (e2: Exception) {
                // Final fallback to copy
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    suspend fun installAssets(client: HttpClient, assetIndex: AssetIndex, baseDir: File, onProgress: suspend (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
        val assetsDir = baseDir.resolve("assets")
        val objectsDir = assetsDir.resolve("objects")
        val indexesDir = assetsDir.resolve("indexes")
        indexesDir.mkdirs()

        val indexFile = indexesDir.resolve("${assetIndex.id}.json")
        downloadFile(client, assetIndex.url, indexFile.toPath(), assetIndex.sha1)

        val indexContent = indexFile.readText()
        val indexData = json.decodeFromString<AssetIndexContent>(indexContent)

        var downloaded = 0
        val total = indexData.objects.size

        indexData.objects.forEach { (name, obj) ->
            val path = obj.hash.substring(0, 2) + "/" + obj.hash
            val destFile = objectsDir.resolve(path)
            destFile.parentFile.mkdirs()
            
            val url = "https://resources.download.minecraft.net/${path}"
            downloadFile(client, url, destFile.toPath(), obj.hash)
            downloaded++
            onProgress(downloaded, total)
        }
    }

    suspend fun ensureJavaRuntime(client: HttpClient, javaVersion: JavaVersion, onProgress: suspend (StatusType, Int, Int) -> Unit) = withContext(Dispatchers.IO) {
        val runtimeDir = PlatformUtils.sharedStorageDirectory.resolve("java_runtimes").resolve(javaVersion.component)
        val successMarker = runtimeDir.resolve(".success")
        if (successMarker.exists()) return@withContext

        runtimeDir.mkdirs()
        onProgress(StatusType.JAVA_FETCH, 0, 0)
        val platformKey = getPlatformKey()
        val response = client.get("https://piston-meta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json")
        val bodyText = response.bodyAsText().trim().removePrefix("\uFEFF")
        val rootJson = json.parseToJsonElement(bodyText).jsonObject

        val platformObj = rootJson[platformKey]?.jsonObject ?: throw Exception("Platform $platformKey not found")
        val componentList = platformObj[javaVersion.component]?.jsonArray ?: throw Exception("Component ${javaVersion.component} not found")
        val manifestUrl = componentList[0].jsonObject["manifest"]?.jsonObject?.get("url")?.jsonPrimitive?.content ?: throw Exception("Manifest URL not found")

        val manifestReq = client.get(manifestUrl)
        val manifestData = json.decodeFromString<JavaRuntimeManifest>(manifestReq.bodyAsText())

        onProgress(StatusType.JAVA_INSTALL, 0, manifestData.files.size)
        var count = 0
        manifestData.files.forEach { (path, fileData) ->
            val destFile = runtimeDir.resolve(path)
            if (fileData.type == "directory") {
                destFile.mkdirs()
            } else if (fileData.type == "file") {
                destFile.parentFile.mkdirs()
                // Prefer 'raw' over 'lzma' to avoid needing decompression logic
                val dl = fileData.downloads?.raw ?: fileData.downloads?.lzma
                if (dl != null) {
                    if (dl == fileData.downloads?.lzma) {
                        // If we only have LZMA, we might still have issues unless we implement decompression.
                        // For now, let's warn.
                        println("Warning: Only LZMA download available for ${path}. This might fail to execute.")
                    }
                    downloadFile(client, dl.url, destFile.toPath(), dl.sha1)
                    if (fileData.executable == true) {
                        destFile.setExecutable(true)
                    }
                }
            } else if (fileData.type == "link") {
                val target = fileData.target
                if (target != null) {
                    destFile.parentFile.mkdirs()
                    try {
                        val linkPath = destFile.toPath()
                        Files.deleteIfExists(linkPath)
                        // Create a relative symlink
                        val targetPath = java.nio.file.Paths.get(target)
                        Files.createSymbolicLink(linkPath, targetPath)
                    } catch (e: Exception) {
                        println("Failed to create symlink ${destFile.name} -> $target: ${e.message}")
                    }
                }
            }
            count++
            onProgress(StatusType.JAVA_INSTALL, count, manifestData.files.size)
        }
        successMarker.createNewFile()
    }

    suspend fun downloadAuthlibInjector(client: HttpClient, targetPath: Path): String? {
        val now = Instant.now()

        try {
            val releaseUrl = "https://api.github.com/repos/yushijinhun/authlib-injector/releases/latest"
            val release: GitHubRelease = client.get(releaseUrl) {
                header(HttpHeaders.UserAgent, "AnyMC")
            }.body()

            val targetAsset = release.assets.find { Regex("authlib-injector-.*\\.jar").matches(it.name) }
                ?: throw IllegalStateException("Authlib injector asset on GitHub not found.")

            val newHash = downloadFile(
                client,
                targetAsset.browserDownloadUrl,
                targetPath,
                null
            )

            return newHash
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun provisionZlibCompat(client: HttpClient): String? {
        if (getCurrentOS() != "linux") return null

        val storageDir = PlatformUtils.sharedStorageDirectory.resolve("storage").resolve("compat")
        val zlibFile = storageDir.resolve("libz.so.1")
        val successMarker = storageDir.resolve(".zlib_success")

        if (zlibFile.exists() && successMarker.exists()) return zlibFile.absolutePath

        try {
            storageDir.mkdirs()
            val debFile = storageDir.resolve("zlib.deb")
            val debUrl = "http://ftp.us.debian.org/debian/pool/main/z/zlib/zlib1g_1.2.13.dfsg-1_amd64.deb"

            println("[COMPAT]: Downloading standard zlib for Forge compatibility...")
            val response = client.get(debUrl)
            if (response.status.value !in 200..299) throw Exception("Failed to download zlib deb")

            withContext(Dispatchers.IO) {
                debFile.outputStream().use { output ->
                    response.bodyAsChannel().toInputStream().copyTo(output)
                }

                // Extraction using system tools
                val pb = ProcessBuilder("bash", "-c", "ar x ${debFile.name} && tar -xvf data.tar.xz ./lib/x86_64-linux-gnu/libz.so.1.2.13")
                pb.directory(storageDir)
                val process = pb.start()
                process.waitFor()

                val extractedLib = storageDir.resolve("lib/x86_64-linux-gnu/libz.so.1.2.13")
                if (extractedLib.exists()) {
                    extractedLib.copyTo(zlibFile, overwrite = true)
                    successMarker.writeText(System.currentTimeMillis().toString())

                    // Cleanup
                    debFile.delete()
                    storageDir.resolve("data.tar.xz").delete()
                    storageDir.resolve("control.tar.xz").delete()
                    storageDir.resolve("debian-binary").delete()
                    storageDir.resolve("lib").deleteRecursively()
                    println("[COMPAT]: Successfully provisioned classic zlib.")
                } else {
                    println("[COMPAT]: Extraction failed - libz.so.1.2.13 not found in expected location.")
                }
            }

            return if (zlibFile.exists()) zlibFile.absolutePath else null
        } catch (e: Exception) {
            println("[COMPAT]: Failed to provision zlib compatibility library: ${e.message}")
            return null
        }
    }

    private fun getPlatformKey(): String {
        val os = PlatformUtils.getCurrentOS()
        val arch = System.getProperty("os.arch").lowercase()
        return when (os) {
            "windows" -> when {
                arch.contains("aarch64") || arch.contains("arm64") -> "windows-arm64"
                arch.contains("64") -> "windows-x64"
                else -> "windows-x86"
            }
            "osx" -> if (arch.contains("aarch64") || arch.contains("arm64")) "mac-os-arm64" else "mac-os"
            else -> when {
                arch.contains("aarch64") || arch.contains("arm64") -> "linux-arm64"
                arch.contains("64") -> "linux"
                else -> "linux-i386"
            }
        }
    }
}
