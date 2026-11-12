/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import com.bartixw.anymc.ui.theme.LocalMinecraftColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

val LocalIconModifier = compositionLocalOf<Modifier> { Modifier }

@Composable
fun Icon(
    resource: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalMinecraftColors.current.textPrimary,
    shadowColor: Color = Color(0xFF3F3F3F),
) {
    val res = imageResource(resource)

    Image(
        bitmap = res,
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        filterQuality = FilterQuality.None,
        modifier = LocalIconModifier.current.then(modifier)
    )
}
