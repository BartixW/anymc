/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.tailscale

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.kotlinx.json.json as clientJson
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.CIO
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.*
import java.net.InetSocketAddress
import java.net.Proxy

object TailscaleKtor {

    /**
     * Creates a pre-configured Ktor HttpClient using the local Tailscale SOCKS5 proxy (127.0.0.1:1055).
     */
    fun createHttpClient(): HttpClient {
        return HttpClient(ClientCIO) {
            engine {
                proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 1055))
            }
            install(ClientContentNegotiation) {
                clientJson()
            }
        }
    }

    /**
     * Creates a Ktor embedded server template for hosting APIs listenable via Tailscale.
     * Automatically registers with Tailscale serve via `TailscaleClient.serve(port)` upon startup.
     */
    fun createTailscaleServer(
        port: Int,
        configureRouting: Routing.() -> Unit
    ) = embeddedServer(CIO, port = port, host = "127.0.0.1") {
        install(ContentNegotiation) {
            json()
        }
        routing {
            configureRouting()
        }
    }.apply {
        start(wait = false)
        try {
            TailscaleClient.serve(port)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
