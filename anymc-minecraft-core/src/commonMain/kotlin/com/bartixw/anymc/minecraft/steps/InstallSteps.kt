/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.steps

import com.bartixw.anymc.core.PipelineStep
import com.bartixw.anymc.core.utils.PlatformUtils
import com.bartixw.anymc.core.utils.toSafeString
import com.bartixw.anymc.minecraft.InstallContext
import com.bartixw.anymc.minecraft.InstallLogic
import com.bartixw.anymc.minecraft.InstallLogic.linkFileFromCache
import com.bartixw.anymc.minecraft.data.*
import com.bartixw.anymc.minecraft.data.modrinth.*
import com.bartixw.anymc.minecraft.utils.LaunchUtils
import com.bartixw.anymc.minecraft.utils.MetadataUtils
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.Url
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.time.Instant

class InitializeInstallStep : PipelineStep<InstallContext> {
    override val id = "initialize"
    override suspend fun execute(context: InstallContext) {
        if (context.baseDir == null) {
            val ts = Instant.now().epochSecond
            context.baseDir = context.overrideBaseDir ?: PlatformUtils.userDataDirectory.resolve("installations").resolve("${toSafeString(context.name)}.$ts")
        }
        context.baseDir?.mkdirs()
    }
}

class FetchManifestStep(private val client: HttpClient, private val json: Json) : PipelineStep<InstallContext> {
    override val id = "fetchManifest"
    override suspend fun execute(context: InstallContext) {
        val body = client.get(context.url).bodyAsText()
        val data = json.decodeFromString<Client>(body)
        context.minecraftVersion = data.id
        context.clientData = data
        context.baseDir!!.resolve("${data.id}.json").apply { 
            parentFile.mkdirs()
            writeText(body) 
        }
    }
}

class DownloadClientStep(private val client: HttpClient) : PipelineStep<InstallContext> {
    override val id = "downloadClient"
    override suspend fun execute(context: InstallContext) {
        context.clientData?.downloads?.client?.let {
            InstallLogic.downloadFile(client, it.url, context.baseDir!!.resolve("${context.minecraftVersion}.jar").toPath(), it.sha1)
        }
    }
}

class DownloadLibrariesStep(private val client: HttpClient, private val onProgress: suspend (Int, Int) -> Unit) : PipelineStep<InstallContext> {
    override val id = "downloadLibraries"
    override suspend fun execute(context: InstallContext) {
        val os = PlatformUtils.getCurrentOS()
        val libs = context.clientData!!.libraries.filter { it.isAllowed(os) }
        libs.forEachIndexed { i, lib ->
            onProgress(i, libs.size)
            val nc = lib.natives?.get(os)
            if (nc == null) {
                val art = lib.downloads?.artifact
                val path = art?.path ?: LaunchUtils.libraryToPath(lib.name)
                val file = context.baseDir!!.resolve("libraries").resolve(path).apply { parentFile.mkdirs() }
                try { 
                    InstallLogic.downloadFile(client, art?.url ?: "https://libraries.minecraft.net/$path", file.toPath(), art?.sha1) 
                } catch (e: Exception) {
                    println("Skipped lib: ${lib.name}")
                }
            }
            nc?.let {
                val path = LaunchUtils.libraryToPath("${lib.name}:$it")
                val file = context.baseDir!!.resolve("libraries").resolve(path).apply { parentFile.mkdirs() }
                InstallLogic.downloadFile(client, "https://libraries.minecraft.net/$path", file.toPath(), null)
            }
        }
    }
}

class DownloadAuthlibInjector(
    private val client: HttpClient,
    private val exec: () -> String?
) : PipelineStep<InstallContext> {
    override val id = "downloadAuthlibInjector"
    override suspend fun execute(context: InstallContext) {
        val path = context.baseDir!!.resolve("libraries/authlib-injector.jar").toPath()
        var hash = exec()
        hash = hash ?: InstallLogic.downloadAuthlibInjector(client, path)

        if (hash == null) return

        linkFileFromCache(hash, path)
    }
}

class DownloadAssetsStep(private val client: HttpClient, private val onProgress: suspend (Int, Int) -> Unit) : PipelineStep<InstallContext> {
    override val id = "downloadAssets"
    override suspend fun execute(context: InstallContext) {
        context.clientData?.assetIndex?.let { idx ->
            InstallLogic.installAssets(client, idx, context.baseDir!!, onProgress)
        }
    }
}

class SetupJavaStep(private val client: HttpClient, private val onProgress: suspend (StatusType, Int, Int) -> Unit) : PipelineStep<InstallContext> {
    override val id = "setupJava"
    override suspend fun execute(context: InstallContext) {
        InstallLogic.ensureJavaRuntime(client, context.clientData!!.effectiveJavaVersion, onProgress)
    }
}

