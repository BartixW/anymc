/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core

interface PipelineStep<T> {
    val id: String
    suspend fun execute(context: T)
}

open class Pipeline<T>(val namespace: String) {
    private val steps = mutableListOf<PipelineStep<T>>()

    fun addStep(step: PipelineStep<T>): Pipeline<T> {
        steps.add(step)
        return this
    }

    suspend fun execute(context: T) {
        steps.forEach { step ->
            val hookName = "$namespace/${step.id}"
            HookRegistry.triggerBefore(hookName, context)
            step.execute(context)
            HookRegistry.triggerAfter(hookName, context)
        }
    }
}
