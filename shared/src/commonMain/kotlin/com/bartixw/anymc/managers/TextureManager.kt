/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.core.utils.PlatformUtils
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

object TextureManager {
    private val client = MinecraftManager.client
    private val json = MinecraftManager.json
    private val downloadMutex = Mutex()

    val texturesDir: File by lazy {
        PlatformUtils.sharedStorageDirectory.resolve("mc_textures").also { it.mkdirs() }
    }

    private val mojangTexturesMapping = mapOf(
        "andesite.png" to "assets/minecraft/textures/block/andesite.png",
        "birch_leaves.png" to "assets/minecraft/textures/block/birch_leaves.png",
        "birch_log.png" to "assets/minecraft/textures/block/birch_log.png",
        "birch_log_top.png" to "assets/minecraft/textures/block/birch_log_top.png",
        "bush.png" to "assets/minecraft/textures/block/azalea_leaves.png",
        "dandelion.png" to "assets/minecraft/textures/block/dandelion.png",
        "dirt.png" to "assets/minecraft/textures/block/dirt.png",
        "granite.png" to "assets/minecraft/textures/block/granite.png",
        "grass_block_side.png" to "assets/minecraft/textures/block/grass_block_side.png",
        "grass_block_top.png" to "assets/minecraft/textures/block/grass_block_top.png",
        "gravel.png" to "assets/minecraft/textures/block/gravel.png",
        "rose_bush_bottom.png" to "assets/minecraft/textures/block/rose_bush_bottom.png",
        "rose_bush_top.png" to "assets/minecraft/textures/block/rose_bush_top.png",
        "short_grass.png" to "assets/minecraft/textures/block/short_grass.png",
        "stone.png" to "assets/minecraft/textures/block/stone.png",
        "wildflowers.png" to "assets/minecraft/textures/block/wildflowers.png",
        "wildflowers_stem.png" to "assets/minecraft/textures/block/wildflowers_stem.png",
    )

    fun hasAllTextures(): Boolean {
        return mojangTexturesMapping.keys.all { fileName ->
            val file = texturesDir.resolve(fileName)
            file.exists() && file.length() > 0
        }
    }

