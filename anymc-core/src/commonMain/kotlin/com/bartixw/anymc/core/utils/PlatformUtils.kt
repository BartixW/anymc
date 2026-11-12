/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.utils

import java.io.File

interface PlatformPaths {
    val userProgramsDirectory: File
    val globalProgramsDirectory: File
    val userDataDirectory: File
    val sharedStorageDirectory: File
}

class StandardPlatformPaths(private val productName: String) : PlatformPaths {
    private val os: String = System.getProperty("os.name").lowercase()
    private val userHome: String = System.getProperty("user.home")

    override val userProgramsDirectory: File by lazy {
        when {
            os.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA") ?: "$userHome/AppData/Local"
                File(localAppData, "Programs/$productName")
            }
            os.contains("mac") -> File(userHome, "Applications/$productName")
            else -> File(userHome, ".local/bin/${productName.lowercase()}")
        }
    }

    override val globalProgramsDirectory: File by lazy {
        when {
            os.contains("win") -> {
                val programFiles = System.getenv("ProgramFiles") ?: "C:\\Program Files"
                File(programFiles, productName)
            }
            os.contains("mac") -> File("/Applications/$productName")
            else -> File("/usr/local/bin/${productName.lowercase()}")
        }
    }

    override val userDataDirectory: File by lazy {
        when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
                File(appData, productName)
            }
            os.contains("mac") -> File(userHome, "Library/Application Support/$productName")
            else -> File(userHome, ".local/share/${productName.lowercase()}")
        }
    }

    override val sharedStorageDirectory: File by lazy {
        when {
            os.contains("win") -> {
                val publicDir = System.getenv("PUBLIC") ?: "C:\\Users\\Public"
                File(publicDir, "AppData\\Local\\$productName\\")
            }
            os.contains("mac") -> File("/Users/Shared/$productName/")
            else -> File("/opt/$productName/")
        }
    }
}

data class CustomPlatformPaths(
    override val userProgramsDirectory: File,
    override val globalProgramsDirectory: File,
    override val userDataDirectory: File,
    override val sharedStorageDirectory: File
) : PlatformPaths

object PlatformUtils {

    private var _paths: PlatformPaths? = null
    private val paths: PlatformPaths
        get() = _paths ?: throw IllegalStateException("PlatformUtils not initialized. Call initialize(PlatformPaths) first.")

    fun initialize(paths: PlatformPaths) {
        _paths = paths
    }

    /**
     * User-specific program files (Per-User Installation)
     */
    val userProgramsDirectory: File get() = paths.userProgramsDirectory

    /**
     * Program files for all users (Global installation)
     */
    val globalProgramsDirectory: File get() = paths.globalProgramsDirectory

    /**
     * Program data for a given user (Configs, Profiles)
     */
    val userDataDirectory: File get() = paths.userDataDirectory

    /**
     * Program data for all users WITHOUT admin privileges
     */
    val sharedStorageDirectory: File get() = paths.sharedStorageDirectory

    fun getCurrentOS(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> "windows"
            os.contains("mac") -> "osx"
            else -> "linux"
        }
    }

    fun openBrowser(url: String) {
        val os = System.getProperty("os.name").lowercase()
        val rt = Runtime.getRuntime()
        try {
            when {
                os.contains("win") -> rt.exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", url))
                os.contains("mac") -> rt.exec(arrayOf("open", url))
                else -> { // Linux/Unix
                    val browsers = arrayOf("xdg-open", "gnome-open", "kde-open", "firefox", "google-chrome", "chromium")
                    var opened = false
                    for (browser in browsers) {
                        try {
                            rt.exec(arrayOf(browser, url))
                            opened = true
                            break
                        } catch (e: Exception) {
                            continue
                        }
                    }
                    if (!opened) throw Exception("Could not find a browser to open URL")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
