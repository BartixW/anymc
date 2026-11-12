/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.v2.sections

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.screens.settings.v2.SettingsItemV2
import com.bartixw.anymc.ui.components.v2.Icon
import org.jetbrains.compose.resources.stringResource

@Composable
fun ServersSectionV2(navController: NavController) {
    LazyColumn {
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_servers_tailscale_account_title),
                description = stringResource(Res.string.settings_servers_tailscale_account_desc, stringResource(Res.string.settings_servers_tailscale_account_logged_out)),
                icon = {
                    Icon(
                        resource = Res.drawable.ic_tailscale,
                        contentDescription = null
                    )
                }
            )
        }
    }
}
