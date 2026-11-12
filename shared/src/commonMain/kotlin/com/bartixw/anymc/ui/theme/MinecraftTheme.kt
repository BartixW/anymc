/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bartixw.anymc.managers.ConfigManager

val LocalMinecraftColors = staticCompositionLocalOf { ClassicMinecraftColors }
val LocalMinecraftTextStyle = staticCompositionLocalOf { TextStyle() }
val LocalTextStyle = staticCompositionLocalOf { TextStyle() }
val LocalPixelTextStyle = staticCompositionLocalOf { TextStyle() }
val LocalAnyMCTextStyle = staticCompositionLocalOf { TextStyle() }

object MinecraftTheme {
    val colors: MinecraftColors
        @Composable
        get() = LocalMinecraftColors.current

    val typography: TextStyle
        @Composable get() = LocalMinecraftTextStyle.current

    val typographyV2: TextStyle
        @Composable get() = LocalTextStyle.current

    val typographyPixel: TextStyle
        @Composable get() = LocalPixelTextStyle.current

    val typographyAnyMC: TextStyle
        @Composable get() = LocalAnyMCTextStyle.current
}

@Composable
fun MinecraftTheme(
    colors: MinecraftColors = ClassicMinecraftColors,
    content: @Composable () -> Unit
) {
    val mcFont = minecraftFontFamily()
    val font = anymcFontFamily()
    val config by ConfigManager.data.collectAsState()

    val baseTextStyle = TextStyle(
        fontFamily = mcFont,
        fontSize = 16.sp,
        lineHeight = 18.sp,
        color = colors.textPrimary,
        shadow = Shadow(
            color = Color(0xFF3F3F3F),
            offset = Offset(2f, 2f)
        )
    )

    val baseTextStyleV2 = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 18.sp,
        color = colors.textPrimary
    )

    val pixelTextStyle = if (config.nonPixelUI) baseTextStyleV2 else TextStyle(
        fontFamily = mcFont,
        fontSize = 16.sp,
        lineHeight = 18.sp,
        color = colors.textPrimary,
    )

    val anymcTextStyle = TextStyle(
        fontFamily = font,
        fontSize = 24.sp,
        lineHeight = 18.sp,
        color = colors.textPrimary,
    )

    val baseScrollbarStyle = ScrollbarStyle(
        minimalHeight = Dp.Unspecified,
        thickness = 16.dp,
        shape = RectangleShape,
        hoverDurationMillis = 300,
        hoverColor = colors.controlBackground,
        unhoverColor = colors.controlDisabledBackground
    )


    LocalScrollbarStyle
    CompositionLocalProvider(
        LocalMinecraftColors provides colors,
        LocalMinecraftTextStyle provides baseTextStyle,
        LocalTextStyle provides baseTextStyleV2,
        LocalPixelTextStyle provides pixelTextStyle,
        LocalAnyMCTextStyle provides anymcTextStyle,
        LocalScrollbarStyle provides baseScrollbarStyle
    ) {
        content()
    }
}