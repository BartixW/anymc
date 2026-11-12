/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.welcome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.minecraft.data.DownloadStatus
import com.bartixw.anymc.minecraft.data.StatusType
import com.bartixw.anymc.screens.v2.dialogs.NewAccountDialog
import com.bartixw.anymc.screens.v2.dialogs.NewInstallationDialog
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun WelcomeActionsScreenV2(navController: NavController) {
    val config by ConfigManager.data.collectAsState()
    var accountPopup by remember { mutableStateOf(false) }
    var installationPopup by remember { mutableStateOf(false) }
    val accountManager by AccountManager.data.collectAsState()
    val accountAdded = accountManager.accounts.count() > 0
    val downloadStatus by MinecraftManager.downloadStatus.collectAsState()
    var installTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(downloadStatus.type) {
        if (downloadStatus.type != StatusType.IDLE && !installTriggered) {
            installTriggered = true
            ConfigManager.update { it.copy(isNew = false)}
            navController.navigate("welcome/installing") {
                popUpTo(0) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    if (accountPopup) NewAccountDialog(
        onDismissRequest = { accountPopup = false }
    )

    if (installationPopup) NewInstallationDialog(
        onDismissRequest = { installationPopup = false },
        navController = navController
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = stringResource(Res.string.welcome_actions_title),
                style = MinecraftTheme.typographyPixel
            )
            Spacer(Modifier.size(16.dp))
            ListItem(
                content = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Icon(if (accountAdded) Res.drawable.ic_check else Res.drawable.ic_profile_add, null, Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.welcome_actions_addaccount_title))
                        }
                    }

                    if (!accountAdded) Icon(Res.drawable.ic_chevron_right, null, Modifier.size(32.dp))
                },
                modifier = Modifier
                    .fillMaxWidth(),
                baseModifier = if (accountAdded) Modifier else Modifier.clickable {
                    accountPopup = true
                }
            )
            ListItem(
                content = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Icon(Res.drawable.ic_download, null, Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(Res.string.welcome_actions_createinstallation))
                        }
                    }

                    Icon(Res.drawable.ic_chevron_right, null, Modifier.size(32.dp))
                },
                modifier = Modifier
                    .fillMaxWidth(),
                baseModifier = Modifier
                    .clickable {
                        installationPopup = true
                    }
            )
        }
    }
}
