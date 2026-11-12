/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.v2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_ok
import anymc.shared.generated.resources.nbt_cancel
import com.bartixw.anymc.ui.components.McText
import com.bartixw.anymc.ui.components.McTopAppBar
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.ButtonType
import com.bartixw.anymc.ui.components.v2.Checkbox
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Dropdown
import com.bartixw.anymc.ui.components.v2.Input
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.components.v2.TopAppBar
import org.jetbrains.compose.resources.stringResource

@Composable
fun ComponentTestScreen(navController: NavController) {
    var dialogVisible by remember { mutableStateOf(false) }

    if (dialogVisible) Dialog(
        onDismissRequest = { dialogVisible = false },
        title = { Text("Example dialog") },
        content = {
            Text("This is an example dialog.")
        },
        buttons = {
            Button(
                content = { Text(stringResource(Res.string.nbt_cancel)) },
                type = ButtonType.Secondary,
                onClick = { dialogVisible = false },
            )
            Button(
                content = { Text(stringResource(Res.string.common_ok)) },
                type = ButtonType.Primary,
                onClick = { dialogVisible = false },
            )
        }
    )

    Column {
        TopAppBar(
            navController = navController,
            title = { Text("AnyMC UI v2 Component Examples") },
            actions = {}
        )
        Row {
            Column(Modifier.weight(1f).padding(16.dp)) {
                Text("Primary button")
                Button(
                    content = { Text("Click me") },
                    type = ButtonType.Primary,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f).padding(16.dp)) {
                Text("Secondary button")
                Button(
                    content = { Text("Click me") },
                    type = ButtonType.Secondary,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f).padding(16.dp)) {
                Text("Destructive button")
                Button(
                    content = { Text("Click me") },
                    type = ButtonType.Destructive,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Row {
            Column(Modifier.weight(1f).padding(16.dp)) {
                Text("Disabled button")
                Button(
                    content = { Text("Click me") },
                    type = ButtonType.Primary,
                    disabled = true,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f).padding(16.dp)) {
                Text("Dialog")
                Button(
                    content = { Text("Show dialog") },
                    type = ButtonType.Secondary,
                    onClick = {
                        dialogVisible = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f).padding(16.dp)) {
                Text("Dropdown")
                var selected by remember { mutableStateOf("Option 1") }
                val options = listOf("Option 1", "Option 2", "Option 3")

                Dropdown(
                    options = options,
                    selectedOption = selected,
                    onOptionSelected = { selected = it },
                    labelProvider = { it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Row {
            Column(Modifier.weight(1f).padding(16.dp)) {
                Text("Loading indicator")
                LoadingIndicator(
                    modifier = Modifier.size(32.dp)
                )
            }
            Column(Modifier.weight(1f).padding(16.dp)) {
                var value by remember { mutableStateOf("") }

                Text("Input")
                Input(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(Modifier.weight(1f).padding(16.dp)) {
                var checked by remember { mutableStateOf(false) }

                Text("Checkbox")
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it }
                )
            }
        }
    }
}