/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import com.bartixw.anymc.dataclasses.LauncherConfig
import com.bartixw.anymc.managers.ConfigManager
import anymc.shared.generated.resources.settings_adv_gpu_offload_amd
import anymc.shared.generated.resources.settings_adv_gpu_offload_nvidia
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McCheckbox
import com.bartixw.anymc.ui.components.McDialog
import com.bartixw.anymc.ui.components.McDropdown
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McText
import org.jetbrains.compose.resources.stringResource

@Composable
fun AdvancedSection() {
    val config by ConfigManager.data.collectAsState()
    var showLaunchBehaviorDialog by remember { mutableStateOf(false) }

    if (showLaunchBehaviorDialog) McDialog(
        title = stringResource(Res.string.settings_adv_launch_behavior_title),
        onDismissRequest = {
            showLaunchBehaviorDialog = false
        },
        buttons =
            {
                McButton(
                    text = stringResource(Res.string.common_ok),
                    icon = Res.drawable.ic_check,
                    onClick = {
                        showLaunchBehaviorDialog = false
                    }
                )
            }
    ) {
        Column (verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row (verticalAlignment = Alignment.CenterVertically) {
                McCheckbox(
                    checked = config.minimizeOnLaunch,
                    onCheckedChange = { checked ->
                        ConfigManager.update {
                            it.copy(
                                minimizeOnLaunch = checked
                            )
                        }
                    }
                )
                Spacer(Modifier.size(8.dp))
                McText(stringResource(Res.string.settings_adv_launch_minimize))
            }
            Row (verticalAlignment = Alignment.CenterVertically) {
                McCheckbox(
                    checked = config.showConsoleOnLaunch,
                    onCheckedChange = { checked ->
                        ConfigManager.update {
                            it.copy(
                                showConsoleOnLaunch = checked
                            )
                        }
                    }
                )
                Spacer(Modifier.size(8.dp))
                McText(stringResource(Res.string.settings_adv_launch_console))
            }
        }
    }
    
    McListItem(
        title = stringResource(Res.string.settings_adv_dev_mode_title),
        description = stringResource(Res.string.settings_adv_dev_mode_desc),
        icon = Res.drawable.ic_dev,
        trailingContent = {
            McCheckbox(
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
    if (PlatformUtils.getCurrentOS() == "linux") {
        val labelsMap = mapOf(
            "none" to stringResource(Res.string.common_none),
            "nvidia" to stringResource(Res.string.settings_adv_gpu_offload_nvidia),
            "amd" to stringResource(Res.string.settings_adv_gpu_offload_amd)
        )

        McListItem(
            title = stringResource(Res.string.settings_adv_gpu_offload_title),
            description = stringResource(Res.string.settings_adv_gpu_offload_desc),
            icon = Res.drawable.ic_gpu_offloading,
            trailingContent = {
                McDropdown(
                    options = listOf("none", "nvidia", "amd"),
                    selectedOption = config.gpuOffload,
                    onOptionSelected = { option ->
                        ConfigManager.update {
                            it.copy(
                                gpuOffload = option
                            )
                        }
                    },
                    labelProvider = { labelsMap[it] ?: it }
                )
            }
        )
    }
    McListItem(
        title = stringResource(Res.string.settings_adv_launch_behavior_title),
        description = stringResource(Res.string.settings_adv_launch_behavior_title), // description was same as title or similar
        icon = Res.drawable.ic_power,
        trailingContent = {
            McButton(
                text = stringResource(Res.string.settings_adv_launch_behavior_change),
                icon = Res.drawable.ic_external,
                onClick = {
                    showLaunchBehaviorDialog = true
                }
            )
        }
    )
    McListItem(
        title = stringResource(Res.string.settings_adv_controller_support_title),
        description = stringResource(Res.string.settings_adv_controller_support_desc),
        icon = Res.drawable.ic_gamepad,
        trailingContent = {
            McCheckbox(
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
    McListItem(
        title = stringResource(Res.string.settings_adv_refreshed_ui_title),
        description = stringResource(Res.string.settings_adv_refreshed_ui_desc),
        icon = Res.drawable.ic_theme,
        trailingContent = {
            McCheckbox(
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
