/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import anymc.shared.generated.resources.*
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun JavaPerformanceSection() {
    val config by ConfigManager.data.collectAsState()

    McListItem(
        title = stringResource(Res.string.settings_java_ram_usage_title),
        description = stringResource(Res.string.settings_java_ram_usage_desc),
        modifier = Modifier.heightIn(64.dp).fillMaxWidth(),
        trailingContent = {
            McTextField(
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
