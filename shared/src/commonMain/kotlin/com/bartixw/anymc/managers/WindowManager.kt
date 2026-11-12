/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

object WindowManager {
    var onMinimize: (() -> Unit)? = null
    var onRestore: (() -> Unit)? = null

    fun minimize() {
        onMinimize?.invoke()
    }

    fun restore() {
        onRestore?.invoke()
    }
}
