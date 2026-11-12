/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.v2.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.core.system.SystemRegistryManager
import com.bartixw.anymc.core.updater.UpdateExecutor
import com.bartixw.anymc.core.updater.UpdateInfo
import com.bartixw.anymc.core.updater.UpdateManager
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.managers.ConfigManager
import com.bartixw.anymc.screens.settings.v2.SettingsItemV2
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Checkbox
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.Text
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.system.exitProcess

@Composable
fun AboutSectionV2(navController: NavController) {
    val imageBitmap = imageResource(Res.drawable.app_icon)
    val coroutineScope = rememberCoroutineScope()

    val config by ConfigManager.data.collectAsState(ConfigManager.data.value)
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var noUpdateFound by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var updateCompleted by remember { mutableStateOf(false) }
    var preparedExtractedDir by remember { mutableStateOf<File?>(null) }
    var showUninstallDialog by remember { mutableStateOf(false) }
    var lastManualCheckTime by remember { mutableStateOf(0L) }
    var rateLimitHit by remember { mutableStateOf(false) }

    LazyColumn {
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_section_about_title),
                icon = {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.size(32.dp)
                    )
                },
                description = stringResource(Res.string.settings_about_launcher_desc)
            )
        }
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_about_check_updates),
                description = if (isChecking) "Checking for updates..." else "Click to check for updates",
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastManualCheckTime < 600_000L) {
                        rateLimitHit = true
                        return@SettingsItemV2
                    }
                    if (!isChecking && !isDownloading) {
                        lastManualCheckTime = now
                        coroutineScope.launch {
                            isChecking = true
                            noUpdateFound = false
                            try {
                                val client = HttpClient(CIO) {
                                    install(ContentNegotiation) {
                                        json(Json { ignoreUnknownKeys = true })
                                    }
                                }
                                val manager = UpdateManager(client, currentVersion = "1.0.0", apiHost = config.updateApiHost)
                                val info = manager.checkForUpdate()
                                if (info != null) {
                                    updateInfo = info
                                } else {
                                    noUpdateFound = true
                                }
                                client.close()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                noUpdateFound = true
                            } finally {
                                isChecking = false
                            }
                        }
                    }
                }
            )
        }
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_about_licenses),
                trailingContent = {
                    Icon(
                        resource = Res.drawable.ic_external,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = {
                    navController.navigate("settings/licenses")
                }
            )
        }
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_about_credits),
                trailingContent = {
                    Icon(
                        resource = Res.drawable.ic_external,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = {
                    navController.navigate("settings/credits")
                }
            )
        }
        //item {
        //    SettingsItemV2(
        //        title = "State Simulation",
        //        description = "Simulate Minecraft and Download statuses",
        //        onClick = {
        //            navController.navigate("debug/simulation")
        //        }
        //    )
        //}
        item {
            SettingsItemV2(
                title = stringResource(Res.string.settings_uninstall_title),
                description = stringResource(Res.string.settings_uninstall_desc),
                trailingContent = {
                    Button(
                        content = { Text(stringResource(Res.string.settings_uninstall_confirm_btn)) },
                        type = ButtonType.Destructive,
                        onClick = { showUninstallDialog = true }
                    )
                }
            )
        }
    }

    if (showUninstallDialog) {
        var deleteUserData by remember { mutableStateOf(false) }
        var deleteSharedStorage by remember { mutableStateOf(false) }
        Dialog(
            onDismissRequest = { showUninstallDialog = false },
            title = { Text(stringResource(Res.string.settings_uninstall_dialog_title)) },
            buttons = {
                Button(
                    content = { Text(stringResource(Res.string.common_cancel)) },
                    type = ButtonType.Secondary,
                    onClick = { showUninstallDialog = false }
                )
                Button(
                    content = { Text(stringResource(Res.string.settings_uninstall_confirm_btn)) },
                    type = ButtonType.Destructive,
                    onClick = {
                        SystemRegistryManager.uninstallLauncher(
                            deleteUserData = deleteUserData,
                            deleteSharedStorage = deleteSharedStorage
                        )
                        exitProcess(0)
                    }
                )
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.settings_uninstall_dialog_desc))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteUserData,
                        onCheckedChange = { deleteUserData = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.settings_uninstall_delete_data))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteSharedStorage,
                        onCheckedChange = { deleteSharedStorage = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.settings_uninstall_delete_storage))
                }
            }
        }
    }

    if (rateLimitHit) {
        Dialog(
            onDismissRequest = { rateLimitHit = false },
            title = { Text("Rate Limit Protection") },
            buttons = {
                Button(
                    content = { Text("OK") },
                    onClick = { rateLimitHit = false },
                    type = ButtonType.Primary
                )
            }
        ) {
            Text("Please wait a few minutes before checking for updates again to avoid being rate-limited by GitHub API.")
        }
    }

    if (noUpdateFound) {
        Dialog(
            onDismissRequest = { noUpdateFound = false },
            title = { Text("No updates found") },
            buttons = {
                Button(
                    content = { Text("OK") },
                    onClick = { noUpdateFound = false },
                    type = ButtonType.Primary
                )
            }
        ) {
            Text("You are using the latest version of AnyMC Launcher.")
        }
    }

    updateInfo?.let { info ->
        Dialog(
            onDismissRequest = { if (!isDownloading) updateInfo = null },
            title = { Text("Update Available: ${info.latestVersion}") },
            buttons = {
                if (!isDownloading && !updateCompleted) {
                    Button(
                        content = { Text("Download & Update") },
                        onClick = {
                            coroutineScope.launch {
                                isDownloading = true
                                try {
                                    val client = HttpClient(CIO) {
                                        install(ContentNegotiation) {
                                            json(Json { ignoreUnknownKeys = true })
                                        }
                                    }
                                    val executor = UpdateExecutor()
                                    val extracted = executor.downloadAndPrepareUpdate(client, info) { progress ->
                                        downloadProgress = progress
                                    }
                                    preparedExtractedDir = extracted
                                    updateCompleted = true
                                    client.close()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isDownloading = false
                                }
                            }
                        },
                        type = ButtonType.Primary
                    )
                }
                if (updateCompleted) {
                    Button(
                        content = { Text("Restart & Apply Update") },
                        onClick = {
                            preparedExtractedDir?.let {
                                UpdateExecutor().applyUpdateAndRestart(it)
                            } ?: exitProcess(0)
                        },
                        type = ButtonType.Primary
                    )
                }
                Button(
                    content = { Text(if (updateCompleted) "Close" else "Cancel") },
                    onClick = {
                        if (updateCompleted) {
                            exitProcess(0)
                        } else {
                            updateInfo = null
                        }
                    },
                    type = ButtonType.Secondary
                )
            }
        ) {
            Column {
                Text("A new version (${info.latestVersion}) is available!")
                Spacer(Modifier.size(8.dp))
                if (isDownloading) {
                    Text("Downloading update... ${(downloadProgress * 100).toInt()}%")
                } else if (updateCompleted) {
                    Text("Update installed successfully! Please restart the app.")
                } else {
                    info.releaseNotes?.let {
                        Spacer(Modifier.size(8.dp))
                        Text("Release Notes:")
                        Text(it)
                    }
                }
            }
        }
    }
}
