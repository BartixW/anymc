/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.helpers

import java.io.File
import java.net.URISyntaxException
import java.util.Locale

object HelperResolver {
    private val osName = System.getProperty("os.name").lowercase(Locale.ROOT)

    private val platformFolder = when {
        osName.contains("win") -> "windows"
        osName.contains("mac") -> "macos"
        else -> "linux"
    }

    private val exeName = if (platformFolder == "windows") "AnyMCAuthHelper.exe" else "AnyMCAuthHelper"

    fun getAuthHelperPath(): String {
        val devPath = File("build/authhelper/$exeName")
        if (devPath.exists()) {
            return devPath.absolutePath
        }

        val jarDir = getJarExecutionDirectory()

        val possiblePaths = listOf(
            File(jarDir, "authhelper/$exeName"),
            File(jarDir, "../authhelper/$exeName"),
            File(jarDir, "app/authhelper/$exeName"),
            File(jarDir, "../lib/app/authhelper/$exeName"),
            File(jarDir, "../../authhelper/$exeName"),
            File(jarDir, "Resources/authhelper/$exeName")
        )

        for (path in possiblePaths) {
            if (path.exists()) {
                if (platformFolder != "windows") {
                    path.setExecutable(true)
                }
                return path.absolutePath
            }
        }

        throw IllegalStateException("Nie znaleziono helpera natywnego w lokalizacji: ${File(jarDir, "authhelper/$exeName").absolutePath}")
    }

    private fun getJarExecutionDirectory(): File {
        return try {
            val codeSourceUri = HelperResolver::class.java.protectionDomain.codeSource.location.toURI()
            val jarFile = File(codeSourceUri)

            if (jarFile.isFile) {
                jarFile.parentFile
            } else {
                File(System.getProperty("user.dir"))
            }
        } catch (_: URISyntaxException) {
            File(System.getProperty("user.dir"))
        }
    }
}
