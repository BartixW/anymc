/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.v2.dialogs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.profile_add_account_elyby_error
import anymc.shared.generated.resources.profile_add_account_elyby_logging_in
import anymc.shared.generated.resources.profile_add_account_ms_error_api
import anymc.shared.generated.resources.profile_add_account_ms_error_window
import anymc.shared.generated.resources.profile_add_account_ms_logging_in
import anymc.shared.generated.resources.profile_add_account_ms_starting
import com.bartixw.anymc.helpers.HelperResolver
import com.bartixw.anymc.managers.AccountManager
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.services.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

enum class AccountType {
    MICROSOFT, ELYBY, OFFLINE
}

class NewAccountDialogViewModel : ViewModel() {
    private val authService = AuthService(MinecraftManager.client)
    private val activeProcess = AtomicReference<Process?>(null)

    var accountType by mutableStateOf(AccountType.MICROSOFT)
    var offlineUsername by mutableStateOf("")
    var elyByUsername by mutableStateOf("")
    var elyByPassword by mutableStateOf("")
    
    var isHelperRunning by mutableStateOf(false)
    var helperStatus by mutableStateOf<StringResource?>(null)
    var error by mutableStateOf<StringResource?>(null)
    var errorDetail by mutableStateOf<String?>(null)

    fun reset() {
        error = null
        errorDetail = null
        helperStatus = null
        isHelperRunning = false
        activeProcess.getAndSet(null)?.destroy()
    }

    fun loginMicrosoft(onSuccess: () -> Unit) {
        if (isHelperRunning) return
        
        viewModelScope.launch {
            isHelperRunning = true
            helperStatus = Res.string.profile_add_account_ms_starting
            error = null
            errorDetail = null
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
                    helperStatus = Res.string.profile_add_account_ms_logging_in
                    val account = authService.loginWithCode(code)
                    if (account != null) {
                        AccountManager.addAccount(account)
                        onSuccess()
                    } else {
                        error = Res.string.profile_add_account_ms_error_api
                    }
                } else {
                    error = Res.string.profile_add_account_ms_error_window
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorDetail = e.message
            } finally {
                isHelperRunning = false
                activeProcess.set(null)
            }
        }
    }

    fun loginElyBy(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isHelperRunning = true
            helperStatus = Res.string.profile_add_account_elyby_logging_in
            error = null
            errorDetail = null
            try {
                val account = authService.loginWithElyByPassword(elyByUsername, elyByPassword)
                if (account != null) {
                    AccountManager.addAccount(account)
                    onSuccess()
                } else {
                    error = Res.string.profile_add_account_elyby_error
                }
            } catch (e: Exception) {
                errorDetail = e.message
            } finally {
                isHelperRunning = false
            }
        }
    }

    fun loginOffline(onSuccess: () -> Unit) {
        if (offlineUsername.isNotBlank()) {
            val account = authService.loginOffline(offlineUsername)
            AccountManager.addAccount(account)
            onSuccess()
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeProcess.getAndSet(null)?.destroy()
    }
}
