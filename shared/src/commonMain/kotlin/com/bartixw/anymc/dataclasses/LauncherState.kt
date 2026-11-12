/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.dataclasses

import kotlinx.serialization.Serializable

@Serializable
data class LauncherState(
    val version: String,
    val currentUsername: String? = null,
    val currentUuid: String? = null,
    val lastSelectedInstanceId: String? = null,
    val lastSelectedInstanceString: String? = null,
    val authlibInjectorDownloadTimestamp: String? = null,
    val authlibInjectorSha1: String? = null,
    val lastSkinPath: String? = null
)