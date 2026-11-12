/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.utils

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.onModExternalDrag(
    onDragStart: () -> Unit,
    onDragExit: () -> Unit,
    onDrop: (uris: List<String>) -> Unit
): Modifier {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragExit by rememberUpdatedState(onDragExit)
    val currentOnDrop by rememberUpdatedState(onDrop)

    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                currentOnDragStart()
            }

            override fun onExited(event: DragAndDropEvent) {
                currentOnDragExit()
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val data = event.dragData()
                if (data is DragData.FilesList) {
                    currentOnDrop(data.readFiles())
                    return true
                }
                return false
            }
        }
    }

    return this.dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            event.dragData() is DragData.FilesList
        },
        target = target
    )
}
