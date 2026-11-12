/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.tailscale

import com.bartixw.anymc.core.utils.PlatformUtils
import java.io.File
import java.util.concurrent.atomic.AtomicReference

object TailscaledProcessManager {

    private val processRef = AtomicReference<Process?>(null)

    val stateDir: File
        get() = File(PlatformUtils.userDataDirectory, "tailscale-state")

    val socketFile: File
        get() = File(stateDir, "tailscaled.sock")

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
    }

    @Synchronized
    fun start(): Process {
        if (isRunning()) {
            return processRef.get()!!
        }

        if (!stateDir.exists()) {
            stateDir.mkdirs()
        }

        val (_, daemonBin) = TailscaleDownloader.getBinaries()
        if (!daemonBin.exists()) {
            throw IllegalStateException("Tailscale daemon binary not found at ${daemonBin.absolutePath}. Please download binaries first.")
        }

        val command = listOf(
            daemonBin.absolutePath,
            "--tun=userspace-networking",
            "--socks5-server=127.0.0.1:1055",
            "--socket=${socketFile.absolutePath}",
            "--statedir=${stateDir.absolutePath}"
        )

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        processRef.set(process)
        return process
    }

    @Synchronized
    fun stop() {
        processRef.getAndSet(null)?.let { process ->
            try {
                process.destroy()
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    process.destroyForcibly()
                } catch (ignored: Exception) {}
            }
        }
    }

    fun isRunning(): Boolean {
        val process = processRef.get() ?: return false
        return process.isAlive
    }
}
