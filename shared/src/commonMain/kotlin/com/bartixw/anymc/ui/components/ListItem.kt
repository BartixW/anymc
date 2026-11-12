/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.DrawableResource


@Composable
fun McListItem(
    title: String,
    description: String? = null,
    icon: DrawableResource? = null,
    iconBitmap: ImageBitmap? = null,
    disabled: Boolean = false,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    alignment: Alignment = Alignment.CenterStart,
    bigDescriptionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val inputMode = LocalInputModeManager.current.inputMode

    val colors = MinecraftTheme.colors
    val backgroundColor = when {
        disabled -> colors.controlDisabledBackground
        selected -> colors.controlPrimaryBackground
        else -> colors.controlBackground
    }
    val highlightColor = when {
        selected -> colors.controlPrimaryHighlight
        else -> colors.controlHighlight
    }
    val shadowColor = when {
        selected -> colors.controlPrimaryShadow
        else -> colors.controlShadow
    }
    val borderColor = if (selected) colors.controlPrimaryBorder else colors.controlBorder

    val topColor = if (isPressed) shadowColor else highlightColor
    val leftColor = if (isPressed) shadowColor else highlightColor
    val bottomColor = if (isPressed) highlightColor else shadowColor
    val rightColor = if (isPressed) highlightColor else shadowColor

    Box(
        modifier = modifier
            .zIndex(if (isFocused && inputMode == InputMode.Keyboard) 1f else 0f)
            .then(if (isFocused && inputMode == InputMode.Keyboard) Modifier.drawBehind {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                    size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            } else Modifier)
            .background(borderColor)
            .then(if (disabled || onClick == null) Modifier else Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ))
            .padding(2.dp)
            .drawBehind {
                val size = this.size
                val strokeWidth = 2.dp.toPx()

                drawRect(backgroundColor)

                // Top
                drawRect(topColor, topLeft = Offset.Zero, size = Size(size.width, strokeWidth))
                // Left
                drawRect(leftColor, topLeft = Offset.Zero, size = Size(strokeWidth, size.height))
                // Bottom
                drawRect(bottomColor, topLeft = Offset(0f, size.height - strokeWidth), size = Size(size.width, strokeWidth))
                // Right
                drawRect(rightColor, topLeft = Offset(size.width - strokeWidth, 0f), size = Size(strokeWidth, size.height))
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(2.dp),
        contentAlignment = alignment
    ) {
        val contentOffset = if (isPressed) 2.dp else 0.dp
        Row(
            modifier = Modifier.offset(x = contentOffset, y = contentOffset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    filterQuality = FilterQuality.None
                )
                Spacer(Modifier.size(16.dp))
            } else if (icon != null) {
                McIcon(
                    resource = icon,
                    tint = MinecraftTheme.colors.textPrimary,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.size(16.dp))
            }
            Column (Modifier.weight(1f)) {
                McText(
                    text = title,
                )
                if (description != null) {
                    Spacer(Modifier.size(8.dp))
                    McText(
                        style = TextStyle(
                            color = if (bigDescriptionMode) MinecraftTheme.colors.textPrimary else MinecraftTheme.colors.textSecondary,
                            fontSize = if (bigDescriptionMode) 24.sp else 12.sp
                        ),
                        text = description,
                    )
                }
            }
            if (trailingContent != null) trailingContent()
        }
    }
}

@Preview
@Composable
fun McListItemPreview() {
    MinecraftTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            McListItem(
                title = "Survival World",
                description = "Day 124 - Hardcore",
                onClick = {}
            )
            McListItem(
                title = "Creative Build",
                description = "Redstone testing",
                selected = true,
                onClick = {}
            )
            McListItem(
                title = "Old World",
                description = "Version 1.12.2",
                disabled = true,
                onClick = {}
            )
        }
    }
}
