/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.core.utils.PlatformUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.seconds

abstract class BasePersistentDataManager<T>(
    private val fileName: String,
    private val defaultValue: T,
    private val serializer: KSerializer<T>
) {
    private val _data = MutableStateFlow(defaultValue)
    val data: StateFlow<T> = _data.asStateFlow()

    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var saveJob: Job? = null

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val file by lazy { File(PlatformUtils.userDataDirectory, fileName) }

    var isLoaded = false
        private set

    fun loadBlocking() {
        try {
            if (file.exists()) {
                val content = file.readText()
                _data.value = json.decodeFromString(serializer, content)
            } else {
                _data.value = defaultValue
                saveImmediately(defaultValue)
            }
        } catch (e: Exception) {
            println("Error loading $fileName: ${e.message}. Preserving broken file.")
            // Don't overwrite the file if it's broken! 
            // Just use defaults in memory for this session.
            _data.value = defaultValue
        } finally {
            isLoaded = true
        }
    }

    suspend fun load() {
        withContext(Dispatchers.IO) {
            loadBlocking()
        }
    }

    fun update(transform: (T) -> T) {
        _data.update { current ->
            val newValue = transform(current)

            saveJob?.cancel()
            saveJob = scope.launch {
                delay(1.seconds)
                saveImmediately(newValue)
            }

            newValue
        }
    }

    private fun saveImmediately(value: T) {
        try {
            if (file.exists()) {
                val backupFile = File(file.parentFile, "${file.name}.bak")
                file.copyTo(backupFile, overwrite = true)
            }
            val content = json.encodeToString(serializer, value)
            file.writeText(content)
            println("Saved file $fileName")
        } catch (e: Exception) {
            println("Failed to save files $fileName: ${e.message}")
        }
    }
}