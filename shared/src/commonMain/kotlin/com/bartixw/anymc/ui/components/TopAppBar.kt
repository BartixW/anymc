/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_chevron_left
import com.bartixw.anymc.ui.theme.LocalMinecraftColors

@Composable
fun McTopAppBar(
    backAction: (() -> Unit)?,
    title: String,
    actions: List<@Composable () -> Unit>
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .background(LocalMinecraftColors.current.surfaceShadow),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (backAction != null) {
            McButton(
                text = "Wróć",
                icon = Res.drawable.ic_chevron_left,
                onClick = { backAction() },
                modifier = Modifier.fillMaxHeight()
            )
        }
        Spacer(Modifier.size(16.dp))
        McText(
            text = title,
            modifier = Modifier.weight(1f)
        )
        actions.forEach { action -> action() }
    }
}

@Composable
fun McTopAppBar(
    navController: NavController,
    title: String,
    actions: List<@Composable () -> Unit>
) {
    McTopAppBar(
        backAction = {
            navController.popBackStack()
        },
        title = title,
        actions = actions
    )
}