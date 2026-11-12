/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bartixw.anymc.ui.theme.LocalTextStyle
import com.bartixw.anymc.ui.theme.MinecraftTheme

@Composable
fun SidebarItem(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit = {},
    onClick: () -> Unit,
    selected: Boolean
) {
    val selectedBarColor = MinecraftTheme.colors.sidebarItemSelectedBar
    val barThickness = 4f

    Box(
        Modifier
            .clickable(onClick = onClick).fillMaxWidth()
            .then(
                if (selected)
                Modifier.drawBehind {
                    drawRect(
                        color = selectedBarColor,
                        topLeft = Offset(0f, 5f),
                        size = Size(barThickness, this.size.height - 5f)
                    )
                }
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(
                    LocalIconModifier provides Modifier.fillMaxSize()
                ) {
                    icon()
                }
            }
            Spacer(Modifier.width(8.dp).height(16.dp))
            Column {
                CompositionLocalProvider(
                    LocalTextStyle provides MinecraftTheme.typographyPixel
                ) {
                    title()
                }
                description()
            }
        }
    }
}