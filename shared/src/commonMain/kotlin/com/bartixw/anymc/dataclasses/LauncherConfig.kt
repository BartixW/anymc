/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.dataclasses

import kotlinx.serialization.Serializable

@Serializable
data class LauncherConfig(
    var version: String,
    var isNew: Boolean = true,
    val eulaAccepted: Boolean = false,
    val theme: String = "Default",
    val maxRamUsage: String = "2048",
    val gpuOffload: String = "none",
    val devMode: Boolean = false,
    val showSnapshotsInList: Boolean = false,
    val minimizeOnLaunch: Boolean = false,
    val showConsoleOnLaunch: Boolean = false,
    val controllerSupport: Boolean = false,
    val windowWidth: Int = 854,
    val windowHeight: Int = 480,
    val language: String = "auto",
    val refreshedUI: Boolean = true,
    val nonPixelUI: Boolean = false,
    val syncPreferences: Boolean = false,
    val syncServers: Boolean = false,
    val syncCommandHistory: Boolean = false,
    val syncHotbar: Boolean = false,
    val syncSaves: Boolean = false,
    val syncConfig: Boolean = false,
    val syncScreenshots: Boolean = false,
    val syncResourcepacks: Boolean = false,
    val syncShaderpacks: Boolean = false,
    val syncFlashback: Boolean = false,
    val syncDistantHorizons: Boolean = false,
    val syncVoxy: Boolean = false,
    val syncXareo: Boolean = false,
    val syncJourneymap: Boolean = false,
    val syncBobby: Boolean = false,
    val syncSchematic: Boolean = false,
    val updateApiHost: String = "api.github.com",
    val lastUpdateCheckTimestamp: Long = 0L
)
