/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data.modrinth

import kotlinx.serialization.Serializable

@Serializable
data class ModpackIndex(
    val formatVersion: Int,
    val game: String,
    val versionId: String,
    val name: String,
    val summary: String? = null,
    val files: List<ModpackFile>,
    val dependencies: Map<String, String>
)

@Serializable
data class ModpackFile(
    val path: String,
    val hashes: Map<String, String>,
    val env: ModpackEnv? = null,
    val downloads: List<String>,
    val fileSize: Long
)

@Serializable
data class ModpackEnv(
    val client: String, // "required", "optional", "unsupported"
    val server: String
)
