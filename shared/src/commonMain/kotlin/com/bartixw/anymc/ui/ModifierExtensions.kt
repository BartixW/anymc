/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a border to the top and bottom of the component with independent colors and widths.
 */
fun Modifier.topAndBottomBorder(
    topWidth: Dp = 0.dp,
    topColor: Color = Color.Transparent,
    bottomWidth: Dp = 0.dp,
    bottomColor: Color = Color.Transparent
): Modifier = this.drawWithContent {
    drawContent()

    val topPx = topWidth.toPx()
    val bottomPx = bottomWidth.toPx()

    if (topPx > 0) {
        drawRect(
            color = topColor,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, topPx)
        )
    }

    if (bottomPx > 0) {
        drawRect(
            color = bottomColor,
            topLeft = Offset(0f, size.height - bottomPx),
            size = Size(size.width, bottomPx)
        )
    }
}