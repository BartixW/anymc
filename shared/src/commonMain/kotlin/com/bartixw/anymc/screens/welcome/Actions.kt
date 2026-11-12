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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.popups.NewAccountPopup
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McText
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeActionsScreen(navController: NavController) {
    val config by ConfigManager.data.collectAsState()
    var accountPopup by remember { mutableStateOf(false) }
    val accountManager by AccountManager.data.collectAsState()
    val accountAdded = accountManager.accounts.count() > 0

    NewAccountPopup(
        visible = accountPopup,
        onDismissRequest = { accountPopup = false }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            McText(stringResource(Res.string.welcome_actions_title))
            Spacer(Modifier.size(16.dp))
            McListItem(
                title = stringResource(Res.string.welcome_actions_addaccount_title),
                icon = if (accountAdded) Res.drawable.ic_check else Res.drawable.ic_profile,
                disabled = accountAdded,
                onClick = { accountPopup = true }
            )
        }
        Row {
            McButton(
                text = stringResource(Res.string.welcome_back),
                icon = Res.drawable.ic_chevron_left,
                onClick = { navController.popBackStack() }
            )
            McButton(
                text = stringResource(Res.string.welcome_done),
                icon = Res.drawable.ic_chevron_right,
                primary = true,
                disabled = !accountAdded,
                onClick = {
                    ConfigManager.update { it.copy(isNew = false)}
                    navController.navigate("main") {
                        popUpTo(0) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
