/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.minecraft.data.Account
import com.bartixw.anymc.core.utils.PlatformUtils
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.util.Base64

@Serializable
data class MojangProfileResponse(
    val id: String,
    val name: String,
    val properties: List<MojangProfileProperty>
)

@Serializable
data class MojangProfileProperty(
    val name: String,
    val value: String
)

@Serializable
data class MojangTextures(
    val textures: MojangTextureData
)

@Serializable
data class MojangTextureData(
    val SKIN: MojangSkinData? = null
)

@Serializable
data class MojangSkinData(
    val url: String
)

@Serializable
data class ElyByTextures(
    val SKIN: MojangSkinData? = null
)

object SkinManager {
    private val client = MinecraftManager.client
    private val json = MinecraftManager.json
    internal val skinsDir by lazy { 
        PlatformUtils.sharedStorageDirectory.resolve("skins").also { it.mkdirs() }
    }

    suspend fun getSkinPath(account: Account): String? = withContext(Dispatchers.IO) {
        val cacheFile = skinsDir.resolve("${account.uuid}.png")
        
        // Return cached if exists, is not too old, and is a valid PNG
        if (cacheFile.exists() && cacheFile.length() > 0) {
            val isOld = System.currentTimeMillis() - cacheFile.lastModified() > 3600000
            val isValid = try {
                val header = cacheFile.inputStream().use {
                    val buf = ByteArray(4)
                    it.read(buf)
                    buf
                }
                header.contentEquals(byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte()))
            } catch (e: Exception) { false }

            if (!isOld && isValid) {
                return@withContext cacheFile.absolutePath
            }
        }

        val skinUrl = when (account) {
            is Account.Microsoft -> fetchMojangSkinUrl(account.uuid)
            is Account.ElyBy -> fetchElyBySkinUrl(account.username)
            is Account.Offline -> "https://minotar.net/skin/${account.username}.png" // Fallback
        } ?: return@withContext null

        return@withContext downloadSkin(skinUrl, cacheFile)
    }

    private suspend fun fetchMojangSkinUrl(uuid: String): String? {
        return try {
            val response = client.get("https://sessionserver.mojang.com/session/minecraft/profile/$uuid")
            if (response.status.value != 200) return null
            
            val profile = json.decodeFromString<MojangProfileResponse>(response.bodyAsText())
            val texturesProperty = profile.properties.find { it.name == "textures" } ?: return null
            
            val decodedBytes = Base64.getDecoder().decode(texturesProperty.value)
            val decodedString = decodedBytes.decodeToString()
            
            val texturesJson = json.decodeFromString<MojangTextures>(decodedString)
            texturesJson.textures.SKIN?.url
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchElyBySkinUrl(username: String): String? {
        return try {
            val response = client.get("http://skinsystem.ely.by/textures/$username")
            if (response.status.value != 200) return null
            
            val textures = json.decodeFromString<ElyByTextures>(response.bodyAsText())
            textures.SKIN?.url
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun downloadSkin(url: String, target: File): String? {
        return try {
            val response = client.get(url)
            if (response.status.value != 200) return null
            
            val bytes: ByteArray = response.body()
            
            // Basic validation: must start with PNG magic header
            if (bytes.size < 4 || !bytes.take(4).toByteArray().contentEquals(byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte()))) {
                println("SkinManager: Downloaded data from $url is not a valid PNG")
                return null
            }

            withContext(Dispatchers.IO) {
                target.writeBytes(bytes)
            }
            target.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
