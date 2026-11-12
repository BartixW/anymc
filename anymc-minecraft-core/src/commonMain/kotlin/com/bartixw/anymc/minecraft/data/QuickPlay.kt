/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class QuickPlay(
    val type: QuickPlayType,
    val id: String,
    val name: String,
    @Serializable(with = JavaLocalDateTimeSerializer::class)
    val lastPlayedTime: LocalDateTime,
    @SerialName("gamemode")
    val gameMode: GameMode
)

@Serializable
enum class QuickPlayType {
    @SerialName("singleplayer")
    Singleplayer,
    @SerialName("multiplayer")
    Multiplayer,
    @SerialName("realms")
    Realms
}

@Serializable
enum class GameMode {
    @SerialName("survival")
    Survival,
    @SerialName("creative")
    Creative,
    @SerialName("adventure")
    Adventure,
    @SerialName("spectator")
    Spectator
}