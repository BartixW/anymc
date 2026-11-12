/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bartixw.anymc.ui.theme.MinecraftTheme

@Composable
fun McLoadingIndicator(modifier: Modifier = Modifier) {
    val colors = MinecraftTheme.colors

    val infiniteTransition = rememberInfiniteTransition()
    val step by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Row(
        modifier = modifier.wrapContentSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0..3) {
            val isCurrentStep = step == i
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (isCurrentStep) colors.primary else colors.surfaceShadow)
                    .border(1.dp, if (isCurrentStep) colors.surfaceHighlight else Color.Transparent)
            )
        }
    }
}