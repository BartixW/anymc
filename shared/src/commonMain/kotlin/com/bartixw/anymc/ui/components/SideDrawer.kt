/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bartixw.anymc.ui.theme.MinecraftTheme

enum class DrawerSide {
    Left, Right
}

@Composable
fun McSideDrawer(
    isOpen: Boolean,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    drawerWidth: Dp = 300.dp,
    side: DrawerSide = DrawerSide.Left,
    scrimColor: Color = Color.Black.copy(alpha = 0.4f),
    maxScrimAlpha: Float = 0.8f,
    drawerBackground: Color = MinecraftTheme.colors.backgroundDirt,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val targetOffset = when (side) {
        DrawerSide.Left -> if (isOpen) 0.dp else -drawerWidth
        DrawerSide.Right -> if (isOpen) 0.dp else drawerWidth
    }
    val offsetAction by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 300),
        label = "DrawerOffsetAnimation"
    )

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isOpen) maxScrimAlpha else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "ScrimFadeAnimation"
    )

    val drawerAlignment = when (side) {
        DrawerSide.Left -> Alignment.TopStart
        DrawerSide.Right -> Alignment.TopEnd
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimAlpha }
                    .background(scrimColor)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onCloseRequest() })
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .align(drawerAlignment)
                .offset(x = offsetAction)
                .background(drawerBackground)
        ) {
            drawerContent()
        }
    }
}