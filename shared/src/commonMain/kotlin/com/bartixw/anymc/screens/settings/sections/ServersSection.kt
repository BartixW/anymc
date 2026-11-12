/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.ui.components.McIcon
import com.bartixw.anymc.ui.components.McListItem
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ServersSection(navController: NavController) {
    McListItem(
        title = stringResource(Res.string.settings_servers_tailscale_account_title),
        description = stringResource(Res.string.settings_servers_tailscale_account_desc, stringResource(Res.string.settings_servers_tailscale_account_logged_out)),
        icon = Res.drawable.ic_tailscale
    )
}
