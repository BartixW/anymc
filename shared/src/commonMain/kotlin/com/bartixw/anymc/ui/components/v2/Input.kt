/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.ui.components.v2.Text

@Composable
fun Input(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    readOnly: Boolean = false,
    disabled: Boolean = false,
    multiLine: Boolean = false,
    textStyle: TextStyle = MinecraftTheme.typographyPixel.copy(color = MinecraftTheme.colors.inputText),
    valueProcessor: @Composable (String) -> String = { it },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier
) {
    val inputShadowColor = MinecraftTheme.colors.inputShadow

    BasicTextField(
        value = valueProcessor(value),
        onValueChange = onValueChange,
        readOnly = readOnly,
        enabled = !disabled,
        textStyle = textStyle,
        singleLine = !multiLine,
        cursorBrush = SolidColor(MinecraftTheme.colors.inputText),
        visualTransformation = visualTransformation,
        modifier = Modifier
            .border(2.dp, MinecraftTheme.colors.inputBorder)
            .background(MinecraftTheme.colors.inputBackground)
            .drawWithContent {
                drawContent()

                val shadowThickness = 6.dp.toPx()

                drawRect(
                    color = inputShadowColor,
                    size = Size(size.width, shadowThickness)
                )
            }
            .padding(14.dp)
            .then(modifier),
        decorationBox = { innerTextField ->
            if (value.isEmpty() && placeholder != null) {
                Text(
                    text = placeholder,
                    style = textStyle.copy(color = MinecraftTheme.colors.inputTextHint)
                )
            }
            innerTextField()
        }
    )
}