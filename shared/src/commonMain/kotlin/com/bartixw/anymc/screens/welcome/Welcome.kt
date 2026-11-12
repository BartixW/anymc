/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_next
import anymc.shared.generated.resources.ic_chevron_right
import anymc.shared.generated.resources.ic_plus
import anymc.shared.generated.resources.ic_profile
import anymc.shared.generated.resources.welcome_description
import anymc.shared.generated.resources.welcome_feature_mods_description
import anymc.shared.generated.resources.welcome_feature_mods_title
import anymc.shared.generated.resources.welcome_feature_multiaccount_description
import anymc.shared.generated.resources.welcome_feature_multiaccount_title
import anymc.shared.generated.resources.welcome_title
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McText
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            McText(stringResource(Res.string.welcome_title))
            Spacer(Modifier.size(32.dp))
            McText(stringResource(Res.string.welcome_description))
            Spacer(Modifier.size(16.dp))
            McListItem(
                title = stringResource(Res.string.welcome_feature_mods_title),
                description = stringResource(Res.string.welcome_feature_mods_description),
                icon = Res.drawable.ic_plus
            )
            McListItem(
                title = stringResource(Res.string.welcome_feature_multiaccount_title),
                description = stringResource(Res.string.welcome_feature_multiaccount_description),
                icon = Res.drawable.ic_profile
            )
        }
        McButton(
            text = stringResource(Res.string.common_next),
            icon = Res.drawable.ic_chevron_right,
            primary = true,
            onClick = { navController.navigate("welcome/filesystem") }
        )
    }
}
