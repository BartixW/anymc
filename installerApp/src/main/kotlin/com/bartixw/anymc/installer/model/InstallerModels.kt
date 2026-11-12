/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.installer.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    val size: Long = 0L,
    @SerialName("content_type")
    val contentType: String? = null
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    val prerelease: Boolean = false,
    @SerialName("published_at")
    val publishedAt: String? = null,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

data class InstallerConfig(
    val repoOwner: String = "BartixW",
    val repoName: String = "AnyMC",
    val installDirectory: File,
    val createDesktopShortcut: Boolean = true,
    val createStartMenuShortcut: Boolean = true,
    val registerUriScheme: Boolean = true,
    val uriScheme: String = "anymc",
    val selectedRelease: GitHubRelease? = null,
    val localZipFile: File? = null
)

sealed interface InstallationState {
    data object Idle : InstallationState
    data object FetchingReleases : InstallationState
    data class ReleasesLoaded(val releases: List<GitHubRelease>, val selectedRelease: GitHubRelease?) : InstallationState
    data class Downloading(val release: GitHubRelease, val assetName: String, val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : InstallationState
    data class Extracting(val fileName: String) : InstallationState
    data object RegisteringSystem : InstallationState
    data object RegisteringUriScheme : InstallationState
    data class Completed(val installedPath: File, val executablePath: File?) : InstallationState
    data class Error(val message: String, val cause: Throwable? = null) : InstallationState
}
