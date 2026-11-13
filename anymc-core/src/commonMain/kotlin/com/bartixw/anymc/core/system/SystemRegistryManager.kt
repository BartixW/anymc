/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.system

import com.bartixw.anymc.core.utils.PlatformUtils
import java.io.File

object SystemRegistryManager {

    fun registerApplicationInSystem(
        appName: String = "AnyMC Launcher",
        version: String = "1.0.0",
        executableFile: File,
        iconFile: File? = null,
        createDesktopShortcut: Boolean = true,
        createStartMenuShortcut: Boolean = true
    ): Boolean {
        val os = PlatformUtils.getCurrentOS()
        return try {
            when (os) {
                "windows" -> registerWindows(appName, version, executableFile, iconFile, createDesktopShortcut, createStartMenuShortcut)
                "linux" -> registerLinux(appName, version, executableFile, iconFile, createDesktopShortcut, createStartMenuShortcut)
                "osx" -> registerMacOS(appName, version, executableFile, iconFile, createDesktopShortcut)
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun registerUriScheme(
        scheme: String = "anymc",
        appName: String = "AnyMC Launcher",
        executableFile: File
    ): Boolean {
        val os = PlatformUtils.getCurrentOS()
        return try {
            when (os) {
                "windows" -> registerWindowsUriScheme(scheme, appName, executableFile)
                "linux" -> registerLinuxUriScheme(scheme, appName, executableFile)
                "osx" -> registerMacOSUriScheme(scheme, appName, executableFile)
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun unregisterApplicationInSystem(appName: String = "AnyMC Launcher", scheme: String = "anymc"): Boolean {
        val os = PlatformUtils.getCurrentOS()
        return try {
            when (os) {
                "windows" -> unregisterWindows(appName, scheme)
                "linux" -> unregisterLinux(appName, scheme)
                "osx" -> unregisterMacOS(appName, scheme)
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun detectActualInstallationDirectory(): File {
        try {
            val imagePath = System.getProperty("org.graalvm.nativeimage.imagepath")
            if (!imagePath.isNullOrBlank()) {
                val exeFile = File(imagePath)
                if (exeFile.exists()) {
                    return getRootInstallDirFromLocation(exeFile)
                }
            }

            val codeSource = SystemRegistryManager::class.java.protectionDomain?.codeSource?.location
            if (codeSource != null) {
                val file = File(codeSource.toURI())
                if (file.exists()) {
                    return getRootInstallDirFromLocation(file)
                }
            }
        } catch (_: Exception) {}

        val userDir = File(System.getProperty("user.dir"))
        if (userDir.exists()) {
            return getRootInstallDirFromLocation(userDir)
        }

        return PlatformUtils.userProgramsDirectory
    }

    private fun getRootInstallDirFromLocation(locationFile: File): File {
        var dir: File? = if (locationFile.isFile) locationFile.parentFile else locationFile
        while (dir != null) {
            val name = dir.name.lowercase()
            if (name == "app" || name == "lib" || name == "bin") {
                dir = dir.parentFile
            } else {
                break
            }
        }
        return dir ?: if (locationFile.isFile) locationFile.parentFile ?: locationFile else locationFile
    }

    fun uninstallLauncher(
        installDir: File = detectActualInstallationDirectory(),
        deleteUserData: Boolean = false,
        deleteSharedStorage: Boolean = false,
        appName: String = "AnyMC Launcher",
        scheme: String = "anymc"
    ): Boolean {
        return try {
            // 1. Remove OS shortcuts & registry entries
            unregisterApplicationInSystem(appName, scheme)

            // 2. Schedule background self-deletion helper process
            scheduleSelfDeletion(
                installDir = installDir,
                deleteUserData = deleteUserData,
                deleteSharedStorage = deleteSharedStorage
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun scheduleSelfDeletion(
        installDir: File,
        deleteUserData: Boolean,
        deleteSharedStorage: Boolean
    ) {
        val os = PlatformUtils.getCurrentOS()
        val installPath = installDir.absolutePath
        val userPath = PlatformUtils.userDataDirectory.absolutePath
        val storagePath = PlatformUtils.sharedStorageDirectory.absolutePath

        if (os == "windows") {
            val scriptContent = buildString {
                appendLine("@echo off")
                appendLine("timeout /t 2 /nobreak > NUL")
                appendLine("if exist \"$installPath\" rmdir /s /q \"$installPath\"")
                if (deleteUserData) {
                    appendLine("if exist \"$userPath\" rmdir /s /q \"$userPath\"")
                }
                if (deleteSharedStorage) {
                    appendLine("if exist \"$storagePath\" rmdir /s /q \"$storagePath\"")
                }
                appendLine("del \"%~f0\"")
            }

            val tempBat = File(System.getProperty("java.io.tmpdir"), "anymc_uninstall.bat")
            tempBat.writeText(scriptContent)

            ProcessBuilder("cmd.exe", "/c", "start", "/b", tempBat.absolutePath)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
        } else {
            // Linux / macOS
            val commands = mutableListOf<String>()
            commands.add("sleep 1")
            commands.add("rm -rf \"$installPath\"")
            if (deleteUserData) {
                commands.add("rm -rf \"$userPath\"")
            }
            if (deleteSharedStorage) {
                commands.add("rm -rf \"$storagePath\"")
            }

            val shScript = commands.joinToString(" && ")
            ProcessBuilder("sh", "-c", "nohup sh -c '$shScript' >/dev/null 2>&1 &")
                .start()
        }
    }

    // --- Windows Implementation ---

    private fun getWindowsSpecialFolder(folderName: String): File? {
        return try {
            val script = "\$WshShell = New-Object -ComObject WScript.Shell; Write-Output \$WshShell.SpecialFolders('$folderName')"
            val process = ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (output.isNotBlank()) File(output) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun registerWindows(
        appName: String,
        version: String,
        executableFile: File,
        iconFile: File?,
        createDesktopShortcut: Boolean,
        createStartMenuShortcut: Boolean
    ): Boolean {
        val userHome = System.getProperty("user.home")
        val exePath = executableFile.absolutePath
        val iconPath = iconFile?.absolutePath ?: exePath
        val installDir = executableFile.parentFile.absolutePath

        // 1. Registry Uninstall Entry
        val regKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\AnyMC"
        val uninstallCmd = "\"$exePath\" --uninstall"
        val quietUninstallCmd = "\"$exePath\" --uninstall --quiet"

        runCommand("reg", "add", regKey, "/v", "DisplayName", "/t", "REG_SZ", "/d", appName, "/f")
        runCommand("reg", "add", regKey, "/v", "DisplayVersion", "/t", "REG_SZ", "/d", version, "/f")
        runCommand("reg", "add", regKey, "/v", "Publisher", "/t", "REG_SZ", "/d", "BartixW", "/f")
        runCommand("reg", "add", regKey, "/v", "InstallLocation", "/t", "REG_SZ", "/d", installDir, "/f")
        runCommand("reg", "add", regKey, "/v", "DisplayIcon", "/t", "REG_SZ", "/d", iconPath, "/f")
        runCommand("reg", "add", regKey, "/v", "UninstallString", "/t", "REG_SZ", "/d", uninstallCmd, "/f")
        runCommand("reg", "add", regKey, "/v", "QuietUninstallString", "/t", "REG_SZ", "/d", quietUninstallCmd, "/f")
        runCommand("reg", "add", regKey, "/v", "NoModify", "/t", "REG_DWORD", "/d", "1", "/f")
        runCommand("reg", "add", regKey, "/v", "NoRepair", "/t", "REG_DWORD", "/d", "1", "/f")

        val resolveWindowsFolder = { folderType: String, fallbackPath: String ->
            val file = File(fallbackPath)
            if (file.exists()) file else {
                getWindowsSpecialFolder(folderType) ?: file
            }
        }

        // 2. Start Menu Shortcut
        if (createStartMenuShortcut) {
            val startMenuDir = resolveWindowsFolder("Programs", "$userHome/AppData/Roaming/Microsoft/Windows/Start Menu/Programs")
            startMenuDir.mkdirs()
            val shortcutFile = File(startMenuDir, "$appName.lnk")
            createWindowsShortcut(shortcutFile.absolutePath, exePath, installDir, iconPath)
        }

        // 3. Desktop Shortcut
        if (createDesktopShortcut) {
            val desktopDir = resolveWindowsFolder("Desktop", "$userHome/Desktop")
            desktopDir.mkdirs()
            val shortcutFile = File(desktopDir, "$appName.lnk")
            createWindowsShortcut(shortcutFile.absolutePath, exePath, installDir, iconPath)
        }

        return true
    }

    private fun unregisterWindows(appName: String, scheme: String): Boolean {
        val userHome = System.getProperty("user.home")
        val regUninstallKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\AnyMC"
        val regSchemeKey = "HKCU\\Software\\Classes\\$scheme"

        runCommand("reg", "delete", regUninstallKey, "/f")
        runCommand("reg", "delete", regSchemeKey, "/f")

        val resolveWindowsFolder = { folderType: String, fallbackPath: String ->
            val file = File(fallbackPath)
            if (file.exists()) file else {
                getWindowsSpecialFolder(folderType) ?: file
            }
        }

        val startMenuDir = resolveWindowsFolder("Programs", "$userHome/AppData/Roaming/Microsoft/Windows/Start Menu/Programs")
        val startMenuShortcut = File(startMenuDir, "$appName.lnk")
        if (startMenuShortcut.exists()) startMenuShortcut.delete()

        val desktopDir = resolveWindowsFolder("Desktop", "$userHome/Desktop")
        val desktopShortcut = File(desktopDir, "$appName.lnk")
        if (desktopShortcut.exists()) desktopShortcut.delete()

        return true
    }

    private fun createWindowsShortcut(shortcutPath: String, targetPath: String, workingDir: String, iconPath: String) {
        val cleanIconLocation = if (iconPath.endsWith(",0")) iconPath.removeSuffix(",0") else iconPath
        val escapePs = { s: String -> s.replace("'", "''") }

        val psScript = """
            ${'$'}WshShell = New-Object -ComObject WScript.Shell
            ${'$'}Shortcut = ${'$'}WshShell.CreateShortcut('${escapePs(shortcutPath)}')
            ${'$'}Shortcut.TargetPath = '${escapePs(targetPath)}'
            ${'$'}Shortcut.WorkingDirectory = '${escapePs(workingDir)}'
            ${'$'}Shortcut.IconLocation = '${escapePs(cleanIconLocation)}'
            ${'$'}Shortcut.Save()
        """.trimIndent()

        val tempScript = File.createTempFile("create_shortcut", ".ps1")
        try {
            tempScript.writeText(psScript)
            runCommand("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", tempScript.absolutePath)
        } finally {
            tempScript.delete()
        }
    }

    private fun registerWindowsUriScheme(scheme: String, appName: String, executableFile: File): Boolean {
        val regKey = "HKCU\\Software\\Classes\\$scheme"
        val cmdKey = "$regKey\\shell\\open\\command"
        val commandValue = "\"${executableFile.absolutePath}\" \"%1\""

        runCommand("reg", "add", regKey, "/ve", "/t", "REG_SZ", "/d", "URL:$appName Protocol", "/f")
        runCommand("reg", "add", regKey, "/v", "URL Protocol", "/t", "REG_SZ", "/d", "", "/f")
        runCommand("reg", "add", cmdKey, "/ve", "/t", "REG_SZ", "/d", commandValue, "/f")
        return true
    }

    // --- Linux Implementation ---

    private fun registerLinux(
        appName: String,
        version: String,
        executableFile: File,
        iconFile: File?,
        createDesktopShortcut: Boolean,
        createStartMenuShortcut: Boolean
    ): Boolean {
        val userHome = System.getProperty("user.home")
        val appsDir = File(userHome, ".local/share/applications")
        appsDir.mkdirs()

        val iconPath = iconFile?.absolutePath ?: "anymc"
        val desktopContent = """
            [Desktop Entry]
            Version=1.0
            Type=Application
            Name=$appName
            Comment=AnyMC Launcher
            Exec="${executableFile.absolutePath}" %u
            Icon=$iconPath
            Terminal=false
            Categories=Game;Entertainment;
            MimeType=x-scheme-handler/anymc;
            StartupWMClass=AnyMC
        """.trimIndent()

        if (createStartMenuShortcut) {
            val desktopFile = File(appsDir, "anymc.desktop")
            desktopFile.writeText(desktopContent)
            runCommand("chmod", "+x", desktopFile.absolutePath)
            runCommand("update-desktop-database", appsDir.absolutePath)
        }

        if (createDesktopShortcut) {
            val desktopDir = File(userHome, "Desktop")
            if (desktopDir.exists()) {
                val shortcutFile = File(desktopDir, "anymc.desktop")
                shortcutFile.writeText(desktopContent)
                runCommand("chmod", "+x", shortcutFile.absolutePath)
            }
        }

        return true
    }

    private fun unregisterLinux(appName: String, scheme: String): Boolean {
        val userHome = System.getProperty("user.home")
        val appsDir = File(userHome, ".local/share/applications")
        val desktopFile = File(appsDir, "anymc.desktop")
        if (desktopFile.exists()) desktopFile.delete()

        val desktopDirShortcut = File(userHome, "Desktop/anymc.desktop")
        if (desktopDirShortcut.exists()) desktopDirShortcut.delete()

        runCommand("update-desktop-database", appsDir.absolutePath)
        return true
    }

    private fun registerLinuxUriScheme(scheme: String, appName: String, executableFile: File): Boolean {
        val userHome = System.getProperty("user.home")
        val desktopFile = File(userHome, ".local/share/applications/anymc.desktop")
        if (desktopFile.exists()) {
            runCommand("xdg-mime", "default", "anymc.desktop", "x-scheme-handler/$scheme")
        }
        return true
    }

    // --- macOS Implementation ---

    private fun registerMacOS(
        appName: String,
        version: String,
        executableFile: File,
        iconFile: File?,
        createDesktopShortcut: Boolean
    ): Boolean {
        val userHome = System.getProperty("user.home")
        val appBundleDir = File(userHome, "Applications/$appName.app")
        val contentsDir = File(appBundleDir, "Contents")
        val macOSDir = File(contentsDir, "MacOS")
        val resourcesDir = File(contentsDir, "Resources")

        macOSDir.mkdirs()
        resourcesDir.mkdirs()

        val targetExe = File(macOSDir, appName)
        if (executableFile.exists() && executableFile.canonicalPath != targetExe.canonicalPath) {
            executableFile.copyTo(targetExe, overwrite = true)
        }
        runCommand("chmod", "+x", targetExe.absolutePath)

        val plistFile = File(contentsDir, "Info.plist")
        val plistContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleExecutable</key>
                <string>$appName</string>
                <key>CFBundleIdentifier</key>
                <string>com.bartixw.anymc</string>
                <key>CFBundleName</key>
                <string>$appName</string>
                <key>CFBundlePackageType</key>
                <string>APPL</string>
                <key>CFBundleShortVersionString</key>
                <string>$version</string>
                <key>CFBundleURLTypes</key>
                <array>
                    <dict>
                        <key>CFBundleURLName</key>
                        <string>AnyMC Protocol</string>
                        <key>CFBundleURLSchemes</key>
                        <array>
                            <string>anymc</string>
                        </array>
                    </dict>
                </array>
            </dict>
            </plist>
        """.trimIndent()
        plistFile.writeText(plistContent)

        runCommand("/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister", "-f", appBundleDir.absolutePath)

        return true
    }

    private fun unregisterMacOS(appName: String, scheme: String): Boolean {
        val userHome = System.getProperty("user.home")
        val appBundleDir = File(userHome, "Applications/$appName.app")
        if (appBundleDir.exists()) {
            appBundleDir.deleteRecursively()
        }
        return true
    }

    private fun registerMacOSUriScheme(scheme: String, appName: String, executableFile: File): Boolean {
        val userHome = System.getProperty("user.home")
        val appBundleDir = File(userHome, "Applications/$appName.app")
        if (appBundleDir.exists()) {
            runCommand("/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister", "-f", appBundleDir.absolutePath)
        }
        return true
    }

    // Helper process launcher
    private fun runCommand(vararg command: String): Boolean {
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
}
