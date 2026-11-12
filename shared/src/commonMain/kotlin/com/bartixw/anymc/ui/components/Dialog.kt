/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bartixw.anymc.ui.theme.MinecraftTheme

@Composable
fun McDialog(
    onDismissRequest: () -> Unit,
    title: String,
    properties: DialogProperties = DialogProperties(),
    buttons: @Composable RowScope. () -> Unit = {},
    content: @Composable ColumnScope. () -> Unit
) {
    val colors = MinecraftTheme.colors

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = Modifier
                .widthIn(400.dp, 800.dp)
                .wrapContentHeight()
                .background(colors.backgroundDirt)
                .border(2.dp, colors.surfaceHighlight)
                .padding(2.dp)
                .border(2.dp, colors.surfaceShadow)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                McText(
                    text = title,
                    style = MinecraftTheme.typography
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    content = buttons
                )
            }
        }
    }
}