    suspend fun ensureTexturesDownloaded(): Boolean = downloadMutex.withLock {
        withContext(Dispatchers.IO) {
            if (hasAllTextures()) {
                return@withContext true
            }

            if (!ConfigManager.data.value.eulaAccepted) {
                return@withContext false
            }

            try {
                // 1. Fetch version manifest from Mojang official server
                val manifestResponse = client.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
                if (manifestResponse.status.value != 200) return@withContext false

                val manifestJson = json.parseToJsonElement(manifestResponse.bodyAsText()).jsonObject
                val versions = manifestJson["versions"]?.jsonArray ?: return@withContext false

                val targetVersionObj = versions.asSequence().map { it.jsonObject }.firstOrNull {
                    it["id"]?.jsonPrimitive?.content == "1.21.10"
                } ?: return@withContext false

                val versionUrl = targetVersionObj["url"]?.jsonPrimitive?.content ?: return@withContext false

                val versionResponse = client.get(versionUrl)
                if (versionResponse.status.value != 200) return@withContext false

                val versionJson = json.parseToJsonElement(versionResponse.bodyAsText()).jsonObject
                val clientDownloadObj = versionJson["downloads"]?.jsonObject?.get("client")?.jsonObject ?: return@withContext false
                val clientJarUrl = clientDownloadObj["url"]?.jsonPrimitive?.content ?: return@withContext false

                val jarBytes: ByteArray = client.get(clientJarUrl).body()
                if (jarBytes.isEmpty()) return@withContext false

                val extractedData = mutableMapOf<String, ByteArray>()
                var sideOverlayBytes: ByteArray? = null

                ZipInputStream(ByteArrayInputStream(jarBytes)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        if (entryName == "assets/minecraft/textures/block/grass_block_side_overlay.png") {
                            sideOverlayBytes = zip.readBytes()
                        }
                        for ((targetFileName, mojangPath) in mojangTexturesMapping) {
                            if (extractedData.containsKey(targetFileName)) continue
                            if (entryName == mojangPath || 
                                (targetFileName == "short_grass.png" && entryName == "assets/minecraft/textures/block/grass.png") ||
                                (targetFileName == "bush.png" && entryName == "assets/minecraft/textures/block/dead_bush.png")) {
                                extractedData[targetFileName] = zip.readBytes()
                            }
                        }
                        entry = zip.nextEntry
                    }
                }

                // Apply 2D Multiply tinting to foliage and grass textures (Birch Forest)
                val grassR = 136f / 255.0f
                val grassG = 187f / 255.0f
                val grassB = 103f / 255.0f

                val leavesR = 107f / 255.0f
                val leavesG = 169f / 255.0f
                val leavesB = 65f / 255.0f

                extractedData["grass_block_top.png"]?.let {
                    extractedData["grass_block_top.png"] = applyMultiplyTint(it, grassR, grassG, grassB, scale = 0.85f)
                }
                extractedData["short_grass.png"]?.let {
                    extractedData["short_grass.png"] = applyMultiplyTint(it, grassR, grassG, grassB, scale = 1.65f)
                }
                extractedData["birch_leaves.png"]?.let {
                    extractedData["birch_leaves.png"] = applyMultiplyTint(it, leavesR, leavesG, leavesB, scale = 1.3f)
                }

                // Composite grass_block_side_overlay onto grass_block_side if available
                val grassBlockSideBytes = extractedData["grass_block_side.png"]
                if (grassBlockSideBytes != null && sideOverlayBytes != null) {
                    val compositedSide = createCompositedGrassBlockSide(grassBlockSideBytes, sideOverlayBytes, grassR, grassG, grassB, scale = 0.85f)
                    extractedData["grass_block_side.png"] = compositedSide
                }

                texturesDir.mkdirs()
                for ((fileName, bytes) in extractedData) {
                    val destFile = texturesDir.resolve(fileName)
                    destFile.writeBytes(bytes)
                }

                println("TextureManager: Successfully downloaded and 2D Multiply tinted all official Minecraft textures.")
                return@withContext hasAllTextures()
            } catch (e: Exception) {
                println("TextureManager: Failed to download official Minecraft textures: ${e.message}")
                e.printStackTrace()
                return@withContext hasAllTextures()
            }
        }
    }

    private fun adjustContrast(value: Int, factor: Float = 1.25f): Int {
        val centered = value - 128
        return (centered * factor + 128).toInt().coerceIn(0, 255)
    }

    private fun applyMultiplyTint(imageBytes: ByteArray, rFactor: Float, gFactor: Float, bFactor: Float, scale: Float = 1.0f): ByteArray {
        try {
            val img = javax.imageio.ImageIO.read(ByteArrayInputStream(imageBytes)) ?: return imageBytes
            val width = img.width
            val height = img.height
            val result = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val argb = img.getRGB(x, y)
                    val alpha = (argb shr 24) and 0xFF
                    val r = (argb shr 16) and 0xFF
                    val g = (argb shr 8) and 0xFF
                    val b = argb and 0xFF

                    // Tint and enhance contrast to avoid plastic/single-colored look
                    val nr = adjustContrast((r * rFactor * scale).toInt(), 1.25f)
                    val ng = adjustContrast((g * gFactor * scale).toInt(), 1.25f)
                    val nb = adjustContrast((b * bFactor * scale).toInt(), 1.25f)

                    val newArgb = (alpha shl 24) or (nr shl 16) or (ng shl 8) or nb
                    result.setRGB(x, y, newArgb)
                }
            }

            val baos = java.io.ByteArrayOutputStream()
            javax.imageio.ImageIO.write(result, "png", baos)
            return baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            return imageBytes
        }
    }

    private fun createCompositedGrassBlockSide(dirtBytes: ByteArray, overlayBytes: ByteArray, rFactor: Float, gFactor: Float, bFactor: Float, scale: Float = 1.4f): ByteArray {
        try {
            val dirtImg = javax.imageio.ImageIO.read(ByteArrayInputStream(dirtBytes)) ?: return dirtBytes
            val overlayImg = javax.imageio.ImageIO.read(ByteArrayInputStream(overlayBytes)) ?: return dirtBytes

            val width = dirtImg.width
            val height = dirtImg.height

            val resultImg = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val dirtRgb = dirtImg.getRGB(x, y)
                    val dirtA = (dirtRgb shr 24) and 0xFF
                    val dirtR = (dirtRgb shr 16) and 0xFF
                    val dirtG = (dirtRgb shr 8) and 0xFF
                    val dirtB = dirtRgb and 0xFF

                    val overlayRgb = overlayImg.getRGB(x, y)
                    val overlayA = (overlayRgb shr 24) and 0xFF
                    val overR = (overlayRgb shr 16) and 0xFF
                    val overG = (overlayRgb shr 8) and 0xFF
                    val overB = (overlayRgb and 0xFF)

                    // Tint overlay independently with contrast boost
                    val tintedR = adjustContrast((overR * rFactor * scale).toInt(), 1.25f)
                    val tintedG = adjustContrast((overG * gFactor * scale).toInt(), 1.25f)
                    val tintedB = adjustContrast((overB * bFactor * scale).toInt(), 1.25f)

                    // Alpha blend tinted overlay directly over dirt
                    val alphaFactor = overlayA / 255.0f
                    val finalR = (dirtR * (1 - alphaFactor) + tintedR * alphaFactor).toInt().coerceIn(0, 255)
                    val finalG = (dirtG * (1 - alphaFactor) + tintedG * alphaFactor).toInt().coerceIn(0, 255)
                    val finalB = (dirtB * (1 - alphaFactor) + tintedB * alphaFactor).toInt().coerceIn(0, 255)

                    val finalArgb = (dirtA.coerceAtLeast(overlayA) shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                    resultImg.setRGB(x, y, finalArgb)
                }
            }

            val baos = java.io.ByteArrayOutputStream()
            javax.imageio.ImageIO.write(resultImg, "png", baos)
            return baos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            return dirtBytes
        }
    }
}
