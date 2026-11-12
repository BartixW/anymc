/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import com.bartixw.anymc.ui.theme.LocalMinecraftColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

@Composable
fun McIcon(
    resource: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalMinecraftColors.current.textPrimary,
    shadowColor: Color = Color(0xFF3F3F3F),
) {
    Box(modifier = modifier) {
        val res = imageResource(resource)

        Image(
            bitmap = res,
            contentDescription = null,
            colorFilter = ColorFilter.tint(shadowColor),
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .offset(x = 2.dp, y = 2.dp)
                .fillMaxSize()
        )

        Image(
            bitmap = res,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize()
        )
    }
}