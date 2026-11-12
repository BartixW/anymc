/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.steps

import com.bartixw.anymc.core.PipelineStep
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.minecraft.LaunchContext
import com.bartixw.anymc.minecraft.LaunchLogic
import com.bartixw.anymc.minecraft.InstallLogic
import com.bartixw.anymc.minecraft.utils.LaunchUtils
import com.bartixw.anymc.minecraft.data.*
import io.ktor.client.HttpClient
import java.io.File
import kotlinx.serialization.json.Json

class InitializeLaunchStep : PipelineStep<LaunchContext> {
    override val id = "initialize"
    override suspend fun execute(context: LaunchContext) {
        context.currentOS = PlatformUtils.getCurrentOS()
        if (context.baseDir == null || !context.baseDir!!.exists()) throw Exception("Invalid dir")
    }
}

class LoadConfigStep(private val json: Json) : PipelineStep<LaunchContext> {
    override val id = "loadConfig"
    override suspend fun execute(context: LaunchContext) {
        val bd = context.baseDir!!
        val idat = json.decodeFromString<AnyMCInstallationData>(bd.resolve("AnyMC.json").readText())
        val cdat = json.decodeFromString<Client>(bd.resolve("${idat.minecraftVersion}.json").readText())
        context.installationData = idat
        context.clientData = cdat
        context.targetJavaVersion = idat.javaVersion ?: cdat.effectiveJavaVersion
    }
}

class LoadModLoaderStep(private val json: Json) : PipelineStep<LaunchContext> {
    override val id = "loadModLoader"
    override suspend fun execute(context: LaunchContext) {
        val d = context.installationData!!
        if (d.loaderType in listOf("fabric", "legacyfabric")) {
            context.baseDir!!.resolve("fabric-profile.json").takeIf { it.exists() }?.let { 
                context.modLoaderData = json.decodeFromString<FabricProfile>(it.readText()) 
            }
        } else if (d.loaderType in listOf("forge", "neoforge")) {
            val file = context.baseDir!!.resolve("modloader-profile.json")
            if (file.exists()) {
                val profile = json.decodeFromString<MultiMCProfile>(file.readText())
                context.modLoaderData = profile
                
                (profile.libraries + profile.mavenFiles).find { it.name.contains(":installer") }?.let { lib ->
                    val path = lib.downloads?.artifact?.path ?: LaunchUtils.libraryToPath(lib.name)
                    val jarFile = context.baseDir!!.resolve("libraries").resolve(path)
                    if (jarFile.exists()) {
                        context.installerJarPath = jarFile.absolutePath
                    }
                }
            }
        }
    }
}

class BuildClasspathStep : PipelineStep<LaunchContext> {
    override val id = "buildClasspath"
    override suspend fun execute(context: LaunchContext) {
        val d = context.installationData!!
        val (cp, mp) = LaunchLogic.buildClasspath(context.baseDir!!, context.clientData!!, context.modLoaderData, context.currentOS, 
            d.loaderType in listOf("fabric", "legacyfabric"), d.loaderType in listOf("forge", "neoforge"), d.minecraftVersion)
        context.classpath = cp
        context.modulePath = mp
    }
}

class ExtractNativesStep : PipelineStep<LaunchContext> {
    override val id = "extractNatives"
    override suspend fun execute(context: LaunchContext) { 
        val nativesDir = context.baseDir!!.resolve("natives")
        LaunchLogic.extractNatives(context.baseDir!!, context.clientData!!, context.currentOS) 
        LaunchLogic.fixExecStack(nativesDir)
        LaunchLogic.patchLwjglNatives(nativesDir)
    }
}

class EnsureJavaRuntimeStep(
    private val client: HttpClient,
    private val onProgress: suspend (StatusType, Int, Int) -> Unit
) : PipelineStep<LaunchContext> {
    override val id = "ensureJava"
    override suspend fun execute(context: LaunchContext) {
        InstallLogic.ensureJavaRuntime(client, context.targetJavaVersion!!, onProgress)
    }
}

class EnsureCompatStep(private val client: HttpClient) : PipelineStep<LaunchContext> {
    override val id = "ensureCompat"
    override suspend fun execute(context: LaunchContext) {
        if (context.currentOS == "linux") {
            InstallLogic.provisionZlibCompat(client)
        }
    }
}

class BuildCommandStep : PipelineStep<LaunchContext> {
    override val id = "buildCommand"
    override suspend fun execute(context: LaunchContext) {
        val d = context.installationData!!
        val ph = LaunchLogic.buildPlaceholders(context.baseDir!!, d, context.clientData!!, context.accountData!!, context.classpath!!, context.quickPlay, context.width, context.height)
        context.placeholders = ph
        context.finalCommand = LaunchLogic.buildFinalCommand(context.baseDir!!, d, context.clientData!!, context.modLoaderData, 
            ph.filterValues { it != null }.mapValues { it.value!! }, context.activeFeatures!!, context.installerJarPath, context.maxRamMb, context.currentOS, 
            d.loaderType in listOf("fabric", "legacyfabric"), d.loaderType in listOf("forge", "neoforge"), context.modulePath ?: "", context.targetJavaVersion!!)
    }
}

class ExecuteLaunchStep(
    private val exec: suspend (File, List<String>, String, JavaVersion?, Map<String, String>, String) -> Unit
) : PipelineStep<LaunchContext> {
    override val id = "execute"
    override suspend fun execute(context: LaunchContext) {
        exec(
            context.baseDir!!,
            context.finalCommand!!,
            context.currentOS,
            context.targetJavaVersion,
            context.placeholders!!.filterValues { it != null }.mapValues { it.value!! },
            context.accountData!!.uuid
        )
    }
}
