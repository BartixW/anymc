/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.screens.main.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import anymc.shared.generated.resources.Res
import anymc.shared.generated.resources.common_delete
import anymc.shared.generated.resources.common_error
import anymc.shared.generated.resources.ic_image_unknown
import anymc.shared.generated.resources.screenshots_count
import anymc.shared.generated.resources.screenshots_empty
import anymc.shared.generated.resources.versions_world_gamemode_label
import anymc.shared.generated.resources.worlds_select_installation
import com.bartixw.anymc.managers.MinecraftManager
import com.bartixw.anymc.minecraft.data.GameMode
import com.bartixw.anymc.minecraft.data.QuickPlay
import com.bartixw.anymc.minecraft.data.QuickPlayType
import com.bartixw.anymc.screens.versions.VersionsViewModel
import com.bartixw.anymc.ui.components.v2.Button
import com.bartixw.anymc.ui.components.v2.Dialog
import com.bartixw.anymc.ui.components.v2.Icon
import com.bartixw.anymc.ui.components.v2.LoadingIndicator
import com.bartixw.anymc.ui.components.v2.Text
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.bartixw.anymc.utils.DateTimeFormatterUtil
import io.github.vinceglb.filekit.core.FileKit
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import anymc.shared.generated.resources.common_close
import anymc.shared.generated.resources.common_copy
import anymc.shared.generated.resources.main_error_crash_copy
import anymc.shared.generated.resources.nbt_save
import com.bartixw.anymc.ui.components.v2.ButtonType
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun ScreenshotsTab(
    viewModel: VersionsViewModel
) {
    val screenshotsGridState = rememberLazyGridState()
    val bitmapCache = remember { mutableMapOf<String, ImageBitmap>() }
    var selectedScreenshot by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadScreenshots(viewModel.selectedInstallationId!!)
    }

    if (selectedScreenshot != null) {
        ScreenshotDetailDialog(
            file = selectedScreenshot!!,
            cache = bitmapCache,
            onDismiss = { selectedScreenshot = null },
            onDeleted = {
                viewModel.loadScreenshots(viewModel.selectedInstallationId!!)
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (viewModel.selectedInstallationId == null) {
            Text(
                text = stringResource(Res.string.worlds_select_installation),
                style = MinecraftTheme.typographyV2
            )
            return@Row
        }

        when (val screenshotsState = viewModel.screenshotsState) {
            VersionsViewModel.ScreenshotsState.Loading -> Box(Modifier.fillMaxSize()) {
                LoadingIndicator(Modifier.align(Alignment.Center))
            }
            VersionsViewModel.ScreenshotsState.Empty -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.screenshots_empty))
            }
            is VersionsViewModel.ScreenshotsState.Error -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(
                        Res.string.common_error,
                        screenshotsState.message ?: ""
                    )
                )
            }
            is VersionsViewModel.ScreenshotsState.Loaded -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 350.dp),
                            state = screenshotsGridState,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = screenshotsState.screenshots,
                                key = { it.absolutePath }
                            ) { screenshot ->
                                ScreenshotsListItem(
                                    screenshot = screenshot,
                                    cache = bitmapCache,
                                    onClick = { selectedScreenshot = screenshot }
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(Res.string.screenshots_count, screenshotsState.screenshots.size),
                                style = TextStyle(color = MinecraftTheme.colors.textSecondary)
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(screenshotsGridState),
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun rememberAsyncImageBitmap(path: String?, cache: MutableMap<String, ImageBitmap>): ImageBitmap? {
    var bitmap by remember(path) { mutableStateOf(path?.let { cache[it] }) }
    LaunchedEffect(path) {
        if (path != null && bitmap == null) {
            val cached = cache[path]
            if (cached != null) {
                bitmap = cached
            } else {
                val loaded = withContext(Dispatchers.IO) {
                    try {
                        val file = File(path)
                        if (file.exists()) {
                            file.readBytes().decodeToImageBitmap()
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
                if (loaded != null) {
                    cache[path] = loaded
                    bitmap = loaded
                }
            }
        }
    }
    return bitmap
}

@Composable
fun ScreenshotsListItem(
    screenshot: File,
    cache: MutableMap<String, ImageBitmap>,
    onClick: () -> Unit
) {
    val bitmap = rememberAsyncImageBitmap(screenshot.absolutePath, cache)
    val dateStr = DateTimeFormatterUtil.formatDateTime(
        Instant.ofEpochMilli(screenshot.lastModified())
            .atZone(ZoneId.systemDefault())
    )

    Column(
        Modifier
            .height(IntrinsicSize.Max)
            .border(2.dp, MinecraftTheme.colors.dialogBorder)
            .background(MinecraftTheme.colors.dialogBackground)
            .clickable(onClick = onClick)
    ) {
        if (bitmap != null) Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            contentScale = ContentScale.Crop
        ) else Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            Icon(
                resource = Res.drawable.ic_image_unknown,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(32.dp)
            )
        }
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(dateStr, style = TextStyle(color = MinecraftTheme.colors.textSecondary))
            if (screenshot.nameWithoutExtension.endsWith("4K")) Text(
                text = "4K",
                style = TextStyle(color = MinecraftTheme.colors.textSecondary),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MinecraftTheme.colors.textSecondary,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(4.dp, 0.dp)
            )
        }
    }
}

@Composable
fun ScreenshotDetailDialog(
    file: File,
    cache: MutableMap<String, ImageBitmap>,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val bitmap = cache[file.absolutePath] ?: rememberAsyncImageBitmap(file.absolutePath, cache)

    val clampOffsets = { currentScale: Float, curX: Float, curY: Float ->
        if (currentScale <= 1f) {
            0f to 0f
        } else {
            val maxOffsetX = (currentScale - 1f) * containerWidth / 2f
            val maxOffsetY = (currentScale - 1f) * containerHeight / 2f
            curX.coerceIn(-maxOffsetX, maxOffsetX) to curY.coerceIn(-maxOffsetY, maxOffsetY)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        title = { Text(file.name) },
        buttons = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    content = { Text(stringResource(Res.string.common_delete)) },
                    onClick = {
                        try {
                            file.delete()
                            cache.remove(file.absolutePath)
                            onDeleted()
                            onDismiss()
                        } catch (_: Exception) {}
                    },
                    type = ButtonType.Destructive
                )
                Button(
                    content = { Text(stringResource(Res.string.common_copy)) },
                    onClick = {
                        try {
                            val bufferedImage = javax.imageio.ImageIO.read(file)
                            if (bufferedImage != null) {
                                val transferable = object : java.awt.datatransfer.Transferable {
                                    override fun getTransferDataFlavors() = arrayOf(java.awt.datatransfer.DataFlavor.imageFlavor)
                                    override fun isDataFlavorSupported(flavor: java.awt.datatransfer.DataFlavor) = flavor == java.awt.datatransfer.DataFlavor.imageFlavor
                                    override fun getTransferData(flavor: java.awt.datatransfer.DataFlavor): Any {
                                        if (flavor == java.awt.datatransfer.DataFlavor.imageFlavor) return bufferedImage
                                        throw java.awt.datatransfer.UnsupportedFlavorException(flavor)
                                    }
                                }
                                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
                            }
                        } catch (_: Exception) {}
                    }
                )
                Button(
                    content = { Text(stringResource(Res.string.nbt_save)) },
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val bytes = file.readBytes()
                                FileKit.saveFile(
                                    bytes = bytes,
                                    baseName = file.nameWithoutExtension,
                                    extension = file.extension.ifEmpty { "png" }
                                )
                            } catch (_: Exception) {}
                        }
                    }
                )
                Button(
                    content = { Text(stringResource(Res.string.common_close)) },
                    onClick = onDismiss,
                    type = ButtonType.Primary
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().height(450.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MinecraftTheme.colors.dialogContainerBackground)
                    .clipToBounds()
                    .onSizeChanged { size ->
                        containerWidth = size.width
                        containerHeight = size.height
                        val (cx, cy) = clampOffsets(scale, offsetX, offsetY)
                        offsetX = cx
                        offsetY = cy
                    }
                    .pointerInput(Unit) {
                        coroutineScope {
                            launch {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (scale > 1f) {
                                        val maxOffsetX = (scale - 1f) * containerWidth / 2f
                                        val maxOffsetY = (scale - 1f) * containerHeight / 2f
                                        offsetX = (offsetX + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX)
                                        offsetY = (offsetY + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    }
                                }
                            }
                            launch {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                        if (scrollDelta != 0f) {
                                            val zoomFactor = if (scrollDelta < 0) 1.15f else 0.85f
                                            val newScale = (scale * zoomFactor).coerceIn(1f, 5f)
                                            scale = newScale
                                            val (cx, cy) = clampOffsets(scale, offsetX, offsetY)
                                            offsetX = cx
                                            offsetY = cy
                                            event.changes.firstOrNull()?.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    LoadingIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}