/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.tailscale

import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.core.utils.ZipUtils
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object TailscaleDownloader {

    private const val FALLBACK_VERSION = "1.68.1"

    private fun fetchLatestVersion(): String {
        return try {
            val url = URI("https://pkgs.tailscale.com/stable/latest.txt").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "AnyMC-Launcher")
            connection.connect()
            val response = connection.inputStream.bufferedReader().readText().trim()
            connection.disconnect()
            response.ifEmpty { FALLBACK_VERSION }
        } catch (e: Exception) {
            e.printStackTrace()
            FALLBACK_VERSION
        }
    }

    fun getTailscaleDirectory(): File {
        return File(PlatformUtils.sharedStorageDirectory, "tailscale")
    }

    fun getBinaries(): Pair<File, File> {
        val dir = getTailscaleDirectory()
        val isWindows = PlatformUtils.getCurrentOS() == "windows"
        val tailscaleExe = if (isWindows) "tailscale.exe" else "tailscale"
        val tailscaledExe = if (isWindows) "tailscaled.exe" else "tailscaled"
        return File(dir, tailscaleExe) to File(dir, tailscaledExe)
    }

    fun areBinariesPresent(): Boolean {
        val (client, daemon) = getBinaries()
        return client.exists() && daemon.exists()
    }

    fun downloadAndExtractBinaries(onProgress: (String) -> Unit = {}): Pair<File, File> {
        val dir = getTailscaleDirectory()
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val (clientBin, daemonBin) = getBinaries()
        if (areBinariesPresent()) {
            onProgress("Tailscale binaries already present.")
            return clientBin to daemonBin
        }

        onProgress("Fetching latest Tailscale version...")
        val version = fetchLatestVersion()

        val os = PlatformUtils.getCurrentOS()
        val arch = System.getProperty("os.arch").lowercase()
        val isArm = arch.contains("arm") || arch.contains("aarch64")

        val urlString = when (os) {
            "windows" -> "https://pkgs.tailscale.com/stable/tailscale_${version}_windows_amd64.zip"
            "osx" -> if (isArm) {
                "https://pkgs.tailscale.com/stable/tailscale_${version}_darwin_arm64.zip"
            } else {
                "https://pkgs.tailscale.com/stable/tailscale_${version}_darwin_amd64.zip"
            }
            else -> if (isArm) {
                "https://pkgs.tailscale.com/stable/tailscale_${version}_arm64.tgz"
            } else {
                "https://pkgs.tailscale.com/stable/tailscale_${version}_amd64.tgz"
            }
        }

        onProgress("Downloading Tailscale v$version from $urlString...")
        val tempFile = File.createTempFile("tailscale_download", if (urlString.endsWith(".zip")) ".zip" else ".tgz")
        try {
            URI(urlString).toURL().openStream().use { input ->
                Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            onProgress("Extracting Tailscale binaries...")
            if (urlString.endsWith(".zip")) {
                ZipUtils.unzip(tempFile, dir)
            } else {
                val process = ProcessBuilder("tar", "-xzf", tempFile.absolutePath, "-C", dir.absolutePath, "--strip-components=1")
                    .redirectErrorStream(true)
                    .start()
                process.waitFor()
            }

            dir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("tailscale_")) {
                    file.listFiles()?.forEach { subFile ->
                        val target = File(dir, subFile.name)
                        subFile.renameTo(target)
                    }
                    file.deleteRecursively()
                }
            }

        } finally {
            tempFile.delete()
        }

        clientBin.setExecutable(true)
        daemonBin.setExecutable(true)

        onProgress("Tailscale binaries ready at ${dir.absolutePath}")
        return clientBin to daemonBin
    }
}