class FetchFabricManifestStep(private val client: HttpClient, private val json: Json) : PipelineStep<InstallContext> {
    override val id = "fetchFabricManifest"
    override suspend fun execute(context: InstallContext) {
        val vBody = client.get(context.url).bodyAsText()
        val vData = json.decodeFromString<Client>(vBody)
        context.minecraftVersion = vData.id
        context.clientData = vData
        context.baseDir!!.resolve("${vData.id}.json").apply { parentFile.mkdirs(); writeText(vBody) }

        val mUrl = if (context.isLegacy) "https://meta.legacyfabric.net/v2" else "https://meta.fabricmc.net/v2"
        val fUrl = "$mUrl/versions/loader/${vData.id}/${context.loaderVersion}/profile/json"
        val fBody = client.get(fUrl).bodyAsText()
        context.baseDir!!.resolve("fabric-profile.json").writeText(fBody)
    }
}

class DownloadFabricLibrariesStep(private val client: HttpClient, private val json: Json, private val onProgress: suspend (Int, Int) -> Unit) : PipelineStep<InstallContext> {
    override val id = "downloadLibraries"
    override suspend fun execute(context: InstallContext) {
        val os = PlatformUtils.getCurrentOS()
        val fProf = json.decodeFromString<FabricProfile>(context.baseDir!!.resolve("fabric-profile.json").readText())
        val libs = (context.clientData!!.libraries + fProf.libraries).filter { it.isAllowed(os) }
        libs.forEachIndexed { i, lib ->
            onProgress(i, libs.size)
            val path = lib.downloads?.artifact?.path ?: LaunchUtils.libraryToPath(lib.name)
            val file = context.baseDir!!.resolve("libraries").resolve(path).apply { parentFile.mkdirs() }
            val url = lib.downloads?.artifact?.url ?: lib.url?.let { if (it.endsWith("/")) it else "$it/" }?.let { "$it$path" } ?: "https://libraries.minecraft.net/$path"
            try { 
                InstallLogic.downloadFile(client, url, file.toPath(), lib.downloads?.artifact?.sha1) 
            } catch (e: Exception) {
                println("Skipped lib: ${lib.name}")
            }
        }
        InstallLogic.downloadAuthlibInjector(client, context.baseDir!!.resolve("libraries/authlib-injector.jar").toPath())
    }
}

class FetchMultiMCManifestStep(private val client: HttpClient, private val json: Json) : PipelineStep<InstallContext> {
    override val id = "fetchManifest"
    override suspend fun execute(context: InstallContext) {
        val vBody = client.get(context.url).bodyAsText()
        val vData = json.decodeFromString<Client>(vBody)
        context.minecraftVersion = vData.id
        context.clientData = vData
        context.baseDir!!.resolve("${vData.id}.json").apply { parentFile.mkdirs(); writeText(vBody) }
        val uid = when(context.loaderType) { 
            "forge" -> "net.minecraftforge"
            "neoforge" -> "net.neoforged"
            else -> throw Exception("Unknown loader type: ${context.loaderType}") 
        }
        val pBody = client.get("https://meta.multimc.org/v1/$uid/${context.loaderVersion}.json").bodyAsText()
        context.baseDir!!.resolve("modloader-profile.json").writeText(pBody)
    }
}

class DownloadMultiMCLibrariesStep(private val client: HttpClient, private val json: Json, private val onProgress: suspend (Int, Int) -> Unit) : PipelineStep<InstallContext> {
    override val id = "downloadLibraries"
    override suspend fun execute(context: InstallContext) {
        val os = PlatformUtils.getCurrentOS()
        val mProf = json.decodeFromString<MultiMCProfile>(context.baseDir!!.resolve("modloader-profile.json").readText())
        val libs = (context.clientData!!.libraries + mProf.libraries + mProf.mavenFiles).filter { it.isAllowed(os) }
        libs.forEachIndexed { i, lib ->
            onProgress(i, libs.size)
            val path = lib.downloads?.artifact?.path ?: LaunchUtils.libraryToPath(lib.name)
            val file = context.baseDir!!.resolve("libraries").resolve(path).apply { parentFile.mkdirs() }
            val url = lib.downloads?.artifact?.url ?: lib.url?.let { if (it.endsWith("/")) it else "$it/" }?.let { "$it$path" } ?: "https://libraries.minecraft.net/$path"
            try { 
                InstallLogic.downloadFile(client, url, file.toPath(), lib.downloads?.artifact?.sha1) 
            } catch (e: Exception) {
                println("Skipped lib: ${lib.name}")
            }
        }
        InstallLogic.downloadAuthlibInjector(client, context.baseDir!!.resolve("libraries/authlib-injector.jar").toPath())
    }
}

