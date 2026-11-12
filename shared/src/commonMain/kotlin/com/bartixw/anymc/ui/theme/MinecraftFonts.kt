/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import anymc.shared.generated.resources.AnyMCFont
import anymc.shared.generated.resources.F77_Minecraft
import anymc.shared.generated.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
fun minecraftFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.F77_Minecraft)
    )
}

@Composable
fun anymcFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.AnyMCFont),
        Font(Res.font.F77_Minecraft)
    )
}