/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.v2.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import anymc.shared.generated.resources.*
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.screens.settings.v2.SettingsItemV2
import com.bartixw.anymc.ui.components.v2.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AdvancedSectionV2() {
    val config by ConfigManager.data.collectAsState()
    var showLaunchBehaviorDialog by remember { mutableStateOf(false) }

    if (showLaunchBehaviorDialog) Dialog(
        title = { Text(stringResource(Res.string.settings_adv_launch_behavior_title)) },
        onDismissRequest = {
            showLaunchBehaviorDialog = false
        },
        buttons = {
            Button(
                content = { Text(stringResource(Res.string.common_ok)) },
                onClick = {
                    showLaunchBehaviorDialog = false
                },
                type = ButtonType.Primary
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = config.minimizeOnLaunch,
                    onCheckedChange = { checked ->
                        ConfigManager.update {
                            it.copy(
                                minimizeOnLaunch = checked
                            )
                        }
                    }
                )
                Spacer(Modifier.size(16.dp))
                Text(stringResource(Res.string.settings_adv_launch_minimize))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = config.showConsoleOnLaunch,
                    onCheckedChange = { checked ->
                        ConfigManager.update {
                            it.copy(
                                showConsoleOnLaunch = checked
                            )
                        }
                    }
                )
                Spacer(Modifier.size(16.dp))
                Text(stringResource(Res.string.settings_adv_launch_console))
            }
        }
    }

    LazyColumn {
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_adv_dev_mode_title),
                description = stringResource(Res.string.settings_adv_dev_mode_desc),
                trailingContent = {
                    Checkbox(
                        checked = config.devMode,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    devMode = checked
                                )
                            }
                        }
                    )
                }
            )
        }
        if (PlatformUtils.getCurrentOS() == "linux") {
            item {
                val labelsMap = mapOf(
                    "none" to stringResource(Res.string.common_none),
                    "nvidia" to "Nvidia",
                    "amd" to "AMD"
                )

                SettingsItemV2(
                    title = stringResource(Res.string.settings_adv_gpu_offload_title),
                    description = stringResource(Res.string.settings_adv_gpu_offload_desc),
                    trailingContent = {
                        Dropdown(
                            options = listOf("none", "nvidia", "amd"),
                            selectedOption = config.gpuOffload,
                            onOptionSelected = { option ->
                                ConfigManager.update {
                                    it.copy(
                                        gpuOffload = option
                                    )
                                }
                            },
                            labelProvider = { labelsMap[it] ?: it },
                            modifier = Modifier.width(IntrinsicSize.Max)
                        )
                    }
                )
            }
        }
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_adv_launch_behavior_title),
                description = stringResource(Res.string.settings_adv_launch_behavior_title),
                trailingContent = {
                    Button(
                        content = { Text(stringResource(Res.string.settings_adv_launch_behavior_change)) },
                        onClick = {
                            showLaunchBehaviorDialog = true
                        }
                    )
                }
            )
        }
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_adv_controller_support_title),
                description = stringResource(Res.string.settings_adv_controller_support_desc),
                trailingContent = {
                    Checkbox(
                        checked = config.controllerSupport,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    controllerSupport = checked
                                )
                            }
                        }
                    )
                }
            )
        }
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_adv_refreshed_ui_title),
                description = stringResource(Res.string.settings_adv_refreshed_ui_desc),
                trailingContent = {
                    Checkbox(
                        checked = config.refreshedUI,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    refreshedUI = checked
                                )
                            }
                        }
                    )
                }
            )
        }
        item {
            SettingsItemV2(
                title = "Disable pixel styled UI (beta)",
                description = "Disables pixel styled UI",
                trailingContent = {
                    Checkbox(
                        checked = config.nonPixelUI,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    nonPixelUI = checked
                                )
                            }
                        }
                    )
                }
            )
        }
    }
}
