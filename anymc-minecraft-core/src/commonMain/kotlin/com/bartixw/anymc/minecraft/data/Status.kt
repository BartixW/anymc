/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

enum class StatusType {
    INFO_FETCH,
    INFO_WRITE,
    CLIENT_DL,
    LIBRARY_DL,
    ASSET_FETCH,
    ASSET_DL,
    JAVA_FETCH,
    JAVA_INSTALL,
    MODPACK_DL,
    EXTRACT,
    IDLE
}

data class DownloadStatus(
    val type: StatusType,
    val current: Int,
    val total: Int,
    val errorCode: String? = null
)

enum class MinecraftStatus {
    READY,
    LAUNCHING,
    LAUNCHED,
    FAILED,
    KILLING
}
