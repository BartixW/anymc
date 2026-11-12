/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

import kotlinx.serialization.Serializable

@Serializable
data class FabricGameVersion(
    val version: String,
    val stable: Boolean
)

@Serializable
data class FabricLoaderVersion(
    val version: String,
    val stable: Boolean,
    val maven: String,
    val build: Int
)

@Serializable
data class FabricLoaderResponse(
    val loader: LoaderInfo
) {
    @Serializable
    data class LoaderInfo(
        val version: String,
        val stable: Boolean,
        val maven: String,
        val build: Int
    )
}

@Serializable
data class FabricProfile(
    val id: String,
    val mainClass: String,
    val libraries: List<Library>
)