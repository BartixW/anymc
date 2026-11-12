/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.v2.sections

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.settings_java_ram_usage_desc
import anymc.shared.generated.resources.settings_java_ram_usage_title
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.screens.settings.v2.SettingsItemV2
import com.bartixw.anymc.ui.components.v2.Input
import org.jetbrains.compose.resources.stringResource

@Composable
fun JavaPerformanceSectionV2() {
    val config by ConfigManager.data.collectAsState()

    LazyColumn {
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_java_ram_usage_title),
                description = stringResource(Res.string.settings_java_ram_usage_desc),
                trailingContent = {
                    Input(
                        value = config.maxRamUsage,
                        onValueChange = { ram ->
                            ConfigManager.update {
                                it.copy(
                                    maxRamUsage = ram
                                )
                            }
                        }
                    )
                }
            )
        }
    }
}
