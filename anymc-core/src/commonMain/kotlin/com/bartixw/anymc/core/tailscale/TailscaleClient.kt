/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.tailscale

import kotlinx.serialization.json.Json

data class CommandResult(
    val exitCode: Int,
    val output: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

object TailscaleClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun execute(vararg args: String): CommandResult {
        val (clientBin, _) = TailscaleDownloader.getBinaries()
        val socketPath = TailscaledProcessManager.socketFile.absolutePath

        val command = mutableListOf(clientBin.absolutePath, "--socket=$socketPath")
        command.addAll(args)

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        return CommandResult(exitCode, output)
    }

    fun status(): Status? {
        val result = execute("status", "--json")
        if (!result.isSuccess || result.output.isBlank()) return null
        return try {
            json.decodeFromString<Status>(result.output)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun login(): String? {
        val result = execute("login")
        return extractAuthUrl(result.output)
    }

    fun up(): String? {
        val result = execute("up")
        return extractAuthUrl(result.output)
    }

    fun logout(): CommandResult {
        return execute("logout")
    }

    fun down(): CommandResult {
        return execute("down")
    }

    fun serve(port: Int): CommandResult {
        return execute("serve", "--bg", "$port", "http://127.0.0.1:$port")
    }

    private fun extractAuthUrl(output: String): String? {
        val regex = Regex("https://[\\w./?=-]+login\\.tailscale\\.com[\\w./?=-]*")
        val match = regex.find(output)
        return match?.value ?: output.lines().find { it.contains("https://") }?.trim()
    }
}
