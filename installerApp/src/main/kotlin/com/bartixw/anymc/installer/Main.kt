/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.installer

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.app_icon
import anymc.shared.generated.resources.installer_title
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.core.utils.StandardPlatformPaths
import com.bartixw.anymc.installer.ui.InstallerApp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension
import java.io.File
import javax.swing.UIManager
import javax.swing.plaf.metal.MetalLookAndFeel

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    if (!args.contains("--no-unpack") && ensureNativeEnvironmentAndRelaunchIfNeeded(args)) {
        return
    }

    val argsMap = args.toList().chunked(2).associate { if (it.size == 2) it[0] to it[1] else it[0] to "" }

    PlatformUtils.initialize(StandardPlatformPaths("AnyMC"))

    try {
        UIManager.setLookAndFeel(MetalLookAndFeel())
    } catch (e: Exception) {
        e.printStackTrace()
    }

    application {
        val exceptionHandlerFactory = WindowExceptionHandlerFactory {
            WindowExceptionHandler { throwable ->
                println("Installer error: ${throwable.message}")
                throwable.printStackTrace()
            }
        }

        CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides exceptionHandlerFactory) {
            val state = rememberWindowState(
                width = 800.dp,
                height = 580.dp
            )

            Window(
                onCloseRequest = ::exitApplication,
                title = stringResource(Res.string.installer_title),
                state = state,
                icon = painterResource(Res.drawable.app_icon),
                undecorated = true,
                resizable = false
            ) {
                window.minimumSize = Dimension(800, 580)

                InstallerApp(
                    onMinimize = { state.isMinimized = true },
                    onClose = ::exitApplication,
                    windowDraggableArea = { modifier, content ->
                        this@Window.WindowDraggableArea(modifier) {
                            content()
                        }
                    },
                    apiHost = argsMap["--api-host"] ?: "api.github.com"
                )
            }
        }
    }
}

private fun ensureNativeEnvironmentAndRelaunchIfNeeded(args: Array<String>): Boolean {
    val tempRunDir = File(System.getProperty("java.io.tmpdir"), "anymc_installer_run")
    val libDir = File(tempRunDir, "lib")
    val serverDir = File(libDir, "server")
    val xawtDir = File(libDir, "xawt")
    val headlessDir = File(libDir, "headless")
    val binDir = File(tempRunDir, "bin")
    val binServerDir = File(binDir, "server")

    tempRunDir.mkdirs()
    libDir.mkdirs()
    serverDir.mkdirs()
    xawtDir.mkdirs()
    headlessDir.mkdirs()
    binDir.mkdirs()
    binServerDir.mkdirs()

    // 1. Extract embedded native libraries into /tmp/anymc_installer_run
    extractEmbeddedNativesTo(tempRunDir, libDir, serverDir, xawtDir, headlessDir, binDir, binServerDir)

    // 2. Set System Properties in BOTH parent and child processes
    val allLibDirs = listOf(serverDir, xawtDir, headlessDir, libDir, binServerDir, binDir, tempRunDir)
    val libraryPathStr = allLibDirs.joinToString(File.pathSeparator) { it.absolutePath }

    System.setProperty("java.home", tempRunDir.absolutePath)
    System.setProperty("java.library.path", libraryPathStr)
    System.setProperty("org.lwjgl.librarypath", tempRunDir.absolutePath)

    val isLoaded = System.getenv("ANYMC_NATIVE_LOADED") == "1"
    if (isLoaded) return false

    val isNativeImage = try {
        val clazz = Class.forName("org.graalvm.nativeimage.ImageInfo")
        val method = clazz.getMethod("inImageCode")
        (method.invoke(null) as? Boolean) == true
    } catch (_: Throwable) {
        System.getProperty("org.graalvm.nativeimage.imagepath") != null
    }

    if (!isNativeImage) return false

    val runningExePath = try {
        val cmd = ProcessHandle.current().info().command().orElse(null)
        if (!cmd.isNullOrBlank() && File(cmd).exists()) {
            cmd
        } else {
            System.getProperty("org.graalvm.nativeimage.imagepath")
        }
    } catch (_: Throwable) {
        System.getProperty("org.graalvm.nativeimage.imagepath")
    }

    if (runningExePath.isNullOrBlank() || !File(runningExePath).exists()) return false

    return try {
        // 3. Copy running binary into /tmp/anymc_installer_run/
        val sourceExe = File(runningExePath)
        val targetExe = File(tempRunDir, sourceExe.name)
        if (sourceExe.canonicalPath != targetExe.canonicalPath) {
            sourceExe.copyTo(targetExe, overwrite = true)
        }
        if (!PlatformUtils.getCurrentOS().lowercase().contains("win")) {
            targetExe.setExecutable(true)
        }

        // 4. Relaunch process inside /tmp/anymc_installer_run/ with LD_LIBRARY_PATH & ANYMC_NATIVE_LOADED=1
        val os = PlatformUtils.getCurrentOS().lowercase()
        val pb = ProcessBuilder(listOf(targetExe.absolutePath) + args.toList())
        val env = pb.environment()
        env["ANYMC_NATIVE_LOADED"] = "1"

        if (os.contains("win")) {
            val pathKey = env.keys.firstOrNull { it.equals("PATH", ignoreCase = true) } ?: "PATH"
            val winPathsStr = allLibDirs.joinToString(";") { it.absolutePath }
            env[pathKey] = "$winPathsStr;" + (env[pathKey] ?: "")
        } else {
            val ldKey = "LD_LIBRARY_PATH"
            val existingLd = env[ldKey] ?: ""
            val linuxPathsStr = allLibDirs.joinToString(":") { it.absolutePath }
            env[ldKey] = "$linuxPathsStr:$existingLd"
        }

        println("Main: Self-extracting native payload to ${tempRunDir.absolutePath} and executing...")
        val process = pb.inheritIO().start()
        val exitCode = process.waitFor()

        tempRunDir.deleteRecursively()
        kotlin.system.exitProcess(exitCode)
    } catch (e: Exception) {
        println("Main: Self-extract relaunch notice: ${e.message}")
        false
    }
}

