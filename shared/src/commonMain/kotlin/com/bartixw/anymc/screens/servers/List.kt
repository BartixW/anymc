/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.servers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_external
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McTopAppBar

@Composable
fun ServersListScreen(navController: NavController) {
    Column(Modifier.fillMaxSize()) {
        McTopAppBar(
            navController = navController,
            title = "Lista serwerów",
            actions = listOf(
                {
                    McButton(
                        text = "Otwórz panel zarządzania",
                        icon = Res.drawable.ic_external,
                        onClick = {}
                    )
                }
            )
        )
    }
}