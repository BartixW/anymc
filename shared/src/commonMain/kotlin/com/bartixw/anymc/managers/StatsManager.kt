/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.minecraft.data.Installation
import com.bartixw.anymc.helpers.parseMinecraftWorld
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File
import java.security.MessageDigest

@Serializable
data class CachedFileStats(
    val hash: String,
    val playTime: Long,
    val deaths: Int,
    val leaveGame: Int,
    val totalDist: Long,
    val crouchDist: Long,
    val sprintDist: Long,
    val minedBlocks: Map<String, Int>,
    val worldName: String,
    val playerUuid: String,
    val parserVersion: Int = 0
)

@Serializable
data class StatsData(
    val fileCache: Map<String, CachedFileStats> = emptyMap(),
    val trackedPlayTime: Map<String, Long> = emptyMap(),
    val version: Int = 0
)

@Serializable
data class DetailedWorldStats(
    val worldId: String,
    val worldName: String,
    val playTimeSeconds: Long,
    val deaths: Int,
    val sessions: Int,
    val categories: Map<String, Map<String, Int>> // Category (e.g., Mined) -> ID -> Count
)

data class AggregatedProfileStats(
    val totalPlayTimeSeconds: Long = 0,
    val longestPlayedWorldName: String = "Brak",
    val mostCollectedBlockName: String = "Brak",
    val totalDeaths: Int = 0,
    val totalDistanceBlocks: Long = 0,
    val totalSessions: Int = 0,
    val crouchDistanceBlocks: Long = 0,
    val sprintDistanceBlocks: Long = 0
)

