/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.dataclasses.LauncherConfig

object ConfigManager : BasePersistentDataManager<LauncherConfig>(
    fileName = "config.json",
    defaultValue = LauncherConfig(version = "1"),
    serializer = LauncherConfig.serializer()
)