/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.services

import com.bartixw.anymc.minecraft.data.modrinth.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class VersionFilesUpdateBody(
    val hashes: List<String>,
    val algorithm: String = "sha1",
    val loaders: List<String> = emptyList(),
    @SerialName("game_versions")
    val gameVersions: List<String> = emptyList()
)

class ModrinthService(private val client: HttpClient) {
    private val baseUrl = "https://api.modrinth.com/v2"

    suspend fun getLatestVersionsFromHashes(
        hashes: List<String>,
        algorithm: String = "sha1",
        loaders: List<String> = emptyList(),
        gameVersions: List<String> = emptyList()
    ): Map<String, ModrinthVersion> = withContext(Dispatchers.IO) {
        if (hashes.isEmpty()) return@withContext emptyMap()
        client.post("$baseUrl/version_files/update") {
            contentType(ContentType.Application.Json)
            setBody(VersionFilesUpdateBody(hashes, algorithm, loaders, gameVersions))
            header(HttpHeaders.UserAgent, "AnyMC Launcher")
        }.body()
    }

    suspend fun searchMods(
        query: String,
        loaders: List<String> = emptyList(),
        gameVersions: List<String> = emptyList(),
        limit: Int = 20,
        offset: Int = 0,
        index: String = "relevance",
        projectType: String = "mod",
        category: String? = null
    ): ModrinthSearchResult = withContext(Dispatchers.IO) {
        val facets = mutableListOf<String>()
        if (loaders.isNotEmpty()) {
            val loaderFacet = loaders.joinToString(",") { "\"categories:$it\"" }
            facets.add("[$loaderFacet]")
        }
        if (gameVersions.isNotEmpty()) {
            val versionFacet = gameVersions.joinToString(",") { "\"versions:$it\"" }
            facets.add("[$versionFacet]")
        }
        if (category != null) {
            facets.add("[\"categories:$category\"]")
        }
        facets.add("[\"project_type:$projectType\"]")

        val facetsString = if (facets.isNotEmpty()) "[${facets.joinToString(",")}]" else null

        client.get("$baseUrl/search") {
            parameter("query", query)
            if (facetsString != null) parameter("facets", facetsString)
            parameter("limit", limit)
            parameter("offset", offset)
            parameter("index", index)
            header(HttpHeaders.UserAgent, "AnyMC Launcher")
        }.body()
    }

    suspend fun getProject(projectId: String): ModrinthProject = withContext(Dispatchers.IO) {
        client.get("$baseUrl/project/$projectId") {
            header(HttpHeaders.UserAgent, "AnyMC Launcher")
        }.body()
    }

    suspend fun getProjects(projectIds: List<String>): List<ModrinthProject> = withContext(Dispatchers.IO) {
        if (projectIds.isEmpty()) return@withContext emptyList()
        client.get("$baseUrl/projects") {
            parameter("ids", Json.encodeToString(projectIds))
            header(HttpHeaders.UserAgent, "AnyMC Launcher")
        }.body()
    }

    suspend fun getProjectTeam(teamId: String): List<ModrinthTeamMember> = withContext(Dispatchers.IO) {
        client.get("$baseUrl/team/$teamId/members") {
            header(HttpHeaders.UserAgent, "AnyMC Launcher")
        }.body()
    }

    suspend fun getProjectVersions(
        projectId: String,
        loaders: List<String> = emptyList(),
        gameVersions: List<String> = emptyList()
    ): List<ModrinthVersion> = withContext(Dispatchers.IO) {
        client.get("$baseUrl/project/$projectId/version") {
            if (loaders.isNotEmpty()) parameter("loaders", Json.encodeToString(loaders))
            if (gameVersions.isNotEmpty()) parameter("game_versions", Json.encodeToString(gameVersions))
            header(HttpHeaders.UserAgent, "AnyMC Launcher")
        }.body()
    }

    suspend fun getVersion(versionId: String): ModrinthVersion = withContext(Dispatchers.IO) {
        client.get("$baseUrl/version/$versionId") {
            header(HttpHeaders.UserAgent, "AnyMC Launcher")
        }.body()
    }
}

private object Json {
    fun encodeToString(list: List<String>): String {
        return "[\"${list.joinToString("\",\"")}\"]"
    }
}
