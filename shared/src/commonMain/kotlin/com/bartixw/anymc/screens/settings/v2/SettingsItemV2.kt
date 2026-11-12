/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size

@Composable
fun SettingsItemV2(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth(),
        baseModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (icon != null) {
                    Box(
                        modifier = Modifier.padding(end = 16.dp).size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                }
                Column {
                    Text(text = title)
                    if (description != null) {
                        Text(
                            text = description,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = MinecraftTheme.colors.textSecondary
                            )
                        )
                    }
                }
            }
            if (trailingContent != null) {
                Row(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    trailingContent()
                }
            }
        }
    }
}
