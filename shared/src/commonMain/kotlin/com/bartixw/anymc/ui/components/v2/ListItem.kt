/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.ui.topAndBottomBorder

@Composable
fun ListItem(
    baseModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val config by ConfigManager.data.collectAsState()

    Box(
        modifier = baseModifier
            .then(if (config.nonPixelUI) Modifier.padding(4.dp) else Modifier)
            .background(MinecraftTheme.colors.listItemBackground, shape = if (config.nonPixelUI) RoundedCornerShape(4.dp) else RectangleShape)
            .then(if (!config.nonPixelUI) Modifier.topAndBottomBorder(
                topWidth = 2.dp,
                topColor = MinecraftTheme.colors.listItemHighlight,
                bottomWidth = 2.dp,
                bottomColor = MinecraftTheme.colors.listItemShadow
            ) else Modifier)
            .padding(16.dp)
            .then(modifier)
    ) {
        content()
    }
}