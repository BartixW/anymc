/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.utils

import com.bartixw.anymc.minecraft.data.*
import com.bartixw.anymc.core.utils.PlatformUtils
import java.io.File
import java.util.zip.ZipFile

object LaunchUtils {
    fun isJvmRuleAllowed(rules: List<JVMRule>, currentOS: String): Boolean {
        if (rules.isEmpty()) return true
        var allowed = false
        val currentArch = System.getProperty("os.arch").lowercase()
        for (rule in rules) {
            val osMatch = rule.os == null || rule.os.name == null || rule.os.name.value == currentOS
            val archMatch = rule.os == null || rule.os.arch == null || rule.os.arch == currentArch || (rule.os.arch == "x86" && (currentArch == "amd64" || currentArch == "x86_64"))
            if (osMatch && archMatch) allowed = rule.action == Action.Allow
        }
        return allowed
    }

    fun isGameRuleAllowed(rules: List<GameRule>, activeFeatures: Set<String>): Boolean {
        if (rules.isEmpty()) return true
        var allowed = false
        for (rule in rules) {
            val features = rule.features
            if (features == null) {
                allowed = rule.action == Action.Allow
                continue
            }

            val demoMatch = features.isDemoUser == null || features.isDemoUser == activeFeatures.contains("is_demo_user")
            val quickPlayMatch = (features.hasQuickPlaysSupport == null || features.hasQuickPlaysSupport == activeFeatures.contains("has_quick_plays_support")) &&
                    (features.isQuickPlaySingleplayer == null || features.isQuickPlaySingleplayer == activeFeatures.contains("is_quick_play_singleplayer")) &&
                    (features.isQuickPlayMultiplayer == null || features.isQuickPlayMultiplayer == activeFeatures.contains("is_quick_play_multiplayer")) &&
                    (features.isQuickPlayRealms == null || features.isQuickPlayRealms == activeFeatures.contains("is_quick_play_realms"))

            if (demoMatch && quickPlayMatch) {
                allowed = rule.action == Action.Allow
            }
        }
        return allowed
    }

    fun shouldIncludeJvmArg(arg: String, javaVersion: Long): Boolean {
        return !(arg.startsWith("--sun-misc-unsafe-memory-access=") && javaVersion < 23)
    }

    fun resolveJavaPath(baseDir: File, required: JavaVersion): String {
        val runtimeDir = PlatformUtils.sharedStorageDirectory.resolve("java_runtimes").resolve(required.component)
        val javaExe = if (PlatformUtils.getCurrentOS() == "windows") "bin/java.exe" else "bin/java"
        val javaPath = runtimeDir.resolve(javaExe)
        val successMarker = runtimeDir.resolve(".success")

        if (javaPath.exists() && successMarker.exists()) return javaPath.absolutePath

        val systemMajor = System.getProperty("java.version").substringBefore(".").substringBefore("-").toIntOrNull() ?: 0
        if (systemMajor >= required.majorVersion) return "java"

        throw IllegalStateException("No java installation found at ${javaPath.absolutePath} and system java version $systemMajor is lower than required ${required.majorVersion}")
    }

    fun mergeLibraries(data: Client, libraryMap: MutableMap<String, Library>) {
        val librariesToMerge = mutableListOf<Library>()
        librariesToMerge.addAll(data.libraries)
        data.mavenFiles?.let { librariesToMerge.addAll(it) }

        librariesToMerge.forEach { lib ->
            if (!lib.isAllowed(PlatformUtils.getCurrentOS())) {
                return@forEach
            }

            val parts = lib.name.split(":")
            if (parts.size >= 3) {
                val group = parts[0]
                val artifact = parts[1]
                val classifier = if (parts.size >= 4) parts[3] else ""
                val identity = if (classifier.isNotEmpty()) "$group:$artifact:$classifier" else "$group:$artifact"

                if (!libraryMap.containsKey(identity)) {
                    libraryMap[identity] = lib
                }
            }
        }
    }

    fun extractNatives(zipFile: File, destination: File) {
        try {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (!entry.isDirectory && (entry.name.endsWith(".dll") || entry.name.endsWith(".so") || entry.name.endsWith(".dylib"))) {
                        val outputFile = File(destination, entry.name.substringAfterLast("/"))
                        zip.getInputStream(entry).use { input -> outputFile.outputStream().use { output -> input.copyTo(output) } }
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to extract natives from ${zipFile.absolutePath}: ${e.message}")
            // Delete the corrupted file so it gets redownloaded next time
            if (zipFile.exists()) zipFile.delete()
            throw Exception("Corrupted library file detected: ${zipFile.name}. It has been removed, please try installing again.", e)
        }
    }

    fun resolvePlaceholder(text: String, placeholders: Map<String, String>): String {
        var result = text
        placeholders.forEach { (k, v) -> result = result.replace($$"${$$k}", v) }
        return result
    }

    fun libraryToPath(name: String): String {
        val parts = name.split(":")
        val group = parts[0].replace(".", "/")
        val artifact = parts[1]
        val version = parts[2]
        val classifier = if (parts.size > 3) "-${parts[3]}" else ""
        return "$group/$artifact/$version/$artifact-$version$classifier.jar"
    }

    data class MavenCoordinate(
        val groupId: String,
        val artifactId: String,
        val version: String,
        val classifier: String? = null
    )

    fun parseMavenCoordinate(name: String): MavenCoordinate? {
        val parts = name.split(":")
        if (parts.size < 3) return null

        val groupId = parts[0]
        val artifactId = parts[1]
        val version = parts[2]

        val classifier = if (parts.size >= 4) parts[3] else null

        return MavenCoordinate(groupId, artifactId, version, classifier)
    }

    /**
     * Compares two version strings. Returns a positive number if v1 > v2, negative if v1 < v2, 0 if equal.
     * Simple implementation focusing on numeric segments and stability (pre-release vs stable).
     */
    fun compareVersions(v1: String, v2: String): Int {
        val preReleaseKeywords = listOf("alpha", "beta", "rc", "snapshot", "preview", "pre")
        
        val isPre1 = preReleaseKeywords.any { v1.contains(it, ignoreCase = true) }
        val isPre2 = preReleaseKeywords.any { v2.contains(it, ignoreCase = true) }

        // Rule 1: Prioritize Stable over Pre-release (e.g., 5.0.4 > 6.0-alpha-3)
        if (!isPre1 && isPre2) return 1
        if (isPre1 && !isPre2) return -1

        val s1 = v1.split(".", "-", "+")
        val s2 = v2.split(".", "-", "+")
        val length = maxOf(s1.size, s2.size)

        for (i in 0 until length) {
            val p1 = s1.getOrNull(i)
            val p2 = s2.getOrNull(i)

            if (p1 == p2) continue
            if (p1 == null) return -1
            if (p2 == null) return 1

            val i1 = p1.toIntOrNull()
            val i2 = p2.toIntOrNull()

            if (i1 != null && i2 != null) {
                if (i1 != i2) return i1 - i2
            } else {
                // Prioritize 'jre' over 'android' for Guava if both are present
                if (p1 == "jre" && p2 == "android") return 1
                if (p1 == "android" && p2 == "jre") return -1

                val res = p1.compareTo(p2, ignoreCase = true)
                if (res != 0) return res
            }
        }
        return 0
    }
}
