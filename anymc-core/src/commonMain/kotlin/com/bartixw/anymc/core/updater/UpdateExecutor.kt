/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.updater

import com.bartixw.anymc.core.system.SystemRegistryManager
import com.bartixw.anymc.core.utils.PlatformUtils
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess

class UpdateExecutor {

    suspend fun downloadAndPrepareUpdate(
        client: HttpClient,
        updateInfo: UpdateInfo,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "anymc_update_tmp")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        val downloadedFile = File(tempDir, updateInfo.assetName)
        val extractedDir = File(tempDir, "extracted")
        extractedDir.mkdirs()

        // 1. Download
        onProgress(0.05f)
        client.get(updateInfo.downloadUrl) {
            onDownload { bytesSentTotal, contentLength ->
                if (contentLength != null && contentLength > 0) {
                    val p = 0.05f + (bytesSentTotal.toFloat() / contentLength.toFloat()) * 0.65f
                    onProgress(p.coerceIn(0.05f, 0.70f))
                }
            }
        }.let { response ->
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Failed to download update asset: HTTP ${response.status.value}")
            }
            val channel: ByteReadChannel = response.bodyAsChannel()
            FileOutputStream(downloadedFile).use { out ->
                val buffer = ByteArray(8192)
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                }
            }
        }

        // 2. Extract
        onProgress(0.75f)
        unpackArchive(downloadedFile, extractedDir)
        downloadedFile.delete()
        onProgress(1.0f)

        return@withContext extractedDir
    }

    fun applyUpdateAndRestart(
        extractedDir: File,
        installDir: File = SystemRegistryManager.detectActualInstallationDirectory()
    ) {
        val os = PlatformUtils.getCurrentOS().lowercase()
        val installPath = installDir.absolutePath
        val extractedPath = findAppContentRoot(extractedDir).absolutePath

        val currentCommand = try {
            ProcessHandle.current().info().command().orElse(null)
        } catch (_: Exception) {
            null
        }

        val targetExecutable = if (!currentCommand.isNullOrBlank() && File(currentCommand).exists()) {
            File(currentCommand)
        } else {
            val exeName = when (os) {
                "windows" -> "AnyMC Launcher.exe"
                else -> "AnyMC Launcher"
            }
            findExecutableInDirectory(installDir, exeName)
        }

        val launchPath = targetExecutable.absolutePath

        if (os == "windows") {
            val scriptLines = mutableListOf<String>()
            scriptLines.add("@echo off")
            scriptLines.add("timeout /t 2 /nobreak > NUL")
            scriptLines.add("xcopy /e /y /q \"$extractedPath\\*\" \"$installPath\\\"")
            scriptLines.add("if exist \"${extractedDir.absolutePath}\" rmdir /s /q \"${extractedDir.absolutePath}\"")
            if (File(launchPath).exists()) {
                scriptLines.add("start \"\" \"$launchPath\"")
            }
            scriptLines.add("del \"%~f0\"")

            val tempBat = File(System.getProperty("java.io.tmpdir"), "anymc_apply_update.bat")
            tempBat.writeText(scriptLines.joinToString("\r\n"))

            ProcessBuilder("cmd.exe", "/c", "start", "/b", tempBat.absolutePath)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } else {
            // Linux / macOS
            val scriptCmd = buildString {
                append("sleep 1 && ")
                append("cp -rf \"$extractedPath/\"* \"$installPath/\" && ")
                append("rm -rf \"${extractedDir.absolutePath}\"")
                if (File(launchPath).exists()) {
                    append(" && chmod +x \"$launchPath\" && nohup \"$launchPath\" >/dev/null 2>&1 &")
                }
            }

            ProcessBuilder("sh", "-c", "nohup sh -c '$scriptCmd' >/dev/null 2>&1 &")
                .start()
        }

        exitProcess(0)
    }

    private fun findAppContentRoot(dir: File): File {
        val children = dir.listFiles { f -> f.isDirectory }
        if (children != null && children.size == 1 && (dir.listFiles()?.size == 1)) {
            return children[0]
        }
        return dir
    }

    private fun findExecutableInDirectory(dir: File, expectedName: String): File {
        val found = dir.walkTopDown().firstOrNull { f ->
            f.isFile && (f.name.equals(expectedName, ignoreCase = true) || f.name.equals("AnyMC.jar", ignoreCase = true))
        }
        return found ?: File(dir, expectedName)
    }

    private fun unpackArchive(inputFile: File, targetDir: File) {
        val fileName = inputFile.name.lowercase()
        if (fileName.endsWith(".zip")) {
            ZipInputStream(FileInputStream(inputFile)).use { zipIn ->
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
        } else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
            try {
                val process = ProcessBuilder("tar", "-xzf", inputFile.absolutePath, "-C", targetDir.absolutePath).start()
                process.waitFor()
            } catch (e: Exception) {
                GZIPInputStream(FileInputStream(inputFile)).use { gzipIn ->
                    val destFile = File(targetDir, inputFile.name.removeSuffix(".gz"))
                    FileOutputStream(destFile).use { out ->
                        gzipIn.copyTo(out)
                    }
                }
            }
        }
    }

    suspend fun downloadAndApplyUpdate(
        updateInfo: UpdateInfo,
        appsDirectory: File,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val client = HttpClient(io.ktor.client.engine.cio.CIO) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
            }
        }
        val extracted = downloadAndPrepareUpdate(client, updateInfo, onProgress)
        client.close()
        applyUpdateAndRestart(extracted, appsDirectory)
        return@withContext extracted
    }
}
