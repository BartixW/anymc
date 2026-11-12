/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_back
import anymc.shared.generated.resources.common_close
import anymc.shared.generated.resources.ic_chevron_left
import anymc.shared.generated.resources.ic_close
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.ui.theme.MinecraftColors
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.ui.topAndBottomBorder
import org.jetbrains.compose.resources.stringResource

@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    properties: DialogProperties = DialogProperties(),
    buttons: @Composable RowScope. () -> Unit,
    confirmationContent: (@Composable ColumnScope. () -> Unit)? = null,
    content: @Composable ColumnScope. () -> Unit
) {
    val config by ConfigManager.data.collectAsState()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Column(
            modifier = Modifier
                .border(4.dp, MinecraftTheme.colors.dialogBorder, shape = if (config.nonPixelUI) RoundedCornerShape(16f) else RectangleShape)
                .padding(4.dp)
                .background(MinecraftTheme.colors.dialogBackground, shape = if (config.nonPixelUI) RoundedCornerShape(12f) else RectangleShape)
                .topAndBottomBorder(
                    bottomWidth = 2.dp,
                    bottomColor = MinecraftTheme.colors.dialogBorder
                )
        ) {
            Box(
                modifier = Modifier
                    .then(
                        if (config.nonPixelUI)
                            Modifier.padding(8.dp)
                        else Modifier.topAndBottomBorder(
                            bottomWidth = 2.dp,
                            bottomColor = MinecraftTheme.colors.dialogBorder
                        )
                    )
                    .background(MinecraftTheme.colors.dialogContainerBackground, shape = if (config.nonPixelUI) RoundedCornerShape(16f) else RectangleShape),
            ) {
                TopAppBar(
                    backAction = null,
                    title = title,
                    actions = {
                        Box(
                            modifier = Modifier
                                .clickable(onClick = onDismissRequest)
                        ) {
                            Icon(
                                resource = Res.drawable.ic_close,
                                contentDescription = stringResource(Res.string.common_close),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    },
                    transparent = true,
                )
            }
            Column(Modifier.padding(16.dp)) {
                content()
            }
            Box(
                Modifier
                    .then(
                        if (config.nonPixelUI)
                            Modifier.padding(8.dp)
                        else Modifier.topAndBottomBorder(
                            topWidth = 2.dp,
                            topColor = MinecraftTheme.colors.dialogBorder
                        )
                    )
                    .background(MinecraftTheme.colors.dialogContainerBackground, shape = if (config.nonPixelUI) RoundedCornerShape(16f) else RectangleShape),
            ) {
                Row(
                    modifier = Modifier
                        .padding(if (config.nonPixelUI) 8.dp else 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        if (confirmationContent != null) confirmationContent()
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        buttons()
                    }
                }
            }
        }
    }
}