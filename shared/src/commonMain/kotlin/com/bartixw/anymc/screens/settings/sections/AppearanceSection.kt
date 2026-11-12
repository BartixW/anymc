/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_check
import anymc.shared.generated.resources.ic_chevron_right
import anymc.shared.generated.resources.ic_external
import anymc.shared.generated.resources.ic_settings
import anymc.shared.generated.resources.settings_section_appearance_language_auto
import anymc.shared.generated.resources.settings_section_appearance_language_en
import anymc.shared.generated.resources.settings_section_appearance_language_pl
import anymc.shared.generated.resources.settings_section_appearance_language_title
import anymc.shared.generated.resources.settings_section_appearance_theme_desc
import anymc.shared.generated.resources.settings_section_appearance_theme_title
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McCheckbox
import com.bartixw.anymc.ui.components.McDialog
import com.bartixw.anymc.ui.components.McDropdown
import com.bartixw.anymc.ui.components.McIcon
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McLoadingIndicator
import com.bartixw.anymc.ui.components.McSlider
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.McTextField
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.ui.theme.ThemeManager
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppearanceSection() {
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

    LazyColumn {
        item {
            McListItem(
                title = stringResource(Res.string.settings_section_appearance_theme_title),
                description = stringResource(Res.string.settings_section_appearance_theme_desc, ThemeManager.themesDir),
                modifier = Modifier.heightIn(64.dp).fillMaxWidth(),
                trailingContent = {
                    McDropdown(
                        options = themeOptions,
                        selectedOption = config.theme,
                        onOptionSelected = { name ->
                            ConfigManager.update {
                                it.copy(
                                    theme = name
                                )
                            }
                        }
                    )
                }
            )
        }
        item {
            McListItem(
                title = stringResource(Res.string.settings_section_appearance_language_title),
                modifier = Modifier.heightIn(64.dp).fillMaxWidth(),
                trailingContent = {
                    McDropdown(
                        options = languageOptions,
                        selectedOption = config.language,
                        onOptionSelected = { lang ->
                            ConfigManager.update {
                                it.copy(
                                    language = lang
                                )
                            }
                        },
                        labelProvider = { languageLabels[it] ?: it }
                    )
                }
            )
        }
        item {
            McText(
                "Przykładowe komponenty:",
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, bottom = 8.dp)
            )
        }
        item {
            Row(
                modifier = Modifier
                    .background(MinecraftTheme.colors.controlBackground)
                    .border(2.dp, MinecraftTheme.colors.controlBorder)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McButton(
                        text = "Przycisk",
                        icon = Res.drawable.ic_chevron_right,
                        onClick = {}
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McButton(
                        text = "Główny przycisk",
                        icon = Res.drawable.ic_chevron_right,
                        primary = true,
                        onClick = {}
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McButton(
                        text = "Wył. przycisk",
                        icon = Res.drawable.ic_chevron_right,
                        disabled = true,
                        onClick = {}
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .background(MinecraftTheme.colors.controlBackground)
                    .border(2.dp, MinecraftTheme.colors.controlBorder)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McDropdown(
                        options = listOf(
                            "Opcja 1",
                            "Opcja 2",
                            "Opcja 3"
                        ),
                        selectedOption = "Opcja 1",
                        onOptionSelected = {}
                    )
                    Spacer(Modifier.size(8.dp))
                    McText("Dropdown")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McText("Przykładowy\ntekst.")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McIcon(
                        resource = Res.drawable.ic_settings,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    McText("Ikona")
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .background(MinecraftTheme.colors.controlBackground)
                    .border(2.dp, MinecraftTheme.colors.controlBorder)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McTextField(
                        value = "Wpisany tekst",
                        onValueChange = {}
                    )
                    Spacer(Modifier.size(8.dp))
                    McText("Pole tekstowe")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McTextField(
                        value = "",
                        placeholder = "Placeholder",
                        onValueChange = {}
                    )
                    Spacer(Modifier.size(8.dp))
                    McText("Puste pole tekstowe")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row {
                        McCheckbox(
                            checked = false,
                            onCheckedChange = {}
                        )
                        Spacer(Modifier.size(16.dp))
                        McCheckbox(
                            checked = true,
                            onCheckedChange = {}
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    McText("Checkbox")
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .background(MinecraftTheme.colors.controlBackground)
                    .border(2.dp, MinecraftTheme.colors.controlBorder)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McLoadingIndicator()
                    Spacer(Modifier.size(8.dp))
                    McText("Wskaźnik ładowania")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McSlider(
                        value = 0.5f,
                        onValueChange = {}
                    )
                    Spacer(Modifier.size(8.dp))
                    McText("Suwak")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    McButton(
                        text = "Wyświetl dialog",
                        icon = Res.drawable.ic_external,
                        onClick = {
                            dialogVisible = true
                        }
                    )
                }
            }
        }
    }

    if (dialogVisible) McDialog(
        onDismissRequest = {
            dialogVisible = false
        },
        title = "Przykładowy dialog",
        buttons = {
            McButton(
                icon = Res.drawable.ic_check,
                text = "OK",
                onClick = {
                    dialogVisible = false
                }
            )
        }
    ) {
        McText("Zawartość dialogu")
    }
}
