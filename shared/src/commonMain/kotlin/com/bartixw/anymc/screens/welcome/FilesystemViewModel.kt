/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.welcome

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bartixw.anymc.core.utils.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import kotlin.io.path.Path

class WelcomeFilesystemViewModel : ViewModel() {
    sealed interface UiState {
        object Checking : UiState
        object NeedsSudo : UiState
        object Error : UiState
    }

    var uiState by mutableStateOf<UiState>(UiState.Checking)
        private set

    private val _navigationEvent = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val os = System.getProperty("os.name").lowercase()
    val isLinux = !(os.contains("win") || os.contains("mac"))

    init {
        checkAndInitializeSetup()
    }

    private fun checkAndInitializeSetup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Files.createDirectories(Path(PlatformUtils.userDataDirectory.path))

                if (!isLinux) {
                    Files.createDirectories(Path(PlatformUtils.sharedStorageDirectory.path))
                    _navigationEvent.emit(Unit)
                } else {
                    val storagePath = Path(PlatformUtils.sharedStorageDirectory.path)
                    if (Files.exists(storagePath) && Files.isWritable(storagePath)) {
                        _navigationEvent.emit(Unit)
                    } else {
                        withContext(Dispatchers.Main) {
                            uiState = UiState.NeedsSudo
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    uiState = UiState.Error
                }
            }
        }
    }

    fun createSharedStorage() {
        if (uiState == UiState.Checking) return

        viewModelScope.launch(Dispatchers.Main) {
            uiState = UiState.Checking
        }

        viewModelScope.launch(Dispatchers.IO) {
            val targetPath = PlatformUtils.sharedStorageDirectory.path
            try {
                val command = "mkdir -p $targetPath && chmod -R 777 $targetPath/.."
                val process = ProcessBuilder("pkexec", "sh", "-c", command).start()

                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    _navigationEvent.emit(Unit)
                } else {
                    withContext(Dispatchers.Main) {
                        uiState = UiState.Error
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    uiState = UiState.Error
                }
            }
        }
    }
}
