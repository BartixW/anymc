/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.popups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.ic_close
import anymc.shared.generated.resources.ic_exit
import anymc.shared.generated.resources.ic_external
import anymc.shared.generated.resources.ic_profile_add
import com.bartixw.anymc.helpers.HelperResolver
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.services.AuthService
import com.bartixw.anymc.ui.components.McButton
import com.bartixw.anymc.ui.components.McCheckbox
import com.bartixw.anymc.ui.components.McDialog
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.McTextField
import com.bartixw.anymc.core.utils.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

@Composable
fun NewAccountPopup(visible: Boolean, onDismissRequest: () -> Unit) {
    var accountType by remember { mutableStateOf("ms") }
    var offlineAccountName by remember { mutableStateOf("") }
    var elyByUsername by remember { mutableStateOf("") }
    var elyByPassword by remember { mutableStateOf("") }
    var msaRedirectUrl by remember { mutableStateOf("") }
    var isHelperRunning by remember { mutableStateOf(false) }
    var helperStatus by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val authService = remember { AuthService(MinecraftManager.client) }
    val activeProcess = remember { AtomicReference<Process?>(null) }

    // Ensure process is killed when the component is disposed or app closes
    DisposableEffect(Unit) {
        onDispose {
            activeProcess.getAndSet(null)?.destroy()
        }
    }

    if (visible) McDialog(
        onDismissRequest = { 
            activeProcess.getAndSet(null)?.destroy()
            onDismissRequest() 
        },
        title = "Dodaj konto"
    ) {
        Row {
            Column {
                Row (verticalAlignment = Alignment.CenterVertically) {
                    McCheckbox(
                        checked = accountType == "ms",
                        onCheckedChange = { accountType = "ms" }
                    )
                    Spacer(Modifier.size(8.dp))
                    McText("Microsoft account")
                }
                Spacer(Modifier.size(8.dp))
                Row (verticalAlignment = Alignment.CenterVertically) {
                    McCheckbox(
                        checked = accountType == "ely.by",
                        onCheckedChange = { accountType = "ely.by" }
                    )
                    Spacer(Modifier.size(8.dp))
                    McText("Ely.by account")
                }
                Spacer(Modifier.size(8.dp))
                Row (verticalAlignment = Alignment.CenterVertically) {
                    McCheckbox(
                        checked = accountType == "offline",
                        onCheckedChange = { accountType = "offline" }
                    )
                    Spacer(Modifier.size(8.dp))
                    McText("Offline account")
                }
            }
            Spacer(Modifier.size(16.dp))
            Column (Modifier.weight(1f)) {
                when (accountType) {
                    "ms" -> {
                        McButton(
                            text = if (isHelperRunning) "Waiting..." else "Log in",
                            icon = Res.drawable.ic_external,
                            disabled = isHelperRunning,
                            onClick = {
                                scope.launch {
                                    isHelperRunning = true
                                    helperStatus = "Starting auth window..."
                                    try {
                                        val code = withContext(Dispatchers.IO) {
                                            val helperFile = HelperResolver.getAuthHelperPath()
                                            
                                            val completionPattern = ".*(code=|error=).*"
                                            
                                            val process = ProcessBuilder(
                                                helperFile,
                                                authService.getAuthUrl(),
                                                completionPattern
                                            ).start()
                                            
                                            activeProcess.set(process)
                                            
                                            val reader = BufferedReader(InputStreamReader(process.inputStream))
                                            var outputLine: String?
                                            var foundCode: String? = null
                                            
                                            try {
                                                while (reader.readLine().also { outputLine = it } != null) {
                                                    val line = outputLine?.trim() ?: continue
                                                    println("[AuthHelper]: $line")
                                                    
                                                    // The helper prints the final URL at stdout
                                                    if (line.contains("code=")) {
                                                        val rawCode = line.substringAfter("code=").substringBefore("&")
                                                        foundCode = URLDecoder.decode(rawCode, StandardCharsets.UTF_8.name())
                                                        break
                                                    } else if (line.contains("error=")) {
                                                        break
                                                    }
                                                }
                                            } finally {
                                                activeProcess.set(null)
                                                process.destroy()
                                            }
                                            process.waitFor()
                                            foundCode
                                        }
                                        
                                        if (code != null) {
                                            helperStatus = "Logging in..."
                                            val account = authService.loginWithCode(code)
                                            if (account != null) {
                                                AccountManager.addAccount(account)
                                                onDismissRequest()
                                            } else {
                                                helperStatus = "Login error (Minecraft API)"
                                            }
                                        } else {
                                            helperStatus = "Cancelled"
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        helperStatus = "Error: ${e.message}"
                                    } finally {
                                        isHelperRunning = false
                                        activeProcess.set(null)
                                    }
                                }
                            }
                        )
                        if (helperStatus.isNotEmpty()) {
                            McText(helperStatus, modifier = Modifier.padding(top = 4.dp))
                        }

                    }
                    "ely.by" -> {
                        McText("E-mail or name:")
                        McTextField(
                            value = elyByUsername,
                            onValueChange = { elyByUsername = it },
                            placeholder = "Your Ely.by name or e-mail"
                        )
                        Spacer(Modifier.size(8.dp))
                        McText("Hasło:")
                        McTextField(
                            value = elyByPassword,
                            onValueChange = { elyByPassword = it },
                            placeholder = "Your password",
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(Modifier.size(16.dp))
                        McButton(
                            text = if (isHelperRunning) "Logging in..." else "Log in",
                            icon = Res.drawable.ic_profile_add,
                            disabled = isHelperRunning || elyByUsername.isBlank() || elyByPassword.isBlank(),
                            onClick = {
                                scope.launch {
                                    isHelperRunning = true
                                    helperStatus = "Logging in to Ely.by..."
                                    try {
                                        val account = authService.loginWithElyByPassword(elyByUsername, elyByPassword)
                                        if (account != null) {
                                            AccountManager.addAccount(account)
                                            onDismissRequest()
                                        } else {
                                            helperStatus = "Login error. Check credentials."
                                        }
                                    } catch (e: Exception) {
                                        helperStatus = "Error: ${e.message}"
                                    } finally {
                                        isHelperRunning = false
                                    }
                                }
                            }
                        )
                        if (helperStatus.isNotEmpty()) {
                            McText(helperStatus, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    "offline" -> {
                        McText("Username:")
                        McTextField(value = offlineAccountName, onValueChange = { offlineAccountName = it })
                        Spacer(Modifier.size(8.dp))
                        McButton(
                            text = "Add account",
                            icon = Res.drawable.ic_profile_add,
                            onClick = {
                                if (offlineAccountName.isNotBlank()) {
                                    val account = authService.loginOffline(offlineAccountName)
                                    AccountManager.addAccount(account)
                                    onDismissRequest()
                                }
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.size(16.dp))
        McButton(
            text = "Cancel",
            icon = Res.drawable.ic_close,
            onClick = {
                activeProcess.getAndSet(null)?.destroy()
                onDismissRequest()
            }
        )
    }
}
