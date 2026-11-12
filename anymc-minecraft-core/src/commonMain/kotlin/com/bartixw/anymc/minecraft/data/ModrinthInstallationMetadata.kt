/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

import kotlinx.serialization.Serializable

@Serializable
data class ModrinthAssetMetadata(
    val filename: String,
    val versionId: String,
    val projectId: String,
    val projectType: String, // "mod", "resourcepack", "shader"
    val relativePath: String // path relative to installation root, e.g. "gameFiles/mods/test.jar"
)

@Serializable
data class ModrinthInstallationMetadata(
    val assets: List<ModrinthAssetMetadata> = emptyList()
)
