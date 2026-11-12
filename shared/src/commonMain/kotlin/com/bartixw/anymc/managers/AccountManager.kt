/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.managers

import com.bartixw.anymc.minecraft.data.Account
import com.bartixw.anymc.services.AuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountListData(
    @SerialName("WARNING_PLEASE_READ")
    val warning: String = "",
    @SerialName("UWAGA_PROSZĘ_PRZECZYTAĆ")
    val warningPl: String = "",
    val accounts: List<Account> = emptyList(),
    val activeAccountUuid: String? = null
)

object AccountManager : BasePersistentDataManager<AccountListData>(
    fileName = "DO_NOT_SHARE_WITH_ANYONE.accounts.json",
    defaultValue = AccountListData(),
    serializer = AccountListData.serializer()
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val authService = AuthService(MinecraftManager.client)

    fun refreshActiveAccount() {
        val active = activeAccount ?: return
        if (active is Account.Microsoft) {
            managerScope.launch {
                val updated = authService.refreshMicrosoftToken(active.refreshToken)
                if (updated != null) {
                    addAccount(updated)
                    println("Refreshed account: ${updated.username}")
                } else {
                    println("Failed to refresh account: ${active.username}")
                }
            }
        }
    }

    fun addAccount(account: Account) {
        update { current ->
            val updatedAccounts = current.accounts.toMutableList()
            // Replace if already exists
            updatedAccounts.removeAll { it.uuid == account.uuid }
            updatedAccounts.add(account)
            current.copy(
                warning = "This file contains your tokens. DO NOT SHARE ANY CONTENTS OF THIS FILE WITH ANYONE, IT WILL GIVE THE OTHER PERSON ACCESS TO YOUR MINECRAFT ACCOUNT. Close this file if you do not know what are you doing.",
                warningPl = "Ten plik zawiera twoje tokeny. NIE UDOSTĘPNIAJ ŻADNEJ ZAWARTOŚCI TEGO PLIKU NIKOMU, GDYŻ DA TO DOSTĘP TEJ INNEJ OSOBIE DOSTĘP DO TWOJEGO KONTA MINECRAFT. Zamknij ten plik, gdy nie wiesz co dokładnie robisz.",
                accounts = updatedAccounts,
                activeAccountUuid = account.uuid,
            )
        }
    }

    fun setActiveAccount(uuid: String) {
        update { it.copy(activeAccountUuid = uuid) }
    }

    fun removeAccount(uuid: String) {
        val account = data.value.accounts.find { it.uuid == uuid }
        if (account is Account.ElyBy) {
            managerScope.launch {
                authService.invalidateElyBy(account.accessToken, account.clientToken)
            }
        }

        update { current ->
            val updatedAccounts = current.accounts.filterNot { it.uuid == uuid }
            val newActive = if (current.activeAccountUuid == uuid) {
                updatedAccounts.firstOrNull()?.uuid
            } else {
                current.activeAccountUuid
            }
            current.copy(accounts = updatedAccounts, activeAccountUuid = newActive)
        }
    }

    val activeAccount: Account?
        get() = data.value.accounts.find { it.uuid == data.value.activeAccountUuid }
}
