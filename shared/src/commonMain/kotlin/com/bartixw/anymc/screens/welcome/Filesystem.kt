/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McLoadingIndicator
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.core.utils.PlatformUtils
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeFilesystemScreen(
    navController: NavController,
    viewModel: WelcomeFilesystemViewModel = viewModel { WelcomeFilesystemViewModel() }
) {
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect {
            navController.navigate("welcome/actions") {
                popUpTo("welcome/filesystem") { inclusive = true }
            }
        }
    }

    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (viewModel.uiState) {
            is WelcomeFilesystemViewModel.UiState.Checking -> {
                McLoadingIndicator()
            }
            is WelcomeFilesystemViewModel.UiState.NeedsSudo -> {
                McText(stringResource(Res.string.welcome_fs_sudo_needed, PlatformUtils.sharedStorageDirectory.absolutePath))
                McButton(
                    text = stringResource(Res.string.welcome_fs_create_dir),
                    icon = Res.drawable.ic_plus,
                    primary = true,
                    onClick = { viewModel.createSharedStorage() }
                )
            }
            is WelcomeFilesystemViewModel.UiState.Error -> {
                McText(stringResource(Res.string.welcome_fs_error))
                McButton(
                    text = stringResource(Res.string.common_retry),
                    icon = Res.drawable.ic_plus,
                    primary = true,
                    onClick = { viewModel.createSharedStorage() }
                )
            }
        }
    }
}
