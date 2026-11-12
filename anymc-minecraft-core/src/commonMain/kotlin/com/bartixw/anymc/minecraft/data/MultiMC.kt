/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MultiMCIndex(
    val versions: List<MultiMCIndexVersion>
)

@Serializable
data class MultiMCIndexVersion(
    val version: String,
    val requires: List<MultiMCRequirement> = emptyList(),
    val releaseTime: String,
    val type: String? = null
)

@Serializable
data class MultiMCRequirement(
    val equals: String? = null,
    val uid: String
)

@Serializable
data class MultiMCProfile(
    val id: String? = null,
    val name: String? = null,
    val mainClass: String? = null,
    val libraries: List<Library> = emptyList(),
    val mavenFiles: List<Library> = emptyList(),
    val minecraftArguments: String? = null,
    val systemProperties: Map<String, String>? = null,
    @SerialName("+tweakers") val extraTweakers: List<String>? = null,
    @SerialName("+jvmArgs") val extraJvmArgs: List<String>? = null,
    @SerialName("+minecraftArguments") val extraMinecraftArguments: String? = null
)
