/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Modifier.onModExternalDrag(
    onDragStart: () -> Unit,
    onDragExit: () -> Unit,
    onDrop: (uris: List<String>) -> Unit
): Modifier
