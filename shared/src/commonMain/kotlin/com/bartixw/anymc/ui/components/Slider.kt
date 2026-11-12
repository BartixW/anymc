/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bartixw.anymc.ui.theme.MinecraftTheme

@Composable
fun McSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MinecraftTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val thumbWidthDp = 16.dp
        val thumbWidthPx = with(density) { thumbWidthDp.toPx() }

        val maxOffset = widthPx - thumbWidthPx
        val currentOffset = value * maxOffset

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.backgroundDirt)
                .border(1.dp, colors.surfaceShadow)
        )

        Box(
            modifier = Modifier
                .offset(x = with(density) { currentOffset.toDp() })
                .size(width = thumbWidthDp, height = 24.dp)
                .background(colors.surfaceFrame)
                .border(2.dp, colors.surfaceHighlight)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = (currentOffset + delta).coerceIn(0f, maxOffset)
                        onValueChange(if (maxOffset > 0) newOffset / maxOffset else 0f)
                    }
                )
        )
    }
}