/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.mc3d

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.bartixw.anymc.managers.TextureManager
import com.jme3.system.AppSettings
import com.jme3.system.JmeContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

object Mc3DController {
    private var appInstance: Mc3D? = null

    private val _activeFrame = MutableStateFlow<ImageBitmap?>(null)
    val activeFrame = _activeFrame.asStateFlow()

    enum class RenderTarget { MAIN_MAP, PLAYER_PREVIEW, NONE }

    @Volatile
    var currentTarget = RenderTarget.MAIN_MAP
        set(value) {
            field = value
            _activeFrame.value = null
        }

    fun init() {
        if (appInstance != null) return

        CoroutineScope(Dispatchers.IO).launch {
            TextureManager.ensureTexturesDownloaded()

            withContext(Dispatchers.Main) {
                if (appInstance != null) return@withContext
                val app = Mc3D().apply {
                    val settings = AppSettings(true).apply {
                        val screen = java.awt.Toolkit.getDefaultToolkit().screenSize
                        width = screen.width
                        height = screen.height
                        audioRenderer = null
                        setUseJoysticks(false)

                        isGammaCorrection = true
                    }
                    setSettings(settings)
                    isShowSettings = false
                    start(JmeContext.Type.OffscreenSurface)
                }
                appInstance = app
            }
        }
    }

    fun emitFrame(bitmap: ImageBitmap) {
        _activeFrame.value = bitmap
    }

    fun resize(width: Int, height: Int) {
        val app = appInstance ?: return
        if (width <= 0 || height <= 0) return

        app.enqueue {
            app.camera.resize(width, height, true)
            app.viewPort.processors.filterIsInstance<McPixelProcessor>().forEach {
                it.updateSize(width, height)
            }
        }
    }

    fun saveNextFrame(file: File) {
        val app = appInstance ?: return
        app.enqueue {
            app.viewPort.processors.filterIsInstance<McPixelProcessor>().forEach {
                it.requestSaveFile = file
            }
        }
    }

    fun rotatePlayer(dx: Float, dy: Float) {
        val app = appInstance ?: return
        app.playerYaw -= dx * 0.01f
        app.playerPitch -= dy * 0.01f
        
        // Clamp pitch to avoid flipping
        app.playerPitch = app.playerPitch.coerceIn(-1.5f, 1.5f)
    }

    suspend fun updatePlayerSkin(path: String) {
        var attempts = 0
        while (appInstance == null && attempts < 50) { // Wait up to 5 seconds
            kotlinx.coroutines.delay(100.milliseconds)
            attempts++
        }
        appInstance?.updatePlayerSkin(path)
    }

    @Composable
    fun setupTheme() {
        appInstance?.setupTheme()
    }

    suspend fun playOnceThen(actionName: String, nextActionName: String) {
        appInstance?.playOnceThen(actionName, nextActionName)
    }
}