class WriteMetadataStep(private val json: Json, private val onFinished: suspend () -> Unit) : PipelineStep<InstallContext> {
    override val id = "writeMetadata"
    override suspend fun execute(context: InstallContext) {
        val file = context.baseDir!!.resolve("AnyMC.json")
        val existing = if (file.exists()) {
            try { json.decodeFromString<AnyMCInstallationData>(file.readText()) } catch (_: Exception) { null }
        } else null

        val d = AnyMCInstallationData(
            name = existing?.name ?: context.name, 
            minecraftVersion = context.minecraftVersion ?: existing?.minecraftVersion ?: "", 
            version = "1", 
            loaderType = context.loaderType ?: existing?.loaderType, 
            loaderVersion = context.loaderVersion ?: existing?.loaderVersion,
            jvmArgs = existing?.jvmArgs ?: emptyList(),
            removedArgs = existing?.removedArgs ?: emptyList(),
            javaVersion = existing?.javaVersion
        )
        file.writeText(json.encodeToString(d))
        onFinished()
    }
}

class DownloadModpackStep(
    private val client: HttpClient,
    private val json: Json,
    private val onStatusUpdate: suspend (StatusType) -> Unit,
    private val onProgress: suspend (Int, Int) -> Unit,
    private val onManifestReady: suspend (VersionManifest) -> Unit,
    private val installFabric: suspend (Url, String, String) -> Unit,
    private val installMultiMC: suspend (Url, String, String, String) -> Unit,
    private val installVanilla: suspend (Url, String) -> Unit
) : PipelineStep<InstallContext> {
    override val id = "downloadPack"
    override suspend fun execute(context: InstallContext) {
        onStatusUpdate(StatusType.MODPACK_DL)
        val file = context.baseDir!!.resolve("modpack.mrpack")
        InstallLogic.downloadFile(client, context.url.toString(), file.toPath())
        val index = json.decodeFromString<ModpackIndex>(com.bartixw.anymc.core.utils.ZipUtils.readEntry(file, "modrinth.index.json") ?: throw Exception("Invalid"))
        val mcV = index.dependencies["minecraft"] ?: throw Exception("No MC version")
        
        val manifestReq = client.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
        val manifest = json.decodeFromString<VersionManifest>(manifestReq.bodyAsText())
        onManifestReady(manifest)
        
        val mcUrl = Url(manifest.versions.find { it.id == mcV }?.url ?: throw Exception("Not found"))
        
        if (index.dependencies.containsKey("fabric-loader")) {
            installFabric(mcUrl, index.dependencies["fabric-loader"]!!, context.name)
        } else if (index.dependencies.containsKey("forge")) {
            installMultiMC(mcUrl, "forge", index.dependencies["forge"]!!, context.name)
        } else if (index.dependencies.containsKey("neoforge")) {
            installMultiMC(mcUrl, "neoforge", index.dependencies["neoforge"]!!, context.name)
        } else {
            installVanilla(mcUrl, context.name)
        }

        index.files.forEachIndexed { i, f ->
            onProgress(i, index.files.size)
            val target = context.baseDir!!.resolve("gameFiles").resolve(f.path).apply { parentFile.mkdirs() }
            InstallLogic.downloadFile(client, f.downloads.first(), target.toPath(), f.hashes["sha1"])
            
            // Try to track Modrinth metadata from URL
            val modrinthUrl = f.downloads.find { it.contains("cdn.modrinth.com/data/") }
            if (modrinthUrl != null) {
                val parts = modrinthUrl.split("/")
                // Index of "data" is 3, P7dR8mSH is 4, "versions" is 5, XyZ123 is 6
                if (parts.size >= 7) {
                    val projectId = parts[4]
                    val versionId = parts[6]
                    val projectType = when {
                        f.path.startsWith("resourcepacks/") -> "resourcepack"
                        f.path.startsWith("shaderpacks/") -> "shader"
                        else -> "mod"
                    }
                    MetadataUtils.trackModrinthMetadata(context.baseDir!!, ModrinthAssetMetadata(
                        filename = target.name,
                        versionId = versionId,
                        projectId = projectId,
                        projectType = projectType,
                        relativePath = "gameFiles/${f.path}"
                    ))
                }
            }
        }
        
        val temp = context.baseDir!!.resolve("temp_extract").apply { mkdirs() }
        com.bartixw.anymc.core.utils.ZipUtils.unzip(file, temp)
        listOf("overrides", "client-overrides").forEach { d -> 
            temp.resolve(d).takeIf { it.exists() }?.copyRecursively(context.baseDir!!.resolve("gameFiles"), true) 
        }
        temp.deleteRecursively()
    }
}
