/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.controllers

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Deprecated("ThemeController is deprecated in favor of SettingsController")
data class ThemeController(
    val updateSeedColor: (Color) -> Unit,
    val updateDarkTheme: (Boolean) -> Unit,
    val darkTheme: Boolean,
    val seedColor: Color
)

@Deprecated("ThemeController is deprecated in favor of SettingsController")
val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("No ThemeController provided")
}