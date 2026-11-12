/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.system.exitProcess

@Composable
fun EulaScreen(navController: NavController) {
    var declined by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.eula_title),
                style = MinecraftTheme.typographyPixel.copy(fontSize = 24.sp, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(16.dp))
            if (!declined) {
                Text(
                    text = stringResource(Res.string.eula_description),
                    style = TextStyle(textAlign = TextAlign.Center),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    content = { Text(stringResource(Res.string.eula_read_web_button)) },
                    type = ButtonType.Secondary,
                    onClick = { PlatformUtils.openBrowser("https://www.minecraft.net/eula") },
                )
            } else {
                Text(
                    text = "${stringResource(Res.string.eula_declined_title)}\n\n${stringResource(Res.string.eula_declined_desc)}",
                    style = TextStyle(textAlign = TextAlign.Center),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!declined) {
                Button(
                    content = { Text(stringResource(Res.string.eula_decline_button)) },
                    type = ButtonType.Secondary,
                    onClick = { declined = true },
                )
                Button(
                    content = { Text(stringResource(Res.string.eula_agree_button)) },
                    type = ButtonType.Hero,
                    onClick = {
                        ConfigManager.update { it.copy(eulaAccepted = true) }
                        navController.navigate("welcome/download_textures") {
                            popUpTo("welcome/eula") { inclusive = true }
                        }
                    },
                )
            } else {
                Button(
                    content = { Text(stringResource(Res.string.eula_exit_button)) },
                    type = ButtonType.Hero,
                    onClick = {
                        exitProcess(0)
                    },
                )
            }
        }
    }
}
