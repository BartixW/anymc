/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.v2.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.anymc_link_welcome_bg
import anymc.shared.generated.resources.common_more_info
import anymc.shared.generated.resources.settings_link_header
import anymc.shared.generated.resources.settings_link_main_button
import anymc.shared.generated.resources.settings_link_main_description
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LinkSection(navController: NavController) {
    Box(Modifier.background(Color.Black)) {
        Image(
            bitmap = imageResource(Res.drawable.anymc_link_welcome_bg),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.5f)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(
                    top = 48.dp,
                    bottom = 96.dp,
                    start = 8.dp,
                    end = 8.dp
                )
                .fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.settings_link_header).uppercase(),
                    style = MinecraftTheme.typographyAnyMC.copy(fontSize = 48.sp)
                )
                Spacer(Modifier.size(16.dp))
                Text(stringResource(Res.string.settings_link_main_description))
            }
            Row {
                Button(
                    content = { Text(stringResource(Res.string.settings_link_main_button).uppercase()) },
                    type = ButtonType.Hero,
                    onClick = {
                        navController.navigate("link/setup/download")
                    }
                )
                Spacer(Modifier.size(16.dp))
                Button(
                    content = { Text(stringResource(Res.string.common_more_info)) },
                    onClick = {}
                )
            }
        }
    }
}