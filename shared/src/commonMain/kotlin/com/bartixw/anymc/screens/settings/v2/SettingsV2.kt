/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.v2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.navigation.NavController
import androidx.compose.ui.unit.sp
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.settings_title
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.screens.settings.SettingsSection
import com.bartixw.anymc.screens.settings.v2.sections.AboutSectionV2
import com.bartixw.anymc.screens.settings.v2.sections.AdvancedSectionV2
import com.bartixw.anymc.screens.settings.v2.sections.AppearanceSectionV2
import com.bartixw.anymc.screens.settings.v2.sections.JavaPerformanceSectionV2
import com.bartixw.anymc.screens.settings.v2.sections.LinkSection
import com.bartixw.anymc.screens.settings.v2.sections.ServersSectionV2
import com.bartixw.anymc.screens.settings.v2.sections.SyncSection
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.SidebarItem
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.components.v2.TopAppBar
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SettingsScreenV2(navController: NavController) {
    var selectedSection by rememberSaveable(
        saver = Saver(
            save = { it.value.name },
            restore = { mutableStateOf(SettingsSection.valueOf(it)) }
        )
    ) { mutableStateOf(SettingsSection.APPEARANCE) }
    var isReady by remember { mutableStateOf(false) }

    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isReady) {
        if (isReady && ConfigManager.data.value.controllerSupport) {
            firstItemFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        delay(400.milliseconds)
        isReady = true
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            navController = navController,
            title = { Text(stringResource(Res.string.settings_title)) },
            actions = {}
        )
        if (!isReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else {
            Row {
                Column(
                    Modifier
                        .weight(1f),
                ) {
                    SettingsSection.entries.forEachIndexed { index, section ->
                        SidebarItem(
                            icon = {
                                Icon(
                                    resource = section.icon,
                                    contentDescription = null
                                )
                            },
                            title = { Text(stringResource(section.title)) },
                            description = {
                                section.description?.let {
                                    Text(
                                        stringResource(it),
                                        style = androidx.compose.ui.text.TextStyle(
                                            color = com.bartixw.anymc.ui.theme.MinecraftTheme.colors.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            },
                            selected = selectedSection == section,
                            onClick = { selectedSection = section }
                        )
                    }
                }
                Column(Modifier.weight(2f)) {
                    when (selectedSection) {
                        SettingsSection.APPEARANCE -> AppearanceSectionV2()
                        SettingsSection.JAVA_PERFORMANCE -> JavaPerformanceSectionV2()
                        //SettingsSection.SYNC -> SyncSection()
                        //SettingsSection.LINK -> LinkSection(navController)
                        SettingsSection.ADVANCED -> AdvancedSectionV2()
                        SettingsSection.ABOUT -> AboutSectionV2(navController)
                        //SettingsSection.SERVERS -> ServersSectionV2(navController)
                    }
                }
            }
        }
    }
}
