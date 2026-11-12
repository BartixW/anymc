/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.services

import com.bartixw.anymc.minecraft.data.Account
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.util.UUID

class AuthService(private val client: HttpClient) {

    private val CLIENT_ID = "00000000402b5328"
    private val REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf"
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val ELYBY_AUTH_SERVER = "https://authserver.ely.by"

    fun getAuthUrl(): String {
        return "https://login.live.com/oauth20_authorize.srf" +
                "?client_id=$CLIENT_ID" +
                "&response_type=code" +
                "&scope=XboxLive.signin%20offline_access" +
                "&redirect_uri=$REDIRECT_URI" +
                "&display=touch" +
                "&locale=en"
    }

    suspend fun loginWithCode(code: String): Account.Microsoft? = withContext(Dispatchers.IO) {
        try {
            println("AuthService: Starting login with code")
            val msaToken = exchangeCodeForToken(code) ?: run {
                println("AuthService: exchangeCodeForToken failed")
                return@withContext null
            }
            
            println("AuthService: Authenticating with XBL")
            val xblResponse = authenticateWithXBL(msaToken.accessToken) ?: run {
                println("AuthService: authenticateWithXBL failed")
                return@withContext null
            }
            
            println("AuthService: Authenticating with XSTS")
            val xstsResponse = authenticateWithXSTS(xblResponse.token) ?: run {
                println("AuthService: authenticateWithXSTS failed")
                return@withContext null
            }
            
            println("AuthService: Logging into Minecraft")
            val mcLoginResponse = loginWithMinecraft(xstsResponse.token, xstsResponse.userHash) ?: run {
                println("AuthService: loginWithMinecraft failed")
                return@withContext null
            }
            
            println("AuthService: Checking entitlements")
            val hasLicense = checkMinecraftEntitlements(mcLoginResponse.accessToken)
            
            println("AuthService: Getting Minecraft profile")
            val profile = getMinecraftProfile(mcLoginResponse.accessToken) ?: run {
                println("AuthService: getMinecraftProfile failed")
                return@withContext null
            }

            println("AuthService: Login successful for ${profile.name}")
            Account.Microsoft(
                username = profile.name,
                uuid = profile.id,
                accessToken = mcLoginResponse.accessToken,
                refreshToken = msaToken.refreshToken ?: "",
                expiresAt = System.currentTimeMillis() + (msaToken.expiresIn * 1000),
                isDemo = !hasLicense,
                xuid = xstsResponse.xuid
            )
        } catch (e: Exception) {
            println("AuthService: Exception in loginWithCode: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun refreshMicrosoftToken(refreshToken: String): Account.Microsoft? = withContext(Dispatchers.IO) {
        try {
            println("AuthService: Starting token refresh")
            val msaToken = exchangeRefreshTokenForToken(refreshToken) ?: run {
                println("AuthService: exchangeRefreshTokenForToken failed")
                return@withContext null
            }
            
            println("AuthService: Authenticating with XBL (refresh)")
            val xblResponse = authenticateWithXBL(msaToken.accessToken) ?: run {
                println("AuthService: authenticateWithXBL (refresh) failed")
                return@withContext null
            }
            
            println("AuthService: Authenticating with XSTS (refresh)")
            val xstsResponse = authenticateWithXSTS(xblResponse.token) ?: run {
                println("AuthService: authenticateWithXSTS (refresh) failed")
                return@withContext null
            }
            
            println("AuthService: Logging into Minecraft (refresh)")
            val mcLoginResponse = loginWithMinecraft(xstsResponse.token, xstsResponse.userHash) ?: run {
                println("AuthService: loginWithMinecraft (refresh) failed")
                return@withContext null
            }
            
            println("AuthService: Checking entitlements (refresh)")
            val hasLicense = checkMinecraftEntitlements(mcLoginResponse.accessToken)
            
            println("AuthService: Getting Minecraft profile (refresh)")
            val profile = getMinecraftProfile(mcLoginResponse.accessToken) ?: run {
                println("AuthService: getMinecraftProfile (refresh) failed")
                return@withContext null
            }

            println("AuthService: Token refresh successful for ${profile.name}")
            Account.Microsoft(
                username = profile.name,
                uuid = profile.id,
                accessToken = mcLoginResponse.accessToken,
                refreshToken = msaToken.refreshToken ?: refreshToken,
                expiresAt = System.currentTimeMillis() + (msaToken.expiresIn * 1000),
                isDemo = !hasLicense,
                xuid = xstsResponse.xuid
            )
        } catch (e: Exception) {
            println("AuthService: Exception in refreshMicrosoftToken: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun loginOffline(username: String): Account.Offline {
        val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray()).toString().replace("-", "")
        return Account.Offline(username, uuid)
    }

    suspend fun loginWithElyByPassword(username: String, password: String): Account.ElyBy? = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            putJsonObject("agent") {
                put("name", "Minecraft")
                put("version", 1)
            }
            put("username", username)
            put("password", password)
            put("requestUser", true)
        }

        try {
            val response = client.post("$ELYBY_AUTH_SERVER/auth/authenticate") {
                contentType(ContentType.Application.Json)
                header("User-Agent", USER_AGENT)
                setBody(payload)
            }

            if (response.status != HttpStatusCode.OK) {
                println("AuthService: Ely.by authentication failed with status ${response.status}")
                println("AuthService: Response body: ${response.bodyAsText()}")
                return@withContext null
            }

            val authResponse: ElyByAuthenticateResponse = response.body()
            val profile = authResponse.selectedProfile ?: authResponse.availableProfiles?.firstOrNull()
                ?: return@withContext null

            println("AuthService: Ely.by login successful for ${profile.name}")
            Account.ElyBy(
                username = profile.name,
                uuid = profile.id,
                accessToken = authResponse.accessToken,
                clientToken = authResponse.clientToken
            )
        } catch (e: Exception) {
            println("AuthService: Error in loginWithElyByPassword: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun invalidateElyBy(accessToken: String, clientToken: String?): Boolean = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("accessToken", accessToken)
            if (clientToken != null) {
                put("clientToken", clientToken)
            }
        }

        try {
            val response = client.post("$ELYBY_AUTH_SERVER/auth/invalidate") {
                contentType(ContentType.Application.Json)
                header("User-Agent", USER_AGENT)
                setBody(payload)
            }
            if (response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK) {
                println("AuthService: Ely.by session invalidated successfully")
                true
            } else {
                println("AuthService: Ely.by session invalidation failed with status ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("AuthService: Error in invalidateElyBy: ${e.message}")
            false
        }
    }

    private suspend fun exchangeCodeForToken(code: String): MSATokenResponse? {
        return try {
            val trimmedCode = code.trim()
            println("AuthService: exchangeCodeForToken with code length: ${trimmedCode.length}, starts with: ${trimmedCode.take(10)}...")
            val response = client.submitForm(
                url = "https://login.live.com/oauth20_token.srf",
                formParameters = parameters {
                    append("client_id", CLIENT_ID)
                    append("code", trimmedCode)
                    append("grant_type", "authorization_code")
                    append("redirect_uri", REDIRECT_URI)
                    append("scope", "XboxLive.signin offline_access")
                }
            ) {
                header("User-Agent", USER_AGENT)
            }
            if (response.status != HttpStatusCode.OK) {
                println("AuthService: exchangeCodeForToken failed with status ${response.status}")
                println("AuthService: Response body: ${response.bodyAsText()}")
                return null
            }
            response.body()
        } catch (e: Exception) {
            println("AuthService: Error in exchangeCodeForToken: ${e.message}")
            null
        }
    }

    private suspend fun exchangeRefreshTokenForToken(refreshToken: String): MSATokenResponse? {
        return try {
            println("AuthService: exchangeRefreshTokenForToken")
            val response = client.submitForm(
                url = "https://login.live.com/oauth20_token.srf",
                formParameters = parameters {
                    append("client_id", CLIENT_ID)
                    append("refresh_token", refreshToken)
                    append("grant_type", "refresh_token")
                    append("redirect_uri", REDIRECT_URI)
                    append("scope", "XboxLive.signin offline_access")
                }
            ) {
                header("User-Agent", USER_AGENT)
            }
            if (response.status != HttpStatusCode.OK) {
                println("AuthService: exchangeRefreshTokenForToken failed with status ${response.status}")
                println("AuthService: Response body: ${response.bodyAsText()}")
                return null
            }
            response.body()
        } catch (e: Exception) {
            println("AuthService: Error in exchangeRefreshTokenForToken: ${e.message}")
            null
        }
    }

    private suspend fun authenticateWithXBL(msaAccessToken: String): XBLResponse? {
        println("AuthService: authenticateWithXBL with token length: ${msaAccessToken.length}")
        println("AuthService: Token starts with eyJ: ${msaAccessToken.startsWith("eyJ")}")
        val payload = buildJsonObject {
            putJsonObject("Properties") {
                put("AuthMethod", "RPS")
                put("SiteName", "user.auth.xboxlive.com")
                put("RpsTicket", "d=$msaAccessToken")
            }
            put("RelyingParty", "http://auth.xboxlive.com")
            put("TokenType", "JWT")
        }

        return try {
            val response = client.post("https://user.auth.xboxlive.com/user/authenticate") {
                contentType(ContentType.Application.Json)
                header("Accept", "application/json")
                header("x-xbl-contract-version", "1")
                header("User-Agent", USER_AGENT)
                setBody(payload)
            }
            if (response.status != HttpStatusCode.OK) {
                println("AuthService: authenticateWithXBL failed with status ${response.status}")
                println("AuthService: Response body: ${response.bodyAsText()}")
                return null
            }
            response.body()
        } catch (e: Exception) {
            println("AuthService: Error in authenticateWithXBL: ${e.message}")
            null
        }
    }

    private suspend fun authenticateWithXSTS(xblToken: String): XSTSResponse? {
        val payload = buildJsonObject {
            putJsonObject("Properties") {
                put("SandboxId", "RETAIL")
                putJsonArray("UserTokens") {
                    add(xblToken)
                }
            }
            put("RelyingParty", "rp://api.minecraftservices.com/")
            put("TokenType", "JWT")
        }

        return try {
            val response = client.post("https://xsts.auth.xboxlive.com/xsts/authorize") {
                contentType(ContentType.Application.Json)
                header("Accept", "application/json")
                header("x-xbl-contract-version", "1")
                header("User-Agent", USER_AGENT)
                setBody(payload)
            }
            if (response.status != HttpStatusCode.OK) {
                println("AuthService: authenticateWithXSTS failed with status ${response.status}")
                println("AuthService: Response body: ${response.bodyAsText()}")
                return null
            }
            response.body()
        } catch (e: Exception) {
            println("AuthService: Error in authenticateWithXSTS: ${e.message}")
            null
        }
    }

    private suspend fun loginWithMinecraft(xstsToken: String, userHash: String): MinecraftLoginResponse? {
        val payload = buildJsonObject {
            put("identityToken", "XBL3.0 x=$userHash;$xstsToken")
        }

        return try {
            val response = client.post("https://api.minecraftservices.com/authentication/login_with_xbox") {
                contentType(ContentType.Application.Json)
                header("User-Agent", USER_AGENT)
                setBody(payload)
            }
            if (response.status != HttpStatusCode.OK) {
                println("AuthService: loginWithMinecraft failed with status ${response.status}")
                println("AuthService: Response body: ${response.bodyAsText()}")
                return null
            }
            response.body()
        } catch (e: Exception) {
            println("AuthService: Error in loginWithMinecraft: ${e.message}")
            null
        }
    }

    private suspend fun checkMinecraftEntitlements(mcAccessToken: String): Boolean {
        return try {
            val response = client.get("https://api.minecraftservices.com/entitlements/mcstore") {
                header("Authorization", "Bearer $mcAccessToken")
                header("User-Agent", USER_AGENT)
            }
            if (response.status != HttpStatusCode.OK) {
                println("AuthService: checkMinecraftEntitlements failed with status ${response.status}")
                println("AuthService: Response body: ${response.bodyAsText()}")
                return false
            }
            val jsonResponse: JsonObject = response.body()

            val items = jsonResponse["items"]?.jsonArray
            items?.any { it.jsonObject["name"]?.jsonPrimitive?.content == "game_minecraft" } ?: false
        } catch (e: Exception) {
            println("AuthService: Error in checkMinecraftEntitlements: ${e.message}")
            false
        }
    }

    private suspend fun getMinecraftProfile(mcAccessToken: String): MinecraftProfileResponse? {
        return try {
            val response = client.get("https://api.minecraftservices.com/minecraft/profile") {
                header("Authorization", "Bearer $mcAccessToken")
                header("User-Agent", USER_AGENT)
            }
            if (response.status != HttpStatusCode.OK) {
                println("AuthService: getMinecraftProfile failed with status ${response.status}")
                println("AuthService: Response body: ${response.bodyAsText()}")
                return null
            }
            response.body()
        } catch (e: Exception) {
            println("AuthService: Error in getMinecraftProfile: ${e.message}")
            null
        }
    }

    @Serializable
    private data class MSATokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long
    )

    @Serializable
    private data class XBLResponse(
        @SerialName("Token") val token: String
    )

    @Serializable
    private data class XSTSResponse(
        @SerialName("Token") val token: String,
        @SerialName("DisplayClaims") val displayClaims: JsonObject
    ) {
        val userHash: String get() = displayClaims["xui"]?.jsonArray?.get(0)?.jsonObject?.get("uhs")?.jsonPrimitive?.content ?: ""
        val xuid: String get() = displayClaims["xui"]?.jsonArray?.get(0)?.jsonObject?.get("xid")?.jsonPrimitive?.content ?: ""
    }

    @Serializable
    private data class MinecraftLoginResponse(
        @SerialName("access_token") val accessToken: String
    )

    @Serializable
    data class MinecraftProfileResponse(
        val id: String,
        val name: String
    )

    @Serializable
    private data class ElyByAuthenticateResponse(
        val accessToken: String,
        val clientToken: String? = null,
        val availableProfiles: List<ElyByProfile>? = null,
        val selectedProfile: ElyByProfile? = null
    )

    @Serializable
    private data class ElyByProfile(
        val id: String,
        val name: String
    )
}
