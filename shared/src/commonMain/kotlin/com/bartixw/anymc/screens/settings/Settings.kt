/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.settings_title
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.screens.settings.sections.AboutSection
import com.bartixw.anymc.screens.settings.sections.AdvancedSection
import com.bartixw.anymc.screens.settings.sections.AppearanceSection
import com.bartixw.anymc.screens.settings.sections.JavaPerformanceSection
import com.bartixw.anymc.screens.settings.sections.ServersSection
import com.bartixw.anymc.screens.settings.v2.sections.LinkSection
import com.bartixw.anymc.screens.settings.v2.sections.SyncSection
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McLoadingIndicator
import com.bartixw.anymc.ui.components.McTopAppBar
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SettingsScreen(navController: NavController) {
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
        McTopAppBar(
            navController = navController,
            title = stringResource(Res.string.settings_title),
            actions = listOf()
        )
        if (!isReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                McLoadingIndicator()
            }
        } else {
            Row {
                Column(
                    Modifier
                        .weight(1f),
                ) {
                    SettingsSection.entries.forEachIndexed { index, section ->
                        McListItem(
                            title = stringResource(section.title),
                            description = section.description?.let { stringResource(it) },
                            icon = section.icon,
                            disabled = selectedSection == section,
                            onClick = { selectedSection = section },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(64.dp)
                                .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                        )
                    }
                }
                Column(Modifier.weight(2f)) {
                    when (selectedSection) {
                        SettingsSection.APPEARANCE -> AppearanceSection()
                        SettingsSection.JAVA_PERFORMANCE -> JavaPerformanceSection()
                        //SettingsSection.SYNC -> SyncSection()
                        //SettingsSection.LINK -> LinkSection(navController)
                        SettingsSection.ADVANCED -> AdvancedSection()
                        SettingsSection.ABOUT -> AboutSection(navController)
                        //SettingsSection.SERVERS -> ServersSection(navController)
                    }
                }
            }
        }
    }
}
