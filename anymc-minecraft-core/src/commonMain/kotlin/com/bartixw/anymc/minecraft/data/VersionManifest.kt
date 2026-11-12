/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

import kotlinx.serialization.Serializable


@Serializable
data class LatestVersion(
    val release: String,
    val snapshot: String
)

@Serializable
data class Version(
    val id: String,
    val type: String,
    val url: String,
    val time: String,
    val releaseTime: String,
    val sha1: String,
    val complianceLevel: Int,
)

@Serializable
data class VersionManifest(
    val latest: LatestVersion,
    val versions: List<Version>,
)