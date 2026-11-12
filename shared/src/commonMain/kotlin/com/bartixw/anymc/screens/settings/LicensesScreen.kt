/*
 * Copyright (C) 2024 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.settings_about_licenses
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McTopAppBar
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.components.v2.TopAppBar
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource

data class LicenseInfo(val name: String, val author: String, val license: String)

val licenses = listOf(
    LicenseInfo("AnyMC Launcher", "BartixW", "GNU GPL v3.0"),
    LicenseInfo("AnyMC AuthHelper", "BartixW", "MIT License"),
    LicenseInfo("Compose Multiplatform", "JetBrains", "Apache License 2.0"),
    LicenseInfo("Kotlin Standard Library", "JetBrains", "Apache License 2.0"),
    LicenseInfo("Kotlinx Coroutines", "JetBrains", "Apache License 2.0"),
    LicenseInfo("Kotlinx Serialization", "JetBrains", "Apache License 2.0"),
    LicenseInfo("Ktor Client", "JetBrains", "Apache License 2.0"),
    LicenseInfo("Jetpack Navigation Compose", "JetBrains / Google", "Apache License 2.0"),
    LicenseInfo("AndroidX Lifecycle", "Google", "Apache License 2.0"),
    LicenseInfo("Multiplatform Markdown Renderer", "Mike Penz", "Apache License 2.0"),
    LicenseInfo("NBT", "ViaVersion", "MIT License"),
    LicenseInfo("FileKit", "vinceglb", "MIT License"),
    LicenseInfo("jMonkeyEngine", "jMonkeyEngine Team", "New BSD License"),
    LicenseInfo("LWJGL (Lightweight Java Game Library)", "LWJGL Organization", "BSD 3-Clause License"),
    LicenseInfo("Avalonia UI", "AvaloniaUI OU", "MIT License"),
    LicenseInfo("Avalonia.Controls.WebView", "AvaloniaUI OU", "MIT License"),
    LicenseInfo("Avalonia.Themes.Fluent", "AvaloniaUI OU", "MIT License"),
    LicenseInfo("Avalonia.Fonts.Inter", "AvaloniaUI OU", "MIT License"),
    LicenseInfo("StaticLink.Avalonia", "AvaloniaUI OU", "MIT License"),
    LicenseInfo("SkiaSharp", "Microsoft / Mono Project", "MIT License"),
    LicenseInfo("HarfBuzzSharp", "Microsoft / Mono Project", "MIT License")
)

@Composable
fun LicensesScreen(navController: NavController) {
    Column {
        TopAppBar(
            navController = navController,
            title = { Text(stringResource(Res.string.settings_about_licenses)) },
            actions = {}
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            licenses.forEach { license ->
                ListItem(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(text = license.name)
                        Text(
                            text = "${license.author} - ${license.license}",
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
