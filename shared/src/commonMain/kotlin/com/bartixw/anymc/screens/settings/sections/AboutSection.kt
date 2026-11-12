/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.settings.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.*
import com.bartixw.anymc.core.updater.UpdateExecutor
import com.bartixw.anymc.core.updater.UpdateInfo
import com.bartixw.anymc.core.updater.UpdateManager
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McDialog
import com.bartixw.anymc.ui.components.McIcon
import com.bartixw.anymc.ui.components.McListItem
import com.bartixw.anymc.ui.components.McText
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource
import kotlin.system.exitProcess

@Composable
fun AboutSection(navController: NavController) {
    val imageBitmap = imageResource(Res.drawable.app_icon)
    val coroutineScope = rememberCoroutineScope()

    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var noUpdateFound by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var updateCompleted by remember { mutableStateOf(false) }

    McListItem(
        title = stringResource(Res.string.settings_section_about_title),
        iconBitmap = imageBitmap,
        description = stringResource(Res.string.settings_about_launcher_desc),
        modifier = Modifier.heightIn(64.dp).fillMaxWidth(),
    )
    McListItem(
        title = stringResource(Res.string.settings_about_check_updates),
        disabled = isChecking || isDownloading,
        onClick = {
            coroutineScope.launch {
                isChecking = true
                noUpdateFound = false
                try {
                    val client = HttpClient(CIO) {
                        install(ContentNegotiation) {
                            json(Json { ignoreUnknownKeys = true })
                        }
                    }
                    val manager = UpdateManager(client, currentVersion = "v0.1.0")
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
    )
    McListItem(
        title = stringResource(Res.string.settings_about_licenses),
        trailingContent = {
            McIcon(
                resource = Res.drawable.ic_external,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        onClick = {
            navController.navigate("settings/licenses")
        }
    )
    McListItem(
        title = stringResource(Res.string.settings_about_credits),
        trailingContent = {
            McIcon(
                resource = Res.drawable.ic_external,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        onClick = {
            navController.navigate("settings/credits")
        }
    )
    //McListItem(
    //    title = "State Simulation",
    //    description = "Simulate Minecraft and Download statuses",
    //    onClick = {
    //        navController.navigate("debug/simulation")
    //    }
    //)
    McListItem(
        title = stringResource(Res.string.settings_uninstall_title),
        description = stringResource(Res.string.settings_uninstall_desc),
        trailingContent = {
            McButton(
                text = stringResource(Res.string.settings_uninstall_confirm_btn),
                onClick = {
                    com.bartixw.anymc.core.system.SystemRegistryManager.uninstallLauncher()
                    exitProcess(0)
                }
            )
        }
    )

    if (isChecking) {
        McDialog(
            onDismissRequest = {},
            title = "Checking for updates...",
            buttons = {}
        ) {
            McText("Please wait while we check for available updates.")
        }
    }

    if (noUpdateFound) {
        McDialog(
            onDismissRequest = { noUpdateFound = false },
            title = "No updates found",
            buttons = {
                McButton(
                    text = "OK",
                    onClick = { noUpdateFound = false }
                )
            }
        ) {
            McText("You are using the latest version of AnyMC Launcher.")
        }
    }

    updateInfo?.let { info ->
        McDialog(
            onDismissRequest = { if (!isDownloading) updateInfo = null },
            title = "Update Available: ${info.latestVersion}",
            buttons = {
                if (!isDownloading && !updateCompleted) {
                    McButton(
                        text = "Download & Update",
                        onClick = {
                            coroutineScope.launch {
                                isDownloading = true
                                try {
                                    val executor = UpdateExecutor()
                                    val appsDir = PlatformUtils.userProgramsDirectory
                                    executor.downloadAndApplyUpdate(info, appsDir) { progress ->
                                        downloadProgress = progress
                                    }
                                    updateCompleted = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isDownloading = false
                                }
                            }
                        }
                    )
                }
                if (updateCompleted) {
                    McButton(
                        text = "Restart Now",
                        onClick = {
                            exitProcess(0)
                        }
                    )
                }
                McButton(
                    text = if (updateCompleted) "Close" else "Cancel",
                    onClick = {
                        if (updateCompleted) {
                            exitProcess(0)
                        } else {
                            updateInfo = null
                        }
                    }
                )
            }
        ) {
            Column {
                McText("A new version (${info.latestVersion}) is available!")
                Spacer(Modifier.size(8.dp))
                if (isDownloading) {
                    McText("Downloading update... ${(downloadProgress * 100).toInt()}%")
                } else if (updateCompleted) {
                    McText("Update installed successfully! Please restart the app.")
                } else {
                    info.releaseNotes?.let {
                        Spacer(Modifier.size(8.dp))
                        McText("Release Notes:")
                        McText(it)
                    }
                }
            }
        }
    }
}
