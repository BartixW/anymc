/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.mc3d

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.jme3.post.SceneProcessor
import com.jme3.renderer.RenderManager
import com.jme3.renderer.queue.RenderQueue
import com.jme3.renderer.ViewPort
import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image.Format
import com.jme3.util.BufferUtils
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12

class McPixelProcessor(private val onFrameReady: (ImageBitmap) -> Unit) : SceneProcessor {
    private var renderManager: RenderManager? = null
    private var byteBuffer: ByteBuffer? = null
    private var bufferedImage: BufferedImage? = null
    private var width: Int = 0
    private var height: Int = 0

    var requestSaveFile: File? = null

    fun updateSize(w: Int, h: Int) {
        this.width = w
        this.height = h
        this.byteBuffer = BufferUtils.createByteBuffer(w * h * 4)
        this.bufferedImage = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE)
    }

    override fun initialize(rm: RenderManager, vp: ViewPort) {
        this.renderManager = rm
        updateSize(vp.camera.width, vp.camera.height)
    }

    override fun postFrame(out: FrameBuffer?) {
        val buf = byteBuffer ?: return
        val img = bufferedImage ?: return

        buf.clear()
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf)

        val data = (img.raster.dataBuffer as DataBufferInt).data

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = (x + y * width) * 4
                val r = buf.get(i).toInt() and 0xFF
                val g = buf.get(i + 1).toInt() and 0xFF
                val b = buf.get(i + 2).toInt() and 0xFF
                val a = buf.get(i + 3).toInt() and 0xFF

                val flippedY = height - y - 1
                data[x + flippedY * width] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        onFrameReady(img.toComposeImageBitmap())

        requestSaveFile?.let { file ->
            try {
                ImageIO.write(img, "png", file)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                requestSaveFile = null
            }
        }
    }

    override fun isInitialized(): Boolean = renderManager != null
    override fun reshape(vp: ViewPort, w: Int, h: Int) {}
    override fun preFrame(tpf: Float) {}
    override fun postQueue(rq: RenderQueue) {}
    override fun cleanup() {}
    override fun setProfiler(prof: com.jme3.profile.AppProfiler?) {}
}