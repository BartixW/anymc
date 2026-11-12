/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File
import java.util.Locale

@Serializable
data class TranslationData(
    val languageCode: String = "",
    val translations: Map<String, String> = emptyMap()
)

object TranslationManager : BasePersistentDataManager<TranslationData>(
    fileName = "minecraft_translations.json",
    defaultValue = TranslationData(),
    serializer = TranslationData.serializer()
) {
    init {
        loadBlocking()
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun translate(key: String): String {
        val direct = data.value.translations[key]
        if (direct != null) return direct
        
        // Try variations for common prefixes
        if (key.startsWith("minecraft:")) {
            val id = key.substringAfter(":")
            return data.value.translations["stat.minecraft.$id"]
                ?: data.value.translations["entity.minecraft.$id"]
                ?: data.value.translations["block.minecraft.$id"]
                ?: data.value.translations["item.minecraft.$id"]
                ?: data.value.translations["stat.${camelCase(id)}"] // Legacy variations
                ?: key
        }
        
        return key
    }

    private fun camelCase(id: String): String {
        return id.split("_").mapIndexed { index, s ->
            if (index == 0) s else s.replaceFirstChar { it.uppercase() }
        }.joinToString("")
    }

    fun getTranslatedBlockName(blockId: String): String {
        val id = blockId.substringAfter(":")
        return translate("block.minecraft.$id")
            .takeIf { it != "block.minecraft.$id" }
            ?: translate("item.minecraft.$id")
            .takeIf { it != "item.minecraft.$id" }
            ?: blockId
    }

    /**
     * Attempts to find and cache translations from a specific installation.
     */
    fun loadTranslationsFromInstallation(installationDir: File) {
        val targetLangCode = getTargetLanguageCode()

        // Reset cache if language changed
        if (data.value.languageCode != targetLangCode) {
            update { it.copy(translations = emptyMap(), languageCode = targetLangCode) }
        } else if (data.value.translations.size > 2000) {
            // Already have plenty of translations for this language
            return
        }

        val assetsDir = installationDir.resolve("assets")
        if (assetsDir.exists()) {
            val indexesDir = assetsDir.resolve("indexes")
            if (indexesDir.exists()) {
                val indexFiles = indexesDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
                for (indexFile in indexFiles) {
                    try {
                        val indexContent = json.parseToJsonElement(indexFile.readText()).jsonObject
                        val objects = indexContent["objects"]?.jsonObject ?: continue

                        // Try various paths for the target language
                        val langAsset = objects["minecraft/lang/$targetLangCode.json"]?.jsonObject
                            ?: objects["minecraft/lang/$targetLangCode.lang"]?.jsonObject
                            ?: objects["lang/$targetLangCode.lang"]?.jsonObject

                        if (langAsset != null) {
                            val hash = langAsset["hash"]?.jsonPrimitive?.content ?: continue
                            val prefix = hash.substring(0, 2)
                            val langFile = assetsDir.resolve("objects").resolve(prefix).resolve(hash)

                            if (langFile.exists()) {
                                val translations = parseLangFile(langFile)
                                if (translations.isNotEmpty()) {
                                    updateCacheWithTranslations(translations, targetLangCode)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("TranslationManager: Error reading index ${indexFile.name}: ${e.message}")
                    }
                }
            }
        }

        // Fallback: Default English translations are often not in the assets index,
        // but are inside the Minecraft client JAR.
        if (targetLangCode == "en_us") {
            try {
                val jarFiles = installationDir.listFiles { f -> f.extension == "jar" && !f.name.contains("authlib") }
                for (jarFile in jarFiles ?: emptyArray()) {
                    java.util.zip.ZipFile(jarFile).use { zip ->
                        val entry = zip.getEntry("assets/minecraft/lang/en_us.json")
                            ?: zip.getEntry("assets/minecraft/lang/en_US.lang")
                        
                        if (entry != null) {
                            val content = zip.getInputStream(entry).bufferedReader().readText()
                            val translations = parseLangContent(content)
                            if (translations.isNotEmpty()) {
                                updateCacheWithTranslations(translations, targetLangCode)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore JAR errors
            }
        }
    }

    private fun parseLangContent(content: String): Map<String, String> {
        return try {
            if (content.trim().startsWith("{")) {
                val jsonObject = json.parseToJsonElement(content).jsonObject
                jsonObject.mapValues { it.value.jsonPrimitive.content }
            } else {
                content.lines()
                    .filter { it.contains("=") && !it.trim().startsWith("#") }
                    .associate {
                        val parts = it.split("=", limit = 2)
                        parts[0].trim() to parts[1].trim()
                    }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun getTargetLanguageCode(): String {
        return when (val lang = ConfigManager.data.value.language) {
            "pl" -> "pl_pl"
            "en" -> "en_us"
            else -> {
                val locale = Locale.getDefault()
                val language = locale.language.lowercase()
                val country = locale.country.lowercase()
                if (country.isNotEmpty()) "${language}_$country" else "${language}_$language"
            }
        }
    }

    private fun parseLangFile(file: File): Map<String, String> {
        return parseLangContent(file.readText())
    }

    private fun updateCacheWithTranslations(translations: Map<String, String>, langCode: String) {
        if (translations.isEmpty()) return
        
        update { current ->
            // If language changed in between, we start fresh.
            // Otherwise, we merge into existing translations.
            val newTranslations = if (current.languageCode == langCode) {
                current.translations.toMutableMap()
            } else {
                mutableMapOf()
            }

            var added = 0
            translations.forEach { (key, value) ->
                if (!newTranslations.containsKey(key)) {
                    newTranslations[key] = value
                    added++
                }
            }
            
            if (added > 0) {
                println("TranslationManager: Added $added new translations for $langCode.")
            }
            
            current.copy(translations = newTranslations, languageCode = langCode)
        }
    }
}