private fun extractEmbeddedNativesTo(
    tempDir: File,
    libDir: File,
    serverDir: File,
    xawtDir: File,
    headlessDir: File,
    binDir: File,
    binServerDir: File
) {
    try {
        val os = PlatformUtils.getCurrentOS().lowercase()
        val ext = when {
            os.contains("win") -> ".dll"
            os.contains("mac") -> ".dylib"
            else -> ".so"
        }

        val libNames = listOf(
            "jvm", "java", "verify", "nio", "net", "awt", "awt_xawt", "awt_headless", "awt_lwawt", "jawt", "fontmanager", "management", "management_ext", "lwjgl", "skiko"
        )
        val classLoader = Thread.currentThread().contextClassLoader ?: object {}.javaClass.classLoader

        for (libName in libNames) {
            val baseName = if (os.contains("win")) "$libName$ext" else "lib$libName$ext"
            val targetPaths = listOf(
                "native_libs/$baseName",
                "native_libs/lib/$baseName",
                "native_libs/lib/server/$baseName",
                "native_libs/lib/xawt/$baseName",
                "native_libs/lib/headless/$baseName",
                "native_libs/bin/$baseName",
                "native_libs/bin/server/$baseName"
            )

            for (resName in targetPaths) {
                try {
                    val stream = classLoader.getResourceAsStream(resName)
                        ?: ClassLoader.getSystemResourceAsStream(resName)

                    if (stream != null) {
                        stream.use { input ->
                            val subPath = resName.removePrefix("native_libs/")
                            val targetFile = File(tempDir, subPath)
                            targetFile.parentFile?.mkdirs()
                            if (!targetFile.exists() || targetFile.length() == 0L) {
                                targetFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                                println("Main: Extracted native resource: $resName -> ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Main: Failed extracting $resName: ${e.message}")
                }
            }
        }

        // Copy critical shared libraries across all lib subdirectories so dyld/ld.so finds them regardless of directory
        val copyDirs = listOf(tempDir, libDir, serverDir, xawtDir, headlessDir, binDir, binServerDir)
        for (f in (tempDir.listFiles() ?: emptyArray()) + (libDir.listFiles() ?: emptyArray())) {
            if (f.isFile && (f.name.endsWith(".so") || f.name.endsWith(".dll") || f.name.endsWith(".dylib"))) {
                for (destDir in copyDirs) {
                    val dest = File(destDir, f.name)
                    if (!dest.exists()) {
                        try { f.copyTo(dest, overwrite = true) } catch (_: Exception) {}
                    }
                }
            }
        }

        // Explicitly pre-load extracted native libraries via absolute path System.load()
        val libPrefix = if (os.contains("win")) "" else "lib"
        val preloadOrder = listOf(
            "jvm", "java", "verify", "nio", "net", "awt", "awt_xawt", "awt_headless", "awt_lwawt", "jawt", "fontmanager", "management", "management_ext",
            "skiko"
        )

        for (name in preloadOrder) {
            val fileName = "$libPrefix$name$ext"
            val candidates = listOf(
                File(serverDir, fileName),
                File(libDir, fileName),
                File(tempDir, fileName),
                File(xawtDir, fileName),
                File(headlessDir, fileName),
                File(binServerDir, fileName),
                File(binDir, fileName)
            )
            val fileToLoad = candidates.firstOrNull { it.exists() && it.length() > 0L }
            if (fileToLoad != null) {
                try {
                    System.load(fileToLoad.absolutePath)
                    println("Main: Explicitly pre-loaded native library: ${fileToLoad.absolutePath}")
                } catch (e: Throwable) {
                    println("Main: Note on pre-loading ${fileToLoad.name}: ${e.message}")
                }
            }
        }

    } catch (e: Exception) {
        println("Main: Failed to extract embedded natives: ${e.message}")
    }
}