object StatsManager : BasePersistentDataManager<StatsData>(
    fileName = "stats_cache.json",
    defaultValue = StatsData(),
    serializer = StatsData.serializer()
) {
    private const val CURRENT_PARSER_VERSION = 6

    init {
        loadBlocking()
        if (data.value.version < CURRENT_PARSER_VERSION) {
            update { it.copy(fileCache = emptyMap(), version = CURRENT_PARSER_VERSION) }
        }

        // Re-calculate stats and reload translations when language changes
        scope.launch {
            ConfigManager.data
                .map { it.language }
                .distinctUntilChanged()
                .collect {
                    refreshStats()
                }
        }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private var refreshJob: Job? = null

    private val _aggregatedStats = MutableStateFlow(AggregatedProfileStats())
    val aggregatedStats = _aggregatedStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun addTrackedPlayTime(uuid: String, seconds: Long) {
        update { current ->
            val newTracked = current.trackedPlayTime.toMutableMap()
            newTracked[uuid] = (newTracked[uuid] ?: 0L) + seconds
            current.copy(trackedPlayTime = newTracked)
        }
        // Refresh UI
        refreshStats()
    }

    fun refreshStats() {
        val activeAccount = AccountManager.activeAccount ?: run {
            _aggregatedStats.value = AggregatedProfileStats()
            return
        }
        val uuid = formatUuid(activeAccount.uuid)
        
        refreshJob?.cancel()
        refreshJob = scope.launch {
            _isLoading.value = true
            try {
                val installations = MinecraftManager.getVersions()
                val newCache = data.value.fileCache.toMutableMap()
                var changed = false

            for (inst in installations) {
                val instDir = MinecraftManager.getDirectory(inst.id)
                
                // Try to load translations if cache is small
                if (TranslationManager.data.value.translations.size < 1000) {
                    TranslationManager.loadTranslationsFromInstallation(instDir)
                }

                val savesDir = instDir.resolve("gameFiles").resolve("saves")
                    if (!savesDir.exists()) continue

                    val worlds = savesDir.listFiles { f -> f.isDirectory } ?: continue
                    for (worldFolder in worlds) {
                        val statsFile = worldFolder.resolve("stats").resolve("$uuid.json")
                        if (!statsFile.exists()) continue

                        val currentHash = statsFile.sha1()
                        val cached = newCache[statsFile.absolutePath]

                        if (cached == null || cached.hash != currentHash || cached.parserVersion < CURRENT_PARSER_VERSION) {
                            val worldInfo = parseMinecraftWorld(worldFolder)
                            val worldName = worldInfo?.name ?: worldFolder.name
                            
                            val parsed = parseStatsFile(statsFile, worldName, currentHash, uuid)
                            if (parsed != null) {
                                newCache[statsFile.absolutePath] = parsed
                                changed = true
                            }
                        }
                    }
                }

                if (changed) {
                    update { it.copy(fileCache = newCache) }
                }

                val trackedTime = data.value.trackedPlayTime[uuid]
                if (trackedTime == null) {
                    // Seeding: First time tracking, use sum of world stats
                    val seedTime = newCache.values
                        .filter { it.playerUuid == uuid }
                        .sumOf { it.playTime } / 20
                    
                    addTrackedPlayTime(uuid, seedTime)
                    return@launch // addTrackedPlayTime will trigger refreshStats again
                }
                
                aggregate(newCache.values, uuid)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun aggregate(stats: Collection<CachedFileStats>, playerUuid: String) {
        var totalWorldTicks: Long = 0
        var totalDeaths = 0
        var totalLeaveGame = 0
        var totalDistCm: Long = 0
        var crouchDistCm: Long = 0
        var sprintDistCm: Long = 0
        val minedBlocks = mutableMapOf<String, Int>()
        
        var longestWorldName = "Brak"
        var longestWorldTicks: Long = 0

        for (s in stats) {
            if (s.playerUuid != playerUuid) continue
            
            totalWorldTicks += s.playTime
            totalDeaths += s.deaths
            totalLeaveGame += s.leaveGame
            totalDistCm += s.totalDist
            crouchDistCm += s.crouchDist
            sprintDistCm += s.sprintDist
            
            s.minedBlocks.forEach { (id, count) ->
                minedBlocks[id] = (minedBlocks[id] ?: 0) + count
            }

            if (s.playTime > longestWorldTicks) {
                longestWorldTicks = s.playTime
                longestWorldName = s.worldName
            }
        }

        val mostMinedEntry = minedBlocks.maxByOrNull { it.value }
        val mostMinedBlockName = mostMinedEntry?.let { 
            TranslationManager.getTranslatedBlockName(it.key)
        } ?: "Brak"

        val finalPlayTimeSeconds = data.value.trackedPlayTime[playerUuid] ?: (totalWorldTicks / 20)

        _aggregatedStats.value = AggregatedProfileStats(
            totalPlayTimeSeconds = finalPlayTimeSeconds,
            longestPlayedWorldName = longestWorldName,
            mostCollectedBlockName = mostMinedBlockName,
            totalDeaths = totalDeaths,
            totalDistanceBlocks = totalDistCm / 100,
            totalSessions = totalLeaveGame,
            crouchDistanceBlocks = crouchDistCm / 100,
            sprintDistanceBlocks = sprintDistCm / 100
        )
    }

    private fun parseStatsFile(file: File, worldName: String, hash: String, playerUuid: String): CachedFileStats? {
        return try {
            val content = file.readText()
            val root = json.parseToJsonElement(content).jsonObject
            
            if (root.containsKey("stats")) {
                // Modern format (1.13+)
                val stats = root["stats"]?.jsonObject ?: return null
                
                val custom = stats["minecraft:custom"]?.jsonObject
                val mined = stats["minecraft:mined"]?.jsonObject
                
                val playTime = custom?.get("minecraft:play_time")?.jsonPrimitive?.long
                    ?: custom?.get("minecraft:play_one_minute")?.jsonPrimitive?.long
                    ?: 0L
                val deaths = custom?.get("minecraft:deaths")?.jsonPrimitive?.int ?: 0
                val leaveGame = custom?.get("minecraft:leave_game")?.jsonPrimitive?.int ?: 0
                
                var totalDist = 0L
                var crouchDist = 0L
                var sprintDist = 0L
                
                custom?.forEach { (key, value) ->
                    if (key.endsWith("_one_cm")) {
                        val dist = value.jsonPrimitive.long
                        totalDist += dist
                        if (key == "minecraft:crouch_one_cm") crouchDist = dist
                        if (key == "minecraft:sprint_one_cm") sprintDist = dist
                    }
                }
                
                val minedMap = mutableMapOf<String, Int>()
                mined?.forEach { (key, value) ->
                    minedMap[key] = value.jsonPrimitive.int
                }
                
                CachedFileStats(hash, playTime, deaths, leaveGame, totalDist, crouchDist, sprintDist, minedMap, worldName, playerUuid, CURRENT_PARSER_VERSION)
            } else {
                // Legacy format (1.7 - 1.12)
                var playTime = 0L
                var deaths = 0
                var leaveGame = 0
                var totalDist = 0L
                var crouchDist = 0L
                var sprintDist = 0L
                val minedMap = mutableMapOf<String, Int>()
                
                root.forEach { (key, value) ->
                    val v = value.jsonPrimitive
                    when {
                        key == "stat.playOneMinute" -> playTime = v.long
                        key == "stat.deaths" -> deaths = v.int
                        key == "stat.leaveGame" -> leaveGame = v.int
                        key.startsWith("stat.mineBlock.") -> {
                            val id = key.substringAfter("stat.mineBlock.").replace(".", ":")
                            minedMap[id] = v.int
                        }
                        key.endsWith("OneCm") -> {
                            val dist = v.long
                            totalDist += dist
                            if (key == "stat.crouchOneCm") crouchDist = dist
                            if (key == "stat.sprintOneCm") sprintDist = dist
                        }
                    }
                }
                CachedFileStats(hash, playTime, deaths, leaveGame, totalDist, crouchDist, sprintDist, minedMap, worldName, playerUuid, CURRENT_PARSER_VERSION)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun formatUuid(uuid: String): String {
        if (uuid.contains("-")) return uuid
        if (uuid.length != 32) return uuid
        return StringBuilder(uuid)
            .insert(8, "-")
            .insert(13, "-")
            .insert(18, "-")
            .insert(23, "-")
            .toString()
    }

    fun getDetailedStatsForInstallation(installationId: String, playerUuid: String): List<DetailedWorldStats> {
        val uuid = formatUuid(playerUuid)
        val instDir = MinecraftManager.getDirectory(installationId)
        val savesDir = instDir.resolve("gameFiles").resolve("saves")
        if (!savesDir.exists()) return emptyList()

        val result = mutableListOf<DetailedWorldStats>()
        savesDir.listFiles { f -> f.isDirectory }?.forEach { worldFolder ->
            val statsFile = worldFolder.resolve("stats").resolve("$uuid.json")
            if (statsFile.exists()) {
                val worldInfo = parseMinecraftWorld(worldFolder)
                val worldName = worldInfo?.name ?: worldFolder.name
                parseDetailedStats(statsFile, worldFolder.name, worldName)?.let { result.add(it) }
            }
        }
        return result
    }

    private fun parseDetailedStats(file: File, worldId: String, worldName: String): DetailedWorldStats? {
        return try {
            val content = file.readText()
            val root = json.parseToJsonElement(content).jsonObject
            
            if (root.containsKey("stats")) {
                // Modern format (1.13+)
                val stats = root["stats"]?.jsonObject ?: return null
                val custom = stats["minecraft:custom"]?.jsonObject
                
                val playTime = custom?.get("minecraft:play_time")?.jsonPrimitive?.long
                    ?: custom?.get("minecraft:play_one_minute")?.jsonPrimitive?.long
                    ?: 0L
                val deaths = custom?.get("minecraft:deaths")?.jsonPrimitive?.int ?: 0
                val leaveGame = custom?.get("minecraft:leave_game")?.jsonPrimitive?.int ?: 0
                
                val categories = mutableMapOf<String, Map<String, Int>>()
                stats.forEach { (catKey, catValue) ->
                    if (catKey != "minecraft:custom") {
                        val items = mutableMapOf<String, Int>()
                        catValue.jsonObject.forEach { (id, count) ->
                            items[id] = count.jsonPrimitive.int
                        }
                        categories[catKey] = items
                    }
                }
                
                // Add all custom stats
                val customItems = mutableMapOf<String, Int>()
                custom?.forEach { (id, count) ->
                    customItems[id] = count.jsonPrimitive.int
                }
                if (customItems.isNotEmpty()) categories["minecraft:custom"] = customItems

                DetailedWorldStats(worldId, worldName, playTime / 20, deaths, leaveGame, categories)
            } else {
                // Legacy format
                var playTime = 0L
                var deaths = 0
                var leaveGame = 0
                val categories = mutableMapOf<String, MutableMap<String, Int>>()

                root.forEach { (key, value) ->
                    val v = value.jsonPrimitive.int
                    when {
                        key == "stat.playOneMinute" -> playTime = value.jsonPrimitive.long
                        key == "stat.deaths" -> deaths = v
                        key == "stat.leaveGame" -> leaveGame = v
                        key.startsWith("stat.mineBlock.") -> {
                            val id = "minecraft:" + key.substringAfter("stat.mineBlock.").replace(".", ":")
                            categories.getOrPut("minecraft:mined") { mutableMapOf() }[id] = v
                        }
                        key.startsWith("stat.useItem.") -> {
                            val id = "minecraft:" + key.substringAfter("stat.useItem.").replace(".", ":")
                            categories.getOrPut("minecraft:used") { mutableMapOf() }[id] = v
                        }
                        key.startsWith("stat.breakItem.") -> {
                            val id = "minecraft:" + key.substringAfter("stat.breakItem.").replace(".", ":")
                            categories.getOrPut("minecraft:broken") { mutableMapOf() }[id] = v
                        }
                        key.startsWith("stat.craftItem.") -> {
                            val id = "minecraft:" + key.substringAfter("stat.craftItem.").replace(".", ":")
                            categories.getOrPut("minecraft:crafted") { mutableMapOf() }[id] = v
                        }
                        key.startsWith("stat.killEntity.") -> {
                            val id = "minecraft:" + key.substringAfter("stat.killEntity.")
                            categories.getOrPut("minecraft:killed") { mutableMapOf() }[id] = v
                        }
                        key.startsWith("stat.entityKilledBy.") -> {
                            val id = "minecraft:" + key.substringAfter("stat.entityKilledBy.")
                            categories.getOrPut("minecraft:killed_by") { mutableMapOf() }[id] = v
                        }
                        key.startsWith("stat.") && !key.endsWith("OneCm") -> {
                            categories.getOrPut("minecraft:custom") { mutableMapOf() }[key] = v
                        }
                    }
                }
                DetailedWorldStats(worldName, worldId, playTime / 20, deaths, leaveGame, categories)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun File.sha1(): String {
        val digest = MessageDigest.getInstance("SHA-1")
        this.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
