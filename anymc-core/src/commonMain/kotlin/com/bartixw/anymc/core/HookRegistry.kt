/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core

object HookRegistry {
    private val beforeHooks = mutableMapOf<String, MutableList<Pair<String, suspend (Any?) -> Unit>>>()
    private val afterHooks = mutableMapOf<String, MutableList<Pair<String, suspend (Any?) -> Unit>>>()

    fun hookBefore(targetHook: String, hookId: String, action: suspend (Any?) -> Unit) {
        beforeHooks.getOrPut(targetHook) { mutableListOf() }.add(hookId to action)
    }

    fun hookAfter(targetHook: String, hookId: String, action: suspend (Any?) -> Unit) {
        afterHooks.getOrPut(targetHook) { mutableListOf() }.add(hookId to action)
    }

    suspend fun triggerBefore(hookName: String, data: Any? = null) {
        beforeHooks[hookName]?.forEach { (id, action) ->
            // Hooks themselves are hookable
            triggerBefore(id, data)
            action(data)
            triggerAfter(id, data)
        }
    }

    suspend fun triggerAfter(hookName: String, data: Any? = null) {
        afterHooks[hookName]?.forEach { (id, action) ->
            triggerBefore(id, data)
            action(data)
            triggerAfter(id, data)
        }
    }
}
