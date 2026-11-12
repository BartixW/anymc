/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.v2.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.settings_section_appearance_language_auto
import anymc.shared.generated.resources.settings_section_appearance_language_en
import anymc.shared.generated.resources.settings_section_appearance_language_pl
import anymc.shared.generated.resources.settings_section_appearance_language_title
import anymc.shared.generated.resources.settings_section_appearance_theme_desc
import anymc.shared.generated.resources.settings_section_appearance_theme_title
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.screens.settings.v2.SettingsItemV2
import com.bartixw.anymc.ui.components.v2.Dropdown
import com.bartixw.anymc.ui.theme.ThemeManager
import org.jetbrains.compose.resources.stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.bartixw.anymc.ui.components.v2.*

@Composable
fun AppearanceSectionV2() {
    val config by ConfigManager.data.collectAsState()
    var dialogVisible by remember { mutableStateOf(false) }
    
    val themeOptions = remember {
        ThemeManager.getAvailableThemes()
    }

    val languageOptions = listOf("auto", "pl", "en")
    val languageLabels = mapOf(
        "auto" to stringResource(Res.string.settings_section_appearance_language_auto),
        "pl" to stringResource(Res.string.settings_section_appearance_language_pl),
        "en" to stringResource(Res.string.settings_section_appearance_language_en)
    )

    if (dialogVisible) Dialog(
        onDismissRequest = { dialogVisible = false },
        title = { Text("v2 Dialog") },
        buttons = {
            Button(
                content = { Text("OK") },
                onClick = { dialogVisible = false },
                type = ButtonType.Primary
            )
        }
    ) {
        Text("This is a v2 component showcase dialog.")
    }

    LazyColumn {
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_section_appearance_theme_title),
                description = stringResource(Res.string.settings_section_appearance_theme_desc, ThemeManager.themesDir),
                trailingContent = {
                    Dropdown(
                        options = themeOptions,
                        selectedOption = config.theme,
                        onOptionSelected = { name ->
                            ConfigManager.update {
                                it.copy(
                                    theme = name
                                )
                            }
                        },
                        modifier = Modifier.width(IntrinsicSize.Max)
                    )
                }
            )
        }
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_section_appearance_language_title),
                trailingContent = {
                    Dropdown(
                        options = languageOptions,
                        selectedOption = config.language,
                        onOptionSelected = { lang ->
                            ConfigManager.update {
                                it.copy(
                                    language = lang
                                )
                            }
                        },
                        labelProvider = { languageLabels[it] ?: it },
                        modifier = Modifier.width(IntrinsicSize.Max)
                    )
                }
            )
        }
        item {
            Text(
                "Example components:",
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, bottom = 8.dp)
            )
        }
        item {
            ListItem(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            content = { Text("HERO") },
                            type = ButtonType.Hero,
                            onClick = {}
                        )
                        Button(
                            content = { Text("Primary") },
                            type = ButtonType.Primary,
                            onClick = {}
                        )
                        Button(
                            content = { Text("Secondary") },
                            onClick = {}
                        )
                        Button(
                            content = { Text("Destructive") },
                            type = ButtonType.Destructive,
                            onClick = {}
                        )
                        Button(
                            content = { Text("Disabled") },
                            disabled = true,
                            onClick = {}
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = true, onCheckedChange = {})
                        Text("Checkbox Checked")
                        Checkbox(checked = false, onCheckedChange = {})
                        Text("Checkbox Unchecked")
                    }
                    Spacer(Modifier.size(16.dp))
                    Input(
                        value = "Sample Input",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LoadingIndicator()
                        Text("Loading...")
                        Button(
                            content = { Text("Show Dialog") },
                            onClick = { dialogVisible = true }
                        )
                    }
                }
            }
        }
    }
}
