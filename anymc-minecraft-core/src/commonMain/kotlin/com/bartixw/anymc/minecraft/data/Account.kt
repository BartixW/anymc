/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed class Account {
    abstract val username: String
    abstract val uuid: String
    abstract val type: AccountType

    @Serializable
    @SerialName("Microsoft")
    data class Microsoft(
        override val username: String,
        override val uuid: String,
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long,
        val isDemo: Boolean,
        val xuid: String? = null
    ) : Account() {
        override val type = AccountType.MICROSOFT
    }

    @Serializable
    @SerialName("Offline")
    data class Offline(
        override val username: String,
        override val uuid: String
    ) : Account() {
        override val type = AccountType.OFFLINE
    }

    @Serializable
    @SerialName("ElyBy")
    data class ElyBy(
        override val username: String,
        override val uuid: String,
        val accessToken: String,
        val clientToken: String? = null
    ) : Account() {
        override val type = AccountType.ELY_BY
    }
}

data class ResolvedAccount(
    val name: String,
    val uuid: String,
    val token: String,
    val type: String,
    val isDemo: Boolean,
    val xuid: String? = null
)
