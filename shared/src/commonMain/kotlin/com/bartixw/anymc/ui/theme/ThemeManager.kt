/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.theme

import com.bartixw.anymc.core.utils.PlatformUtils
import kotlinx.serialization.json.Json

object ThemeManager {
    val themesDir by lazy { PlatformUtils.userDataDirectory.resolve("themes") }
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private var availableThemesCache: List<String>? = null
    private val themeColorsCache = mutableMapOf<String, MinecraftColors>()

    val builtInThemes = mapOf(
        "AnyMC" to ClassicMinecraftColors,
        "Cave Dark" to CaveDarkColors
    )

    private val themeAliases = mapOf(
        "default" to "AnyMC",
        "cave_dark" to "Cave Dark"
    )

    /**
     * Returns a list of all available themes based on JSON files in the themes directory.
     */
    fun getAvailableThemes(): List<String> {
        val cached = availableThemesCache
        if (cached != null) return cached

        if (!themesDir.exists()) return builtInThemes.keys.toList()

        val externalThemes = themesDir.listFiles { _, name -> name.endsWith(".json") }
            ?.map { it.nameWithoutExtension }
            ?.filter { it !in themeAliases.keys } // Ignore legacy alias filenames if they exist
            ?: emptyList()

        val allThemes = (externalThemes + builtInThemes.keys).distinct().sorted()
        availableThemesCache = allThemes
        return allThemes
    }

    /**
     * Loads theme colors by name. Prioritizes external JSON files, falls back to built-in themes.
     */
    fun getThemeColors(name: String): MinecraftColors {
        val cached = themeColorsCache[name]
        if (cached != null) return cached

        val themeFile = themesDir.resolve("$name.json")
        if (themeFile.exists()) {
            try {
                val colors = json.decodeFromString<MinecraftColors>(themeFile.readText())
                themeColorsCache[name] = colors
                return colors
            } catch (e: Exception) {
                println("Error loading theme $name: ${e.message}")
            }
        }

        val resolvedName = themeAliases[name.lowercase()] ?: name
        val colors = builtInThemes.entries.firstOrNull { it.key.equals(resolvedName, ignoreCase = true) }?.value 
            ?: builtInThemes["AnyMC"] 
            ?: ClassicMinecraftColors
        
        themeColorsCache[name] = colors
        return colors
    }

    fun initialize() {
        if (!themesDir.exists()) {
            themesDir.mkdirs()
        }
        // Export default themes as files so they are visible in the list and editable
        exportTheme("AnyMC", ClassicMinecraftColors)
        exportTheme("Cave Dark", CaveDarkColors)
        
        // Initial cache population
        getAvailableThemes()
    }

    private fun exportTheme(name: String, colors: MinecraftColors) {
        val file = themesDir.resolve("$name.json")
        if (!file.exists()) {
            try {
                file.writeText(json.encodeToString(MinecraftColors.serializer(), colors))
            } catch (e: Exception) {
                println("Error exporting default theme $name: ${e.message}")
            }
        }
    }
}
