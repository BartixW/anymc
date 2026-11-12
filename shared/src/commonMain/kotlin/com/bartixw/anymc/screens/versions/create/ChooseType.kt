/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.versions.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.minecraft.data.InstallationType
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McTopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
fun VersionsCreateChooseTypeScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        McTopAppBar(
            navController = navController,
            title = stringResource(Res.string.versions_create_type_title),
            actions = listOf()
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            McListItem(
                title = "Vanilla",
                icon = Res.drawable.ic_chevron_right,
                onClick = {
                    navController.navigate(ChooseVersionRoute(InstallationType.VANILLA.toString()))
                }
            )
            McListItem(
                title = "Fabric",
                icon = Res.drawable.ic_chevron_right,
                onClick = {
                    navController.navigate(ChooseVersionRoute(InstallationType.FABRICMC.toString()))
                }
            )
            McListItem(
                title = "Legacy Fabric",
                icon = Res.drawable.ic_chevron_right,
                onClick = {
                    navController.navigate(ChooseVersionRoute(InstallationType.LEGACYFABRIC.toString()))
                }
            )
            McListItem(
                title = "Forge",
                icon = Res.drawable.ic_chevron_right,
                onClick = {
                    navController.navigate(ChooseVersionRoute(InstallationType.FORGE.toString()))
                }
            )
            McListItem(
                title = "NeoForge",
                icon = Res.drawable.ic_chevron_right,
                onClick = {
                    navController.navigate(ChooseVersionRoute(InstallationType.NEOFORGE.toString()))
                }
            )
            McListItem(
                title = stringResource(Res.string.versions_create_modpack),
                icon = Res.drawable.ic_chevron_right,
                onClick = {
                    navController.navigate("versions/create/modpack")
                }
            )
        }
    }
}
