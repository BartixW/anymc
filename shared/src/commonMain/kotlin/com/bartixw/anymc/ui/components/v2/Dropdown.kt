/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_chevron_down
import com.bartixw.anymc.ui.theme.MinecraftTheme

@Composable
fun <T> Dropdown(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() },
    descriptionProvider: (T) -> String? = { null },
    itemActions: @Composable (T) -> Unit = {},
    itemContent: @Composable (T) -> Unit = { option ->
        val description = descriptionProvider(option)
        Column {
            Text(text = labelProvider(option))
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
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }
    val colors = MinecraftTheme.colors
    val density = LocalDensity.current

    val lazyListState = rememberLazyListState()

    Box(modifier = modifier) {
        Button(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { buttonSize = it.size },
            type = ButtonType.Dark,
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = labelProvider(selectedOption))
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        resource = Res.drawable.ic_chevron_down,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )

        if (expanded && buttonSize != IntSize.Zero) {
            val selectedIndex = remember(options, selectedOption) { options.indexOf(selectedOption) }
            if (selectedIndex != -1) {
                LaunchedEffect(expanded) {
                    lazyListState.scrollToItem(selectedIndex)
                }
            }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 0),
                onDismissRequest = { expanded = false }
            ) {
                Box(modifier = Modifier.wrapContentSize()) {
                    Box(
                        modifier = Modifier
                            .width(with(density) { buttonSize.width.toDp() + 4.dp })
                            .heightIn(max = 400.dp)
                            .background(colors.dropdownBackground)
                            .border(2.dp, colors.dropdownBorder)
                            .padding(2.dp)
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                //.padding(end = 12.dp)
                        ) {
                            items(options) { option ->
                                DropdownItem(
                                    isSelected = option == selectedOption,
                                    onClick = {
                                        onOptionSelected(option)
                                        expanded = false
                                    },
                                    content = { itemContent(option) },
                                    actions = { itemActions(option) }
                                )
                            }
                        }
//                        TODO: Implement scrollbar properly, so it doesn't make the list always 400dp long
//                        VerticalScrollbar(
//                            modifier = Modifier
//                                .align(Alignment.CenterEnd)
//                                .width(12.dp),
//                            adapter = rememberScrollbarAdapter(lazyListState)
//                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
    actions: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val inputMode = LocalInputModeManager.current.inputMode
    
    val colors = MinecraftTheme.colors

    val backgroundColor = when {
        isSelected -> colors.buttonPrimaryBackground.copy(alpha = 0.2f)
        isHovered || isFocused -> colors.dropdownInnerBorder
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .drawBehind {
                if (isFocused && inputMode == InputMode.Keyboard) {
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            content()
        }

        Box(modifier = Modifier.padding(end = 4.dp)) {
            actions()
        }
    }
}
