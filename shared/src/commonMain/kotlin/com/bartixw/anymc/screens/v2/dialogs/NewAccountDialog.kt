/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.v2.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_back
import anymc.shared.generated.resources.common_error
import anymc.shared.generated.resources.common_next
import anymc.shared.generated.resources.nbt_cancel
import anymc.shared.generated.resources.profile_add_account
import anymc.shared.generated.resources.profile_add_account_add_button
import anymc.shared.generated.resources.profile_add_account_elyby_email
import anymc.shared.generated.resources.profile_add_account_elyby_password
import anymc.shared.generated.resources.profile_add_account_ms_status
import anymc.shared.generated.resources.profile_add_account_ms_waiting
import anymc.shared.generated.resources.profile_add_account_offline_username
import anymc.shared.generated.resources.profile_add_account_type_elyby
import anymc.shared.generated.resources.profile_add_account_type_ms
import anymc.shared.generated.resources.profile_add_account_type_offline
import anymc.shared.generated.resources.profile_add_account_type_title
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Dropdown
import com.bartixw.anymc.ui.components.v2.Input
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun NewAccountDialog(
    onDismissRequest: () -> Unit
) {
    var secondStage by remember { mutableStateOf(false) }
    val viewModel: NewAccountDialogViewModel = viewModel { NewAccountDialogViewModel() }

    val msLabel = stringResource(Res.string.profile_add_account_type_ms)
    val elyLabel = stringResource(Res.string.profile_add_account_type_elyby)
    val offlineLabel = stringResource(Res.string.profile_add_account_type_offline)

    val accountTypes = remember(msLabel, elyLabel, offlineLabel) {
        mapOf(
            AccountType.MICROSOFT to msLabel,
            AccountType.ELYBY to elyLabel,
            AccountType.OFFLINE to offlineLabel
        )
    }

    Dialog(
        onDismissRequest = {
            viewModel.reset()
            onDismissRequest()
        },
        title = { Text(stringResource(Res.string.profile_add_account)) },
        buttons = {
            if (secondStage) {
                Button(
                    content = { Text(stringResource(Res.string.common_back)) },
                    onClick = { 
                        secondStage = false 
                        viewModel.reset()
                    },
                    disabled = viewModel.isHelperRunning
                )
                if (viewModel.accountType != AccountType.MICROSOFT) {
                    Button(
                        content = { Text(stringResource(Res.string.profile_add_account_add_button)) },
                        type = ButtonType.Primary,
                        disabled = viewModel.isHelperRunning || when (viewModel.accountType) {
                            AccountType.ELYBY -> viewModel.elyByUsername.isBlank() || viewModel.elyByPassword.isBlank()
                            AccountType.OFFLINE -> viewModel.offlineUsername.isBlank()
                            else -> false
                        },
                        onClick = {
                            when (viewModel.accountType) {
                                AccountType.ELYBY -> viewModel.loginElyBy(onDismissRequest)
                                AccountType.OFFLINE -> viewModel.loginOffline(onDismissRequest)
                                else -> {}
                            }
                        }
                    )
                }
            } else {
                Button(
                    content = { Text(stringResource(Res.string.nbt_cancel)) },
                    onClick = onDismissRequest
                )
                Button(
                    content = { Text(stringResource(Res.string.common_next)) },
                    type = ButtonType.Primary,
                    onClick = { secondStage = true }
                )
            }
        }
    ) {
        if (secondStage) {
            when (viewModel.accountType) {
                AccountType.MICROSOFT -> {
                    LaunchedEffect(Unit) {
                        viewModel.loginMicrosoft(onDismissRequest)
                    }

                    if (viewModel.isHelperRunning) {
                        LoadingIndicator()
                        Spacer(Modifier.size(16.dp))
                        Text(stringResource(Res.string.profile_add_account_ms_waiting))
                    }
                    
                    viewModel.helperStatus?.let { statusRes ->
                        Text(stringResource(Res.string.profile_add_account_ms_status, stringResource(statusRes)))
                    }
                }
                AccountType.ELYBY -> {
                    Text(stringResource(Res.string.profile_add_account_elyby_email))
                    Input(
                        value = viewModel.elyByUsername,
                        onValueChange = { viewModel.elyByUsername = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.profile_add_account_elyby_password))
                    Input(
                        value = viewModel.elyByPassword,
                        onValueChange = { viewModel.elyByPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
                AccountType.OFFLINE -> {
                    Text(stringResource(Res.string.profile_add_account_offline_username))
                    Input(
                        value = viewModel.offlineUsername,
                        onValueChange = { viewModel.offlineUsername = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            viewModel.error?.let { errorRes ->
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(Res.string.common_error, stringResource(errorRes)),
                    style = TextStyle(color = MinecraftTheme.colors.genericDestructive)
                )
            }
            
            viewModel.errorDetail?.let { detail ->
                Spacer(Modifier.size(4.dp))
                Text(
                    text = stringResource(Res.string.common_error, detail),
                    style = TextStyle(color = MinecraftTheme.colors.genericDestructive)
                )
            }
        } else {
            Text(stringResource(Res.string.profile_add_account_type_title))
            Dropdown(
                options = accountTypes.keys.toList(),
                selectedOption = viewModel.accountType,
                onOptionSelected = { 
                    viewModel.accountType = it
                    viewModel.reset()
                },
                labelProvider = { accountTypes[it]!! },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
