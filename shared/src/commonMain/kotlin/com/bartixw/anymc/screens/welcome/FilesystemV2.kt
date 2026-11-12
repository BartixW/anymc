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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McLoadingIndicator
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WelcomeFilesystemScreenV2(
    navController: NavController,
    viewModel: WelcomeFilesystemViewModel = viewModel { WelcomeFilesystemViewModel() }
) {
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect {
            delay(400.milliseconds)
            navController.navigate("welcome/actions") {
                popUpTo("welcome/filesystem") { inclusive = true }
            }
        }
    }

    Column(
        Modifier.padding(80.dp).fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (viewModel.uiState) {
            is WelcomeFilesystemViewModel.UiState.Checking -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.welcomev2_filesystem_loading),
                        style = MinecraftTheme.typographyPixel.copy(textAlign = TextAlign.Center)
                    )
                }
            }
            is WelcomeFilesystemViewModel.UiState.NeedsSudo -> {
                Text(stringResource(Res.string.welcome_fs_sudo_needed, PlatformUtils.sharedStorageDirectory.absolutePath))
                Button(
                    content = { Text(stringResource(Res.string.welcome_fs_create_dir)) },
                    onClick = { viewModel.createSharedStorage() }
                )
            }
            is WelcomeFilesystemViewModel.UiState.Error -> {
                Text(stringResource(Res.string.welcome_fs_error))
                Button(
                    content = { Text(stringResource(Res.string.common_retry)) },
                    onClick = { viewModel.createSharedStorage() }
                )
            }
        }
    }
}
