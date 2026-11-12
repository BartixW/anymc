/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.ui.theme.LocalTextStyle
import com.bartixw.anymc.ui.theme.MinecraftTheme

enum class ButtonType {
    Hero,
    Primary,
    Secondary,
    Destructive,
    Dark
}

@Composable
fun Button(
    content: @Composable () -> Unit,
    type: ButtonType = ButtonType.Secondary,
    disabled: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rootModifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val inputMode = LocalInputModeManager.current.inputMode
    val showFocusOutline = isFocused && inputMode == InputMode.Keyboard
    val config by ConfigManager.data.collectAsState()

    val colors = MinecraftTheme.colors

    // Colors mapping based on state and type
    val backgroundColor = when {
        disabled -> colors.buttonDisabledBackground
        isPressed -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryPressedBackground
            ButtonType.Secondary -> colors.buttonSecondaryPressedBackground
            ButtonType.Destructive -> colors.buttonDestructivePressedBackground
            ButtonType.Dark -> colors.buttonDarkPressedBackground
        }
        isHovered -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryHoverBackground
            ButtonType.Secondary -> colors.buttonSecondaryHoverBackground
            ButtonType.Destructive -> colors.buttonDestructiveHoverBackground
            ButtonType.Dark -> colors.buttonDarkHoverBackground
        }
        else -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryBackground
            ButtonType.Secondary -> colors.buttonSecondaryBackground
            ButtonType.Destructive -> colors.buttonDestructiveBackground
            ButtonType.Dark -> colors.buttonDarkBackground
        }
    }

    val shadow = when {
        disabled -> colors.buttonDisabledShadow
        isHovered -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryHoverShadow
            ButtonType.Secondary -> colors.buttonSecondaryHoverShadow
            ButtonType.Destructive -> colors.buttonDestructiveHoverShadow
            ButtonType.Dark -> colors.buttonDarkHoverShadow
        }
        else -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryShadow
            ButtonType.Secondary -> colors.buttonSecondaryShadow
            ButtonType.Destructive -> colors.buttonDestructiveShadow
            ButtonType.Dark -> colors.buttonDarkShadow
        }
    }

    val innerBorder = when {
        disabled -> colors.buttonDisabledBorder
        isPressed -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryPressedBorder
            ButtonType.Secondary -> colors.buttonSecondaryPressedBorder
            ButtonType.Destructive -> colors.buttonDestructivePressedBorder
            ButtonType.Dark -> colors.buttonDarkPressedBorder
        }
        isHovered -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryHoverBorder
            ButtonType.Secondary -> colors.buttonSecondaryHoverBorder
            ButtonType.Destructive -> colors.buttonDestructiveHoverBorder
            ButtonType.Dark -> colors.buttonDarkHoverBorder
        }
        else -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryBorder
            ButtonType.Secondary -> colors.buttonSecondaryBorder
            ButtonType.Destructive -> colors.buttonDestructiveBorder
            ButtonType.Dark -> colors.buttonDarkBorder
        }
    }

    val outerBorder = when {
        disabled -> colors.buttonDisabledOuterBorder
        isPressed -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryPressedOuterBorder
            ButtonType.Secondary -> colors.buttonSecondaryPressedOuterBorder
            ButtonType.Destructive -> colors.buttonDestructivePressedOuterBorder
            ButtonType.Dark -> colors.buttonDarkPressedOuterBorder
        }
        isHovered -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryHoverOuterBorder
            ButtonType.Secondary -> colors.buttonSecondaryHoverOuterBorder
            ButtonType.Destructive -> colors.buttonDestructiveHoverOuterBorder
            ButtonType.Dark -> colors.buttonDarkHoverOuterBorder
        }
        else -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryOuterBorder
            ButtonType.Secondary -> colors.buttonSecondaryOuterBorder
            ButtonType.Destructive -> colors.buttonDestructiveOuterBorder
            ButtonType.Dark -> colors.buttonDarkOuterBorder
        }
    }

    val textColor = when {
        disabled -> colors.buttonDisabledText
        isPressed -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryPressedText
            ButtonType.Secondary -> colors.buttonSecondaryPressedText
            ButtonType.Destructive -> colors.buttonDestructivePressedText
            ButtonType.Dark -> colors.buttonDarkPressedText
        }
        isHovered -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryHoverText
            ButtonType.Secondary -> colors.buttonSecondaryHoverText
            ButtonType.Destructive -> colors.buttonDestructiveHoverText
            ButtonType.Dark -> colors.buttonDarkHoverText
        }
        else -> when(type) {
            ButtonType.Primary, ButtonType.Hero -> colors.buttonPrimaryText
            ButtonType.Secondary -> colors.buttonSecondaryText
            ButtonType.Destructive -> colors.buttonDestructiveText
            ButtonType.Dark -> colors.buttonDarkText
        }
    }

    Box (
        Modifier
            .padding(top = if (isPressed || config.nonPixelUI) 4.dp else 0.dp)
            .then(rootModifier)
    ) {
        Box(
            modifier = Modifier
                .zIndex(if (showFocusOutline) 1f else 0f)
                .then(if (showFocusOutline) Modifier.drawBehind {
                    val outlineWidth = 2.dp.toPx()
                    val gap = 2.dp.toPx()
                    if (config.nonPixelUI) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(-outlineWidth - gap, -outlineWidth - gap),
                            size = Size(size.width + (outlineWidth + gap) * 2, size.height + (outlineWidth + gap) * 2),
                            cornerRadius = CornerRadius(10.dp.toPx()),
                            style = Stroke(width = outlineWidth)
                        )
                    } else {
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(-outlineWidth, -outlineWidth),
                            size = Size(size.width + outlineWidth + gap, size.height + outlineWidth + gap),
                            style = Stroke(width = outlineWidth)
                        )
                    }
                } else Modifier)
                .clip(if (config.nonPixelUI) RoundedCornerShape(8.dp) else RectangleShape)
                .background(outerBorder, shape = if (config.nonPixelUI) RoundedCornerShape(8.dp) else RectangleShape)
                .then(if (disabled) Modifier else Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                    .hoverable(interactionSource)
                )
                .padding(top = 2.dp, start = 2.dp, end = 2.dp, bottom = if (isPressed || config.nonPixelUI) 2.dp else 6.dp)
                .height(40.dp)
                .drawBehind {
                    val size = this.size
                    val strokeWidth = 2.dp.toPx()

                    if (config.nonPixelUI) {
                        val cornerRadius = CornerRadius(6.dp.toPx())
                        // Button background
                        drawRoundRect(backgroundColor, cornerRadius = cornerRadius)

                        // Inner border
                        
                    } else {
                        // Button background
                        drawRect(backgroundColor)

                        // Inner bevel
                        drawRect(innerBorder, topLeft = Offset.Zero, size = Size(size.width, strokeWidth)) // Top
                        drawRect(innerBorder, topLeft = Offset(0f, size.height - strokeWidth), size = Size(size.width, strokeWidth)) // Bottom
                        drawRect(innerBorder, topLeft = Offset(0f, 0f), size = Size(strokeWidth, size.height)) // Left
                        drawRect(innerBorder, topLeft = Offset(size.width - strokeWidth, 0f), size = Size(strokeWidth, size.height)) // Right

                        // Shadow below the button (visible when not pressed)
                        if (!isPressed) {
                            drawRect(shadow, topLeft = Offset(0f, size.height), size = Size(size.width, strokeWidth * 2))
                        }
                    }
                }
                .then(modifier),
            contentAlignment = Alignment.Center
        ) {

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                val textStyle = (if (type == ButtonType.Hero) MinecraftTheme.typographyAnyMC else MinecraftTheme.typographyPixel).merge(TextStyle(color = textColor))

                CompositionLocalProvider(
                    LocalTextStyle provides textStyle
                ) {
                    content()
                }
            }
        }
    }
}