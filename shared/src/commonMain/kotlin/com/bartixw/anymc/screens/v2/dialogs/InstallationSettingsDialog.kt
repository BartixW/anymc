/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.v2.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.sp
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_bin
import anymc.shared.generated.resources.ic_check
import anymc.shared.generated.resources.ic_plus
import anymc.shared.generated.resources.nbt_cancel
import anymc.shared.generated.resources.nbt_save
import anymc.shared.generated.resources.versions_dialog_settings_demo_error
import anymc.shared.generated.resources.versions_dialog_settings_java_default
import anymc.shared.generated.resources.versions_dialog_settings_java_label
import anymc.shared.generated.resources.versions_dialog_settings_java_loading
import anymc.shared.generated.resources.versions_dialog_settings_java_loading_desc
import anymc.shared.generated.resources.versions_dialog_settings_java_rec
import anymc.shared.generated.resources.versions_dialog_settings_jvm_args_label
import anymc.shared.generated.resources.versions_dialog_settings_jvm_args_placeholder
import anymc.shared.generated.resources.versions_dialog_settings_remove_args_label
import anymc.shared.generated.resources.versions_dialog_settings_remove_args_placeholder
import anymc.shared.generated.resources.versions_settings_title
import com.bartixw.anymc.minecraft.data.JavaVersion
import com.bartixw.anymc.screens.versions.VersionsViewModel
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.Input
import com.bartixw.anymc.ui.components.v2.ListItem
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource

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

    Dialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.versions_settings_title)) },
        buttons = {
            Button(
                content = { Text(stringResource(Res.string.nbt_cancel)) },
                onClick = onDismissRequest
            )
            Button(
                content = { Text(stringResource(Res.string.nbt_save)) },
                type = ButtonType.Primary,
                onClick = { onSave(currentJvmArgs, currentRemovedArgs, currentJavaVersion) }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .height(400.dp)
                .verticalScroll(rememberScrollState())
                .padding(4.dp)
        ) {
            Text(stringResource(Res.string.versions_dialog_settings_java_label))
            Spacer(Modifier.size(8.dp))

            if (isLoadingRuntimes) {
                ListItem(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    baseModifier = Modifier
                ) {
                    Column {
                        Text(stringResource(Res.string.versions_dialog_settings_java_loading))
                        Text(
                            stringResource(Res.string.versions_dialog_settings_java_loading_desc),
                            style = TextStyle(fontSize = 12.sp, color = MinecraftTheme.colors.textSecondary)
                        )
                    }
                }
            }

            javaOptions.forEach { version ->
                val isSelected = currentJavaVersion?.component == version?.component
                ListItem(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    baseModifier = Modifier.clickable { currentJavaVersion = version }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = version?.let {
                                    "Java ${it.majorVersion} (${it.component.substringAfterLast("-").replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }})"
                                } ?: stringResource(Res.string.versions_dialog_settings_java_default)
                            )
                            Text(
                                text = version?.let { "Komponent: ${it.component}, Wersja: ${it.majorVersion}" } ?: stringResource(Res.string.versions_dialog_settings_java_rec),
                                style = TextStyle(fontSize = 12.sp, color = MinecraftTheme.colors.textSecondary)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                resource = Res.drawable.ic_check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.size(24.dp))

            Text(stringResource(Res.string.versions_dialog_settings_jvm_args_label))
            Spacer(Modifier.size(8.dp))
            currentJvmArgs.forEachIndexed { index, arg ->
                ListItem(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    baseModifier = Modifier
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = arg,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            content = {
                                Icon(
                                    resource = Res.drawable.ic_bin,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                currentJvmArgs = currentJvmArgs.filterIndexed { i, _ -> i != index }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            var newJvmArg by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Input(
                    value = newJvmArg,
                    onValueChange = { newJvmArg = it },
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(Res.string.versions_dialog_settings_jvm_args_placeholder)
                )
                Spacer(Modifier.size(8.dp))
                Button(
                    content = {
                        Icon(
                            resource = Res.drawable.ic_plus,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    type = ButtonType.Primary,
                    onClick = {
                        if (newJvmArg.isNotBlank()) {
                            currentJvmArgs = currentJvmArgs + newJvmArg
                            newJvmArg = ""
                        }
                    }
                )
            }

            Spacer(Modifier.size(24.dp))

            Text(stringResource(Res.string.versions_dialog_settings_remove_args_label))
            Spacer(Modifier.size(8.dp))
            currentRemovedArgs.forEachIndexed { index, arg ->
                ListItem(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    baseModifier = Modifier
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = arg,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            content = {
                                Icon(
                                    resource = Res.drawable.ic_bin,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MinecraftTheme.colors.buttonSecondaryText
                                )
                            },
                            onClick = {
                                currentRemovedArgs = currentRemovedArgs.filterIndexed { i, _ -> i != index }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            var newRemovedArg by remember { mutableStateOf("") }
            var showDemoError by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Input(
                    value = newRemovedArg,
                    onValueChange = {
                        newRemovedArg = it
                        if (showDemoError) showDemoError = false
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(Res.string.versions_dialog_settings_remove_args_placeholder)
                )
                Spacer(Modifier.size(8.dp))
                Button(
                    content = {
                        Icon(
                            resource = Res.drawable.ic_plus,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    type = ButtonType.Primary,
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
                Text(
                    text = stringResource(Res.string.versions_dialog_settings_demo_error),
                    style = TextStyle(color = Color(0xFFFF5555))
                )
            }
        }
    }
}
