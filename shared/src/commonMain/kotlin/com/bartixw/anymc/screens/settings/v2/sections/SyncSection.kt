/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.v2.sections

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.settings_sync_bobby
import anymc.shared.generated.resources.settings_sync_command_history
import anymc.shared.generated.resources.settings_sync_config
import anymc.shared.generated.resources.settings_sync_distant_horizons
import anymc.shared.generated.resources.settings_sync_flashback
import anymc.shared.generated.resources.settings_sync_hotbar
import anymc.shared.generated.resources.settings_sync_journeymap
import anymc.shared.generated.resources.settings_sync_preferences
import anymc.shared.generated.resources.settings_sync_resourcepacks
import anymc.shared.generated.resources.settings_sync_saves
import anymc.shared.generated.resources.settings_sync_schmatic
import anymc.shared.generated.resources.settings_sync_screenshots
import anymc.shared.generated.resources.settings_sync_servers
import anymc.shared.generated.resources.settings_sync_shaderpacks
import anymc.shared.generated.resources.settings_sync_voxy
import anymc.shared.generated.resources.settings_sync_xareo
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.screens.settings.v2.SettingsItemV2
import com.bartixw.anymc.ui.components.v2.Checkbox
import org.jetbrains.compose.resources.stringResource

@Composable
fun SyncSection() {
    val config by ConfigManager.data.collectAsState()
    val scrollState = rememberScrollState()

    Row {
        Column(modifier = Modifier.verticalScroll(scrollState).weight(1f)) {
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_preferences),
                trailingContent = {
                    Checkbox(
                        checked = config.syncPreferences,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncPreferences = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_servers),
                trailingContent = {
                    Checkbox(
                        checked = config.syncServers,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncServers = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_command_history),
                trailingContent = {
                    Checkbox(
                        checked = config.syncCommandHistory,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncCommandHistory = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_hotbar),
                trailingContent = {
                    Checkbox(
                        checked = config.syncHotbar,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncHotbar = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_saves),
                trailingContent = {
                    Checkbox(
                        checked = config.syncSaves,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncSaves = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_config),
                trailingContent = {
                    Checkbox(
                        checked = config.syncConfig,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncConfig = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_screenshots),
                trailingContent = {
                    Checkbox(
                        checked = config.syncScreenshots,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncScreenshots = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_resourcepacks),
                trailingContent = {
                    Checkbox(
                        checked = config.syncResourcepacks,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncResourcepacks = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_shaderpacks),
                trailingContent = {
                    Checkbox(
                        checked = config.syncShaderpacks,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncShaderpacks = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_flashback),
                trailingContent = {
                    Checkbox(
                        checked = config.syncFlashback,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncFlashback = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_distant_horizons),
                trailingContent = {
                    Checkbox(
                        checked = config.syncDistantHorizons,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncDistantHorizons = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_voxy),
                trailingContent = {
                    Checkbox(
                        checked = config.syncVoxy,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncVoxy = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_xareo),
                trailingContent = {
                    Checkbox(
                        checked = config.syncXareo,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncXareo = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_journeymap),
                trailingContent = {
                    Checkbox(
                        checked = config.syncJourneymap,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncJourneymap = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_bobby),
                trailingContent = {
                    Checkbox(
                        checked = config.syncBobby,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncBobby = checked
                                )
                            }
                        }
                    )
                }
            )
            SettingsItemV2(
                title = stringResource(Res.string.settings_sync_schmatic),
                trailingContent = {
                    Checkbox(
                        checked = config.syncSchematic,
                        onCheckedChange = { checked ->
                            ConfigManager.update {
                                it.copy(
                                    syncSchematic = checked
                                )
                            }
                        }
                    )
                }
            )
        }
        VerticalScrollbar(rememberScrollbarAdapter(scrollState))
    }
}