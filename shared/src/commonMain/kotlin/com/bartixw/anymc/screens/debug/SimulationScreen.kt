/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bartixw.anymc.minecraft.data.DownloadStatus
import com.bartixw.anymc.minecraft.data.MinecraftStatus
import com.bartixw.anymc.minecraft.data.StatusType
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.McTopAppBar

@Composable
fun SimulationScreen(navController: NavController) {
    val downloadStatus by MinecraftManager.downloadStatus.collectAsState()
    val minecraftStatus by MinecraftManager.minecraftStatus.collectAsState()

    Column(Modifier.fillMaxSize()) {
        McTopAppBar(
            navController = navController,
            title = "State Simulation",
            actions = emptyList()
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            McText("Minecraft Status", modifier = Modifier.padding(bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MinecraftStatus.entries.forEach { status ->
                    McButton(
                        text = status.name,
                        onClick = { MinecraftManager.updateMinecraftStatus(status) },
                        primary = minecraftStatus == status,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            McText("Download Status Type", modifier = Modifier.padding(bottom = 8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusType.entries.forEach { type ->
                    McButton(
                        text = type.name,
                        onClick = {
                            MinecraftManager.updateDownloadStatus(
                                downloadStatus.copy(type = type)
                            )
                        },
                        primary = downloadStatus.type == type
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            McText("Download Progress: ${downloadStatus.current} / ${downloadStatus.total}")
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McButton(
                    text = "-10",
                    onClick = {
                        MinecraftManager.updateDownloadStatus(
                            downloadStatus.copy(current = (downloadStatus.current - 10).coerceAtLeast(0))
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                McButton(
                    text = "+10",
                    onClick = {
                        MinecraftManager.updateDownloadStatus(
                            downloadStatus.copy(current = (downloadStatus.current + 10).coerceAtMost(downloadStatus.total))
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McButton(
                    text = "Total 100",
                    onClick = {
                        MinecraftManager.updateDownloadStatus(
                            downloadStatus.copy(total = 100)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                McButton(
                    text = "Reset Progress",
                    onClick = {
                        MinecraftManager.updateDownloadStatus(
                            downloadStatus.copy(current = 0, total = 0, type = StatusType.IDLE)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            McText("Error Simulation")
            McButton(
                text = if (downloadStatus.errorCode == null) "Set Error" else "Clear Error",
                onClick = {
                    if (downloadStatus.errorCode == null) {
                        MinecraftManager.updateDownloadStatus(
                            downloadStatus.copy(errorCode = "Simulated Error: Something went wrong!")
                        )
                    } else {
                        MinecraftManager.cleanError()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}
