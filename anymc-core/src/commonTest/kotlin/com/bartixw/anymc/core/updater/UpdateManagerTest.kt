/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.updater

import io.ktor.client.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateManagerTest {

    @Test
    fun testVersionComparison() {
        val client = HttpClient()
        val manager = UpdateManager(client, currentVersion = "v0.1.0")

        assertTrue(manager.isNewerVersion("v0.1.0", "v0.2.0"))
        assertTrue(manager.isNewerVersion("0.1.0", "0.1.1"))
        assertFalse(manager.isNewerVersion("v0.2.0", "v0.1.0"))
        assertFalse(manager.isNewerVersion("v0.1.0", "0.1.0"))
        assertTrue(manager.isNewerVersion("v0.1", "v1.0"))
    }
}
