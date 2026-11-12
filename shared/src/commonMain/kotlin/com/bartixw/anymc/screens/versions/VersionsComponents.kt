/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.versions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import anymc.shared.generated.resources.*
import com.bartixw.anymc.minecraft.data.JavaVersion
import com.bartixw.anymc.minecraft.data.*
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McDialog
import com.bartixw.anymc.ui.components.McIcon
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.McTextField
import com.bartixw.anymc.utils.DateTimeFormatterUtil
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.ZoneId

@Composable
fun RenameInstallationDialog(
    currentName: String,
    onDismissRequest: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    McDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.versions_dialog_rename_title)
    ) {
        McText(stringResource(Res.string.versions_dialog_rename_label))
        Spacer(Modifier.size(16.dp))
        McTextField(
            value = newName,
            onValueChange = { newName = it }
        )
        Spacer(Modifier.size(32.dp))
        McButton(
            text = stringResource(Res.string.common_ok),
            icon = Res.drawable.ic_check,
            onClick = { onRename(newName) }
        )
    }
}

@Composable
fun InstallationSettingsDialog(
    initialJvmArgs: List<String>,
    initialRemovedArgs: List<String>,
    initialJavaVersion: JavaVersion?,
    onDismissRequest: () -> Unit,
    onSave: (List<String>, List<String>, JavaVersion?) -> Unit,
    viewModel: VersionsViewModel
) {
    var currentJvmArgs by remember(initialJvmArgs) { mutableStateOf(initialJvmArgs) }
    var currentRemovedArgs by remember(initialRemovedArgs) { mutableStateOf(initialRemovedArgs) }
    var currentJavaVersion by remember(initialJavaVersion) { mutableStateOf(initialJavaVersion) }

    var javaOptions by remember { mutableStateOf<List<JavaVersion?>>(listOf(null)) }
    var isLoadingRuntimes by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.getAvailableJavaRuntimes { runtimes ->
            javaOptions = listOf(null) + runtimes
            isLoadingRuntimes = false
        }
    }

    McDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.versions_settings_title)
    ) {
        Column(Modifier.height(400.dp).verticalScroll(rememberScrollState())) {
            McText(stringResource(Res.string.versions_dialog_settings_java_label))
            Spacer(Modifier.size(8.dp))
            
            if (isLoadingRuntimes) {
                McListItem(
                    title = stringResource(Res.string.versions_dialog_settings_java_loading),
                    description = stringResource(Res.string.versions_dialog_settings_java_loading_desc),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
            
            javaOptions.forEach { version ->
                val isSelected = currentJavaVersion?.component == version?.component
                McListItem(
                    title = version?.let { 
                        "Java ${it.majorVersion} (${it.component.substringAfterLast("-").replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }})"
                    } ?: stringResource(Res.string.versions_dialog_settings_java_default),
                    description = version?.let { "Komponent: ${it.component}, Wersja: ${it.majorVersion}" } ?: stringResource(Res.string.versions_dialog_settings_java_rec),
                    trailingContent = {
                        if (isSelected) {
                            McIcon(Res.drawable.ic_check, contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                    },
                    onClick = { currentJavaVersion = version },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }

            Spacer(Modifier.size(24.dp))

            McText(stringResource(Res.string.versions_dialog_settings_jvm_args_label))
            Spacer(Modifier.size(8.dp))
            currentJvmArgs.forEachIndexed { index, arg ->
                McListItem(
                    title = arg,
                    trailingContent = {
                        McButton(
                            icon = Res.drawable.ic_bin,
                            onClick = {
                                currentJvmArgs = currentJvmArgs.filterIndexed { i, _ -> i != index }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.size(8.dp))
            var newJvmArg by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                McTextField(
                    value = newJvmArg,
                    onValueChange = { newJvmArg = it },
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(Res.string.versions_dialog_settings_jvm_args_placeholder)
                )
                Spacer(Modifier.size(8.dp))
                McButton(
                    icon = Res.drawable.ic_plus,
                    onClick = {
                        if (newJvmArg.isNotBlank()) {
                            currentJvmArgs = currentJvmArgs + newJvmArg
                            newJvmArg = ""
                        }
                    }
                )
            }

            Spacer(Modifier.size(24.dp))

            McText(stringResource(Res.string.versions_dialog_settings_remove_args_label))
            Spacer(Modifier.size(8.dp))
            currentRemovedArgs.forEachIndexed { index, arg ->
                McListItem(
                    title = arg,
                    trailingContent = {
                        McButton(
                            icon = Res.drawable.ic_bin,
                            onClick = {
                                currentRemovedArgs = currentRemovedArgs.filterIndexed { i, _ -> i != index }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.size(8.dp))
            var newRemovedArg by remember { mutableStateOf("") }
            var showDemoError by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                McTextField(
                    value = newRemovedArg,
                    onValueChange = {
                        newRemovedArg = it
                        if (showDemoError) showDemoError = false
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(Res.string.versions_dialog_settings_remove_args_placeholder)
                )
                Spacer(Modifier.size(8.dp))
                McButton(
                    icon = Res.drawable.ic_plus,
                    onClick = {
                        if (newRemovedArg.trim() == "--demo") {
                            showDemoError = true
                        } else if (newRemovedArg.isNotBlank()) {
                            currentRemovedArgs = currentRemovedArgs + newRemovedArg.trim()
                            newRemovedArg = ""
                            showDemoError = false
                        }
                    }
                )
            }
            if (showDemoError) {
                Spacer(Modifier.size(8.dp))
                McText(
                    text = stringResource(Res.string.versions_dialog_settings_demo_error),
                    style = TextStyle(color = Color(0xFFFF5555))
                )
            }
        }

        Spacer(Modifier.size(32.dp))
        McButton(
            text = stringResource(Res.string.nbt_save),
            icon = Res.drawable.ic_check,
            onClick = { onSave(currentJvmArgs, currentRemovedArgs, currentJavaVersion) }
        )
    }
}

@Composable
fun InstallationListItem(
    installation: Installation,
    isSelected: Boolean,
    minecraftStatus: MinecraftStatus,
    isUpdating: Boolean = false,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onOpenFolder: () -> Unit,
    onSettings: () -> Unit,
    onNbtEditor: () -> Unit,
    onRestoreBackup: () -> Unit,
    devMode: Boolean = false
) {
    var moreOptionsExpanded by remember { mutableStateOf(false) }
    var deleteDisabled by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 64.dp)
    ) {
        McListItem(
            title = installation.name + if (isUpdating) " (Updating...)" else "",
            description = "${installation.version} (${installation.modLoader.replaceFirstChar { it.uppercase() }})",
            onClick = {
                if (isUpdating ||
                    minecraftStatus == MinecraftStatus.LAUNCHING ||
                    minecraftStatus == MinecraftStatus.KILLING ||
                    minecraftStatus == MinecraftStatus.LAUNCHED
                ) return@McListItem
                onSelect()
            },
            disabled = isSelected || isUpdating,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        McButton(
            icon = Res.drawable.ic_more_options,
            onClick = { moreOptionsExpanded = true },
            modifier = Modifier.fillMaxHeight()
        )
        if (moreOptionsExpanded) Popup(
            alignment = Alignment.BottomEnd,
            onDismissRequest = { moreOptionsExpanded = false },
        ) {
            Column(Modifier.width(IntrinsicSize.Max)) {
                McListItem(
                    title = stringResource(Res.string.versions_menu_restore_backup),
                    icon = Res.drawable.ic_reload,
                    onClick = {
                        moreOptionsExpanded = false
                        onRestoreBackup()
                    }
                )
                McListItem(
                    title = stringResource(Res.string.versions_menu_delete),
                    icon = Res.drawable.ic_bin,
                    disabled = deleteDisabled,
                    onClick = {
                        deleteDisabled = true
                        onDelete()
                    }
                )
                McListItem(
                    title = stringResource(Res.string.versions_menu_rename),
                    icon = Res.drawable.ic_edit,
                    onClick = {
                        moreOptionsExpanded = false
                        onRename()
                    }
                )
                McListItem(
                    title = stringResource(Res.string.versions_menu_open_folder),
                    icon = Res.drawable.ic_directory,
                    onClick = {
                        moreOptionsExpanded = false
                        onOpenFolder()
                    }
                )
                McListItem(
                    title = stringResource(Res.string.versions_menu_settings),
                    icon = Res.drawable.ic_settings,
                    onClick = {
                        moreOptionsExpanded = false
                        onSettings()
                    }
                )
                if (devMode) {
                    McListItem(
                        title = stringResource(Res.string.versions_dev_resource_menu),
                        icon = Res.drawable.ic_dev
                    )
                    McListItem(
                        title = stringResource(Res.string.versions_menu_nbt),
                        icon = Res.drawable.ic_dev,
                        onClick = {
                            moreOptionsExpanded = false
                            onNbtEditor()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WorldListItem(
    worldItem: VersionsViewModel.WorldItem,
    iconPath: String,
    minecraftStatus: MinecraftStatus,
    showIds: Boolean = false,
    onLaunch: () -> Unit
) {
    val gameModeStr = when (worldItem.gameType) {
        0 -> stringResource(Res.string.versions_gamemode_survival)
        1 -> stringResource(Res.string.versions_gamemode_creative)
        2 -> stringResource(Res.string.versions_gamemode_adventure)
        3 -> stringResource(Res.string.versions_gamemode_spectator)
        else -> stringResource(Res.string.versions_gamemode_unknown)
    }
    
    val lastPlayedStr = DateTimeFormatterUtil.formatDateTime(
        Instant.ofEpochMilli(worldItem.lastPlayed)
            .atZone(ZoneId.systemDefault())
    )
    
    val description = StringBuilder().apply {
        append(stringResource(Res.string.versions_world_gamemode_label, gameModeStr))
        append("\n")
        if (showIds) {
            append(stringResource(Res.string.versions_world_id_label, worldItem.id))
            append("\n")
        }
        append(stringResource(Res.string.versions_world_last_played_label, lastPlayedStr))
    }.toString()

    Row(
        Modifier.height(IntrinsicSize.Max)
    ) {
        McListItem(
            title = worldItem.name,
            iconBitmap = rememberImageBitmap(iconPath),
            description = description,
            modifier = Modifier.weight(1f)
        )
        McButton(
            icon = Res.drawable.ic_power,
            disabled = minecraftStatus == MinecraftStatus.LAUNCHING ||
                    minecraftStatus == MinecraftStatus.KILLING ||
                    minecraftStatus == MinecraftStatus.LAUNCHED,
            onClick = onLaunch,
            modifier = Modifier.fillMaxHeight()
        )
    }
}
