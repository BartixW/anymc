/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_next
import anymc.shared.generated.resources.ic_chevron_right
import anymc.shared.generated.resources.ic_chevron_right_hero_17
import anymc.shared.generated.resources.ic_plus
import anymc.shared.generated.resources.ic_profile
import anymc.shared.generated.resources.welcome_description
import anymc.shared.generated.resources.welcome_feature_mods_description
import anymc.shared.generated.resources.welcome_feature_mods_title
import anymc.shared.generated.resources.welcome_feature_multiaccount_description
import anymc.shared.generated.resources.welcome_feature_multiaccount_title
import anymc.shared.generated.resources.welcome_title
import anymc.shared.generated.resources.welcomev2_call_to_action_button
import anymc.shared.generated.resources.welcomev2_title
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeScreenV2(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            text = stringResource(Res.string.welcomev2_title).uppercase(),
            style = MinecraftTheme.typographyAnyMC.copy(fontSize = 48.sp)
        )
        Button(
            content = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.welcomev2_call_to_action_button).uppercase())
                    Spacer(Modifier.width(16.dp))
                    Icon(Res.drawable.ic_chevron_right_hero_17, null)
                }
            },
            type = ButtonType.Hero,
            onClick = { navController.navigate("welcome/filesystem") }
        )
    }
}
