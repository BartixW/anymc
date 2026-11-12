/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data.modrinth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthSearchResult(
    val hits: List<ModrinthSearchHit>,
    @SerialName("total_hits")
    val totalHits: Int
)

@Serializable
data class ModrinthSearchHit(
    @SerialName("project_id") val projectId: String,
    val title: String,
    val description: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val author: String,
    val categories: List<String> = emptyList(),
    val downloads: Int,
    @SerialName("latest_version") val latestVersion: String? = null
)

@Serializable
data class ModrinthProject(
    val id: String,
    val slug: String? = null,
    @SerialName("project_type") val projectType: String? = null,
    @SerialName("team") val teamId: String,
    val title: String,
    val description: String,
    val body: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("game_versions") val gameVersions: List<String>,
    val loaders: List<String>,
    val downloads: Int,
    val followers: Int,
    val published: String,
    val updated: String,
    val categories: List<String> = emptyList(),
    @SerialName("additional_categories") val additionalCategories: List<String> = emptyList(),
    @SerialName("client_side") val clientSide: String? = null,
    @SerialName("server_side") val serverSide: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("issues_url") val issuesUrl: String? = null,
    @SerialName("wiki_url") val wikiUrl: String? = null,
    @SerialName("discord_url") val discordUrl: String? = null,
    @SerialName("donation_urls") val donationUrls: List<ModrinthDonationUrl> = emptyList(),
    val license: ModrinthLicense? = null,
    val gallery: List<ModrinthGalleryImage> = emptyList()
)

@Serializable
data class ModrinthDonationUrl(
    val id: String? = null,
    val platform: String,
    val url: String
)

@Serializable
data class ModrinthLicense(
    val id: String,
    val name: String,
    val url: String? = null
)

@Serializable
data class ModrinthGalleryImage(
    val url: String,
    val featured: Boolean,
    val title: String? = null,
    val description: String? = null,
    val created: String? = null,
    val ordering: Int? = null
)

@Serializable
data class ModrinthTeamMember(
    @SerialName("team_id") val teamId: String,
    val user: ModrinthUser,
    val role: String,
    val accepted: Boolean
)

@Serializable
data class ModrinthUser(
    val id: String,
    val username: String,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class ModrinthVersion(
    val id: String,
    @SerialName("project_id") val projectId: String,
    val name: String,
    @SerialName("version_number") val versionNumber: String,
    val changelog: String? = null,
    @SerialName("version_type") val versionType: String,
    @SerialName("date_published") val datePublished: String,
    val downloads: Int,
    @SerialName("game_versions") val gameVersions: List<String>,
    val loaders: List<String>,
    val dependencies: List<ModrinthDependency> = emptyList(),
    val files: List<ModrinthFile>
)

@Serializable
data class ModrinthDependency(
    @SerialName("version_id") val versionId: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("dependency_type") val dependencyType: String // "required", "optional", "incompatible", "embedded"
)

@Serializable
data class ModrinthFile(
    val url: String,
    val filename: String,
    val hashes: Map<String, String>,
    val primary: Boolean,
    val size: Long
)
