/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.dataclasses.LauncherState

object StateManager : BasePersistentDataManager<LauncherState>(
    fileName = "state.json",
    defaultValue = LauncherState(version = "1"),
    serializer = LauncherState.serializer()
)