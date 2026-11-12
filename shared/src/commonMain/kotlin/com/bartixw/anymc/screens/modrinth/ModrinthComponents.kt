/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.modrinth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import anymc.shared.generated.resources.*
import com.bartixw.anymc.minecraft.data.modrinth.ModrinthDependency
import com.bartixw.anymc.ui.components.McIcon
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.DateTimeFormatterUtil
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SidebarSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(MinecraftTheme.colors.controlBackground.copy(alpha = 0.3f))
            .border(2.dp, MinecraftTheme.colors.controlBorder)
            .padding(12.dp)
    ) {
        Text(text = title, style = MinecraftTheme.typographyPixel.copy(fontSize = 16.sp))
        Spacer(Modifier.size(8.dp))
        content()
    }
}

@Composable
fun Tag(text: String) {
    Box(
        modifier = Modifier
            .background(MinecraftTheme.colors.controlBackground)
            .border(1.dp, MinecraftTheme.colors.controlBorder)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MinecraftTheme.typographyPixel.copy(fontSize = 11.sp))
    }
}

@Composable
fun CategoryTag(text: String) {
    Box(
        modifier = Modifier
            .background(MinecraftTheme.colors.controlBackground.copy(alpha = 0.5f))
            .border(1.dp, MinecraftTheme.colors.textSecondary)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = text, style = MinecraftTheme.typographyPixel.copy(fontSize = 10.sp, color = MinecraftTheme.colors.textSecondary))
    }
}

@Composable
fun ExternalLink(label: String, url: String, icon: DrawableResource = Res.drawable.ic_external) {
    val uriHandler = LocalUriHandler.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(resource = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Cyan)
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            style = MinecraftTheme.typography.copy(fontSize = 12.sp, color = Color.Cyan),
            modifier = Modifier.clickable { uriHandler.openUri(url) }
        )
    }
}

@Composable
fun DetailItem(icon: DrawableResource, text: String, url: String? = null) {
    val uriHandler = LocalUriHandler.current

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(resource = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MinecraftTheme.colors.textSecondary)
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
            style = MinecraftTheme.typographyPixel.copy(
                fontSize = 12.sp,
                color = if (url != null) Color.Cyan else MinecraftTheme.colors.textSecondary),
            modifier = if (url != null) Modifier.clickable { uriHandler.openUri(url) } else Modifier
        )
    }
}

@Composable
fun String.translateEnvironment(): String = when (this) {
    "required" -> stringResource(Res.string.modrinth_env_required)
    "optional" -> stringResource(Res.string.modrinth_env_optional)
    "unsupported" -> stringResource(Res.string.modrinth_env_unsupported)
    else -> this
}

fun String.formatModrinthDate(): String {
    return DateTimeFormatterUtil.formatDate(LocalDate.parse(this.take(10)))
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        McText(text = label, style = MinecraftTheme.typography.copy(fontSize = 12.sp, color = MinecraftTheme.colors.textSecondary))
        McText(text = value, style = MinecraftTheme.typography.copy(fontSize = 16.sp))
    }
}

@Composable
fun ModrinthDependency.formatReadableVersion(): String {
    val fn = fileName
    val vId = versionId
    if (fn != null) {
        // Try to extract version number from filename like modname-1.2.3.jar
        val nameWithoutExt = fn!!.substringBeforeLast(".")
        val parts = nameWithoutExt.split("-")
        return parts.lastOrNull { it.firstOrNull()?.isDigit() == true } ?: nameWithoutExt
    }
    if (vId != null && vId!!.length > 8) {
        return "ID: ${vId!!.take(8)}"
    }
    return stringResource(Res.string.modrinth_dependency_any)
}
