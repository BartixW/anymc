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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.status_asset_dl
import anymc.shared.generated.resources.status_asset_fetch
import anymc.shared.generated.resources.status_client_dl
import anymc.shared.generated.resources.status_extract
import anymc.shared.generated.resources.status_info_fetch
import anymc.shared.generated.resources.status_info_write
import anymc.shared.generated.resources.status_java_fetch
import anymc.shared.generated.resources.status_java_install
import anymc.shared.generated.resources.status_library_dl
import anymc.shared.generated.resources.status_modpack_dl
import anymc.shared.generated.resources.welcomev2_filesystem_loading
import anymc.shared.generated.resources.welcomev2_installing_button
import anymc.shared.generated.resources.welcomev2_installing_text
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.minecraft.data.DownloadStatus
import com.bartixw.anymc.minecraft.data.StatusType
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeInstallingScreen(navController: NavController) {
    val downloadStatus by MinecraftManager.downloadStatus.collectAsState()

    val progressFraction = remember(downloadStatus.current, downloadStatus.total) {
        if (downloadStatus.total > 0) {
            downloadStatus.current.toFloat() / downloadStatus.total
        } else {
            0f
        }
    }

    LaunchedEffect(downloadStatus.type) {
        if (downloadStatus.type == StatusType.IDLE) {
            navController.navigate("main") {
                popUpTo(0) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    val percentageText = "${(progressFraction * 100).toInt()}%"

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        LoadingIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.welcomev2_installing_text, when (downloadStatus.type) {
                StatusType.INFO_FETCH -> stringResource(Res.string.status_info_fetch)
                StatusType.INFO_WRITE -> stringResource(Res.string.status_info_write)
                StatusType.CLIENT_DL -> stringResource(Res.string.status_client_dl)
                StatusType.LIBRARY_DL ->
                    stringResource(
                        Res.string.status_library_dl,
                        downloadStatus.current,
                        downloadStatus.total,
                        percentageText
                    )
                StatusType.ASSET_FETCH -> stringResource(Res.string.status_asset_fetch)
                StatusType.ASSET_DL ->
                    stringResource(
                        Res.string.status_asset_dl,
                        downloadStatus.current,
                        downloadStatus.total,
                        percentageText
                    )

                StatusType.JAVA_FETCH -> stringResource(Res.string.status_java_fetch)
                StatusType.JAVA_INSTALL ->
                    stringResource(
                        Res.string.status_java_install,
                        downloadStatus.current,
                        downloadStatus.total,
                        percentageText
                    )
                StatusType.MODPACK_DL -> stringResource(Res.string.status_modpack_dl)
                StatusType.EXTRACT -> stringResource(Res.string.status_extract)
                else -> ""
            }),
            style = MinecraftTheme.typographyPixel.copy(textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(16.dp))
        Button(
            content = { Text(stringResource(Res.string.welcomev2_installing_button)) },
            onClick = {
                navController.navigate("main") {
                    popUpTo(0) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )
    }
}