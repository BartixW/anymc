/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.managers.TextureManager
import com.bartixw.anymc.mc3d.Mc3DController
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun DownloadTexturesScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        val success = TextureManager.ensureTexturesDownloaded()
        if (success) {
            Mc3DController.init()
            val targetRoute = if (ConfigManager.data.value.isNew) "welcome" else "main"
            navController.navigate(targetRoute) {
                popUpTo("welcome/download_textures") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LoadingIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.texture_download_message),
            style = MinecraftTheme.typographyPixel.copy(textAlign = TextAlign.Center),
        )
    }
}
