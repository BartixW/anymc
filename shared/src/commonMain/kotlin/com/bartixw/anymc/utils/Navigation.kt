/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.utils

import androidx.navigation.NavBackStackEntry
import java.net.URI

fun NavBackStackEntry.getQueryParam(paramName: String): String? {
    val route = this.destination.route ?: return null
    return try {
        val uri = URI("app://$route")

        uri.query?.split("&")
            ?.map { it.split("=") }
            ?.firstOrNull { it.size == 2 && it[0] == paramName }
            ?.get(1)
    } catch (e: Exception) {
        println(e)
        "$e"
    }
}