/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_loading_1
import anymc.shared.generated.resources.ic_loading_2
import anymc.shared.generated.resources.ic_loading_3
import anymc.shared.generated.resources.ic_loading_4
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    speedMillis: Long = 75L
) {
    val icons = remember {
        listOf<DrawableResource>(
            Res.drawable.ic_loading_1,
            Res.drawable.ic_loading_2,
            Res.drawable.ic_loading_3,
            Res.drawable.ic_loading_4
        )
    }

    var state by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(speedMillis.milliseconds)
            state = (state + 1) % 16
        }
    }

    val iconIndex = state % 4
    val rotation = (state / 4) * 90f

    Icon(
        resource = icons[iconIndex],
        contentDescription = "Loading",
        modifier = Modifier
            .size(32.dp)
            .rotate(rotation)
            .then(modifier)
    )
}
