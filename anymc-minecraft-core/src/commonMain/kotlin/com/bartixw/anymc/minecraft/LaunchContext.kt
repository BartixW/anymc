/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft

import com.bartixw.anymc.minecraft.data.*
import java.io.File

data class LaunchContext(
    val installationId: String,
    val quickPlay: QuickPlay? = null,
    var baseDir: File? = null,
    var installationData: AnyMCInstallationData? = null,
    var clientData: Client? = null,
    var modLoaderData: Any? = null,
    var installerJarPath: String? = null,
    var classpath: String? = null,
    var modulePath: String? = null,
    var accountData: ResolvedAccount? = null,
    var placeholders: Map<String, String?>? = null,
    var activeFeatures: Set<String>? = null,
    var finalCommand: List<String>? = null,
    var maxRamMb: Int = 4096,
    var width: Int = 854,
    var height: Int = 480,
    var currentOS: String = "linux",
    var targetJavaVersion: JavaVersion? = null
)
