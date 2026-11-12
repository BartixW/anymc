/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.ui.components.v2

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_back
import anymc.shared.generated.resources.ic_chevron_left
import com.bartixw.anymc.ui.theme.LocalTextStyle
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopAppBar(
    backAction: (() -> Unit)?,
    title: @Composable () -> Unit,
    actions: @Composable RowScope. () -> Unit,
    transparent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .then(
                if (!transparent) Modifier
                    .background(MinecraftTheme.colors.surfaceShadow)
                    .border(2.dp, MinecraftTheme.colors.topAppBarBorder)
                else Modifier
            )
            .padding(8.dp)
            .fillMaxWidth()
            .then(modifier),
    ) {
        if (backAction != null) Box(
            modifier = Modifier
                .clickable(onClick = backAction)
                .align(Alignment.CenterStart)
        ) {
            Icon(
                resource = Res.drawable.ic_chevron_left,
                contentDescription = stringResource(Res.string.common_back),
                modifier = Modifier.padding(8.dp)
            )
        } else Box {}
        Box(Modifier.align(Alignment.Center)) {
            CompositionLocalProvider(
                LocalTextStyle provides MinecraftTheme.typographyPixel.copy(color = MinecraftTheme.colors.topAppBarText)
            ) {
                title()
            }
        }
        Row(Modifier.align(Alignment.CenterEnd)) {
            actions()
        }
    }
}

@Composable
fun TopAppBar(
    navController: NavController,
    title: @Composable () -> Unit,
    actions: @Composable RowScope. () -> Unit
) {
    TopAppBar(
        backAction = {
            navController.popBackStack()
        },
        title = title,
        actions = actions
    )
}