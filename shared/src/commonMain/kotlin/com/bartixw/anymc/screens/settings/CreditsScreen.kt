/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.settings_about_credits_title
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McTopAppBar
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.components.v2.TopAppBar
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource

data class Credit(val name: String, val author: String, val license: String, val url: String)

val credits = listOf(
    Credit("FontStruction “F77 Minecraft”", "anonymous-1520403", "Creative Commons Attribution No Derivatives", "https://fontstruct.com/fontstructions/show/1500385")
)

@Composable
fun CreditsScreen(navController: NavController) {
    val uriHandler = LocalUriHandler.current

    Column {
        TopAppBar(
            navController = navController,
            title = { Text(stringResource(Res.string.settings_about_credits_title)) },
            actions = {}
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            credits.forEach { credit ->
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    baseModifier = Modifier.clickable {
                        uriHandler.openUri(credit.url)
                    }
                ) {
                    Column {
                        Text(text = credit.name)
                        Text(
                            text = "${credit.author} - ${credit.license}",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = MinecraftTheme.colors.textSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}
