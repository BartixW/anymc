/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bartixw.anymc.ui.theme.MinecraftTheme

@Composable
fun McTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = MinecraftTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val inputMode = LocalInputModeManager.current.inputMode

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MinecraftTheme.typography,
        cursorBrush = SolidColor(colors.textPrimary),
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .zIndex(if (isFocused && inputMode == InputMode.Keyboard) 1f else 0f)
                    .then(if (isFocused && inputMode == InputMode.Keyboard) Modifier.drawBehind {
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                            size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    } else Modifier)
                    .background(MinecraftTheme.colors.v1inputBackground)
                    .border(2.dp, colors.surfaceShadow)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (value.isEmpty()) {
                    McText(
                        text = placeholder,
                        style = TextStyle(color = colors.textSecondary)
                    )
                }
                innerTextField()
            }
        }
    )
}