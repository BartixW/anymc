/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

import kotlinx.serialization.Serializable

@Serializable
data class AnyMCInstallationData(
    val version: String,
    val name: String,
    val minecraftVersion: String,
    val loaderType: String? = null,
    val loaderVersion: String? = null,
    val jvmArgs: List<String> = emptyList(),
    val removedArgs: List<String> = emptyList(),
    val javaVersion: JavaVersion? = null
)

data class Installation(
    val name: String,
    val id: String,
    val version: String,
    val modLoader: String
)
