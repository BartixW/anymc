/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class InstallationType {
    @SerialName("vanilla") VANILLA,
    @SerialName("fabric") FABRICMC,
    @SerialName("legacyfabric") LEGACYFABRIC,
    @SerialName("forge") FORGE,
    @SerialName("neoforge") NEOFORGE,
    @SerialName("modrinth_modpack") MODRINTH_MODPACK,
    @SerialName("import") IMPORT
}