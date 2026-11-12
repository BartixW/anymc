/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_chevron_down
import com.bartixw.anymc.ui.theme.MinecraftTheme

@Composable
fun McDropdown(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (String) -> String = { it }
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }
    val colors = MinecraftTheme.colors
    val density = LocalDensity.current

    val lazyListState = rememberLazyListState()
    var dropdownHeightPx by remember { mutableStateOf(0) }

    Box(modifier = modifier) {
        McButton(
            icon = Res.drawable.ic_chevron_down,
            text = labelProvider(selectedOption),
            onClick = { expanded = !expanded },
            modifier = Modifier
                .background(colors.v1inputBackground)
                .onGloballyPositioned { buttonSize = it.size }
        )

        if (expanded && buttonSize != IntSize.Zero) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, buttonSize.height),
                onDismissRequest = { expanded = false }
            ) {
                Box(
                    modifier = Modifier
                        .width(with(density) { buttonSize.width.toDp() })
                        .background(colors.v1inputBackground)
                        .border(2.dp, colors.surfaceShadow)
                        .onGloballyPositioned { coordinates ->
                            dropdownHeightPx = coordinates.size.height
                        }
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp)
                    ) {
                        items(options) { option ->
                            val itemInteractionSource = remember { MutableInteractionSource() }
                            val isItemFocused by itemInteractionSource.collectIsFocusedAsState()
                            val inputMode = LocalInputModeManager.current.inputMode

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (isItemFocused && inputMode == InputMode.Keyboard) 1f else 0f)
                                    .then(if (isItemFocused && inputMode == InputMode.Keyboard) Modifier.drawBehind {
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                                            size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    } else Modifier)
                                    .clickable(
                                        interactionSource = itemInteractionSource,
                                        indication = null,
                                        onClick = {
                                            onOptionSelected(option)
                                            expanded = false
                                        }
                                    )
                                    .padding(12.dp)
                            ) {
                                McText(text = labelProvider(option))
                            }
                        }
                    }

                    if (dropdownHeightPx > 0) {
                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .height(with(density) { dropdownHeightPx.toDp() })
                                .width(12.dp)
                                .background(colors.surfaceShadow.copy(alpha = 0.5f)),
                            adapter = rememberScrollbarAdapter(lazyListState),
                        )
                    }
                }
            }
        }
    }
}