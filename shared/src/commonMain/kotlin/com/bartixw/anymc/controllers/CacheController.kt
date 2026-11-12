/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.controllers

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.also

class CacheController {
    // ConcurrentHashMap handles thread-safe map operations on the JVM
    private val cache = ConcurrentHashMap<String, Any>()
    private val mutex = Mutex()

    fun get(key: String, fetch: () -> Any?): Any? {
        // getOrPut on ConcurrentHashMap is thread-safe for the map itself
        return cache.getOrPut(key) { fetch() }
    }

    suspend fun get(key: String, fetch: suspend () -> Any?): Any? {
        // First check without locking for performance
        cache[key]?.let { return it }

        // Use a Mutex to ensure only one 'fetch' runs at a time for this controller
        return mutex.withLock {
            // Check again inside the lock (Double-checked locking pattern)
            cache[key] ?: fetch().also {
                if (it == null) return null
                cache[key] = it
            }
        }
    }

    fun clear(key: String) {
        cache.remove(key)
    }

    fun clearAll() {
        cache.clear()
    }
}

val LocalCacheController = staticCompositionLocalOf<CacheController> {
    error("No CacheController provided")
}