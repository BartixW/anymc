/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft

import io.ktor.client.HttpClient
import io.ktor.http.Url
import com.bartixw.anymc.minecraft.data.Client
import java.io.File

data class InstallContext(
    val client: HttpClient,
    val name: String,
    val url: Url,
    var baseDir: File? = null,
    var minecraftVersion: String? = null,
    var clientData: Client? = null,
    var loaderType: String? = null,
    var loaderVersion: String? = null,
    var isLegacy: Boolean = false,
    var overrideBaseDir: File? = null
)
