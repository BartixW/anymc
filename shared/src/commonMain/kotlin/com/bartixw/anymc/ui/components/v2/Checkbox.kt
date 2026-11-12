/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ui_check
import anymc.shared.generated.resources.ui_empty
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.ui.theme.MinecraftTheme

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val config by ConfigManager.data.collectAsState()

    Icon(
        resource = if (checked) Res.drawable.ui_check else Res.drawable.ui_empty,
        contentDescription = null,
        modifier = Modifier
            .size(32.dp)
            .background(MinecraftTheme.colors.checkboxBackground, shape = if (config.nonPixelUI) RoundedCornerShape(4f) else RectangleShape)
            .border(2.dp, MinecraftTheme.colors.checkboxBorder, shape = if (config.nonPixelUI) RoundedCornerShape(4f) else RectangleShape)
            .clickable(onClick = {
                onCheckedChange(!checked)
            })
            .padding(8.dp)
            .then(modifier)
    )
}