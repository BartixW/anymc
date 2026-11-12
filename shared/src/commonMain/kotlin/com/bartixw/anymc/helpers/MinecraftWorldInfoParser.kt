/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.helpers

import com.viaversion.nbt.io.NBTIO
import com.viaversion.nbt.tag.CompoundTag
import java.io.File

data class MinecraftWorldInfo(
    val name: String,
    val lastPlayed: Long,
    val gameType: Int
)

fun parseMinecraftWorld(worldFolder: File): MinecraftWorldInfo? {
    val levelDatFile = File(worldFolder, "level.dat")
    if (!levelDatFile.exists()) return null

    return try {
        val rootTag =
            NBTIO.reader().named().read(levelDatFile.toPath(), true) as? CompoundTag
            ?: return null

        val dataTag = rootTag.getCompoundTag("Data") ?: return null

        val worldName = dataTag.getString("LevelName") ?: worldFolder.name
        val lastPlayed = dataTag.getLong("LastPlayed")
        val gameType = dataTag.getInt("GameType")

        return MinecraftWorldInfo(
            name = worldName,
            lastPlayed = lastPlayed,
            gameType = gameType
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}