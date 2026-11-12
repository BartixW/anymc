/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.mc3d

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.bartixw.anymc.managers.SkinManager
import com.bartixw.anymc.managers.TextureManager
import com.bartixw.anymc.ui.theme.MinecraftTheme
import com.jme3.anim.AnimComposer
import com.jme3.app.SimpleApplication
import com.jme3.asset.plugins.ClasspathLocator
import com.jme3.asset.plugins.FileLocator
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.material.MatParamTexture
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.shadow.DirectionalLightShadowRenderer
import com.jme3.shadow.EdgeFilteringMode
import com.jme3.texture.Texture
import kotlinx.coroutines.delay
import org.lwjgl.opengl.GL11
import kotlin.time.Duration.Companion.seconds

class Mc3D : SimpleApplication() {
    private lateinit var mapNode: Node
    private lateinit var playerNode: Node
    private lateinit var dl: DirectionalLight
    private lateinit var al: AmbientLight
    private lateinit var shadowProcessor: DirectionalLightShadowRenderer
    var animComposer: AnimComposer? = null
    private var playerPreviewBackground: Color? = null

    var playerYaw: Float = 3.14f
    var playerPitch: Float = 0f

    override fun simpleInitApp() {
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1)
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)

        java.util.logging.Logger.getLogger(com.jme3.util.TangentBinormalGenerator::class.java.name).level = java.util.logging.Level.SEVERE

        inputManager.clearMappings()

        assetManager.registerLocator(SkinManager.skinsDir.absolutePath, FileLocator::class.java)
        assetManager.registerLocator(TextureManager.texturesDir.absolutePath, FileLocator::class.java)
        assetManager.registerLocator("composeResources/anymc.shared.generated.resources/", ClasspathLocator::class.java)

        // viewPort.addProcessor(McPixelProcessor { bitmap ->
        //     Mc3DController.emitFrame(bitmap)
        // })

        mapNode = Node("MapScene")
        playerNode = Node("PlayerScene")

        mapNode.shadowMode = RenderQueue.ShadowMode.CastAndReceive
        playerNode.shadowMode = RenderQueue.ShadowMode.CastAndReceive
        rootNode.shadowMode = RenderQueue.ShadowMode.CastAndReceive

        al = AmbientLight()
        al.color = ColorRGBA(0.4f, 0.45f, 0.55f, 1.0f)
        rootNode.addLight(al)

        dl = DirectionalLight()
        dl.direction = Vector3f(-0.6f, -1f, -0.4f).normalizeLocal()
        dl.color = ColorRGBA(1.1f, 1.08f, 0.98f, 1.0f)
        rootNode.addLight(dl)

        val map = assetManager.loadModel("files/models/Map.glb")
        map.depthFirstTraversal { spatial ->
            if (spatial is Geometry) {
                try {
                    com.jme3.util.TangentBinormalGenerator.generate(spatial.mesh)
                } catch (_: Exception) {
                    // Skip invalid or empty meshes
                }
            }
        }
        setupMinecraftMaterial(map, ColorRGBA(0.5f, 0.5f, 0.5f, 1f))
        makePixelated(map)
        mapNode.attachChild(map)

        val player = assetManager.loadModel("files/models/Player.glb")
        makePixelated(player)
        playerNode.attachChild(player)

        animComposer = playerNode.getControl(AnimComposer::class.java)
            ?: playerNode.getChild("blockbench_export")?.getControl(AnimComposer::class.java)

        animComposer?.setCurrentAction("breathe")

        shadowProcessor = DirectionalLightShadowRenderer(assetManager, 2048, 4).apply {
            light = dl
            edgeFilteringMode = EdgeFilteringMode.PCFPOISSON
            shadowIntensity = 0.7f
            lambda = 0.65f
            shadowZExtend = 500f
        }
        viewPort.addProcessor(shadowProcessor)

        viewPort.addProcessor(McPixelProcessor { bitmap ->
            Mc3DController.emitFrame(bitmap)
        })

        rootNode.attachChild(playerNode)
    }

    override fun simpleUpdate(tpf: Float) {
        when (Mc3DController.currentTarget) {
            Mc3DController.RenderTarget.MAIN_MAP -> {
                if (mapNode.parent == null) {
                    if (playerNode.parent == null) rootNode.attachChild(playerNode)
                    rootNode.attachChild(mapNode)
                    viewPort.backgroundColor = ColorRGBA(0.5f, 0.65f, 0.9f, 1f)

                    al.color = ColorRGBA(0.4f, 0.45f, 0.55f, 1.0f)
                    dl.color = ColorRGBA(1.4f, 1.35f, 1.2f, 1.0f)
                    dl.direction = Vector3f(-0.6f, -1f, -0.4f).normalizeLocal()

                    playerNode.localRotation.fromAngles(0f, 3.1f, 0f)
                    playerNode.setLocalScale(1f)
                    playerNode.localTranslation = Vector3f(43.5f, 57.75f, 320.25f)

                    cam.location = Vector3f(40f, 102f, 440f)
                    cam.lookAt(Vector3f(40f, 100f, 423f), Vector3f.UNIT_Y)
                }
            }
            Mc3DController.RenderTarget.PLAYER_PREVIEW -> {
                if (mapNode.parent != null) {
                    rootNode.detachChild(mapNode)
                }
                if (playerNode.parent == null) rootNode.attachChild(playerNode)

                val bgColor = playerPreviewBackground ?: Color.Black
                viewPort.backgroundColor = ColorRGBA(
                    bgColor.red,
                    bgColor.green,
                    bgColor.blue,
                    bgColor.alpha
                )

                playerNode.localTranslation = Vector3f(0f, 0f, 0f)
                playerNode.setLocalScale(1.25f)
                playerNode.localRotation.fromAngles(playerPitch, playerYaw, 0f)

                al.color = ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f)
                dl.color = ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f)
                dl.direction = Vector3f(0f, -0.5f, -1f).normalizeLocal()

                cam.location = Vector3f(0f, 45f, 100f)
                cam.lookAt(Vector3f(0f, 17.7f, 0f), Vector3f.UNIT_Y)
            }
            Mc3DController.RenderTarget.NONE -> {
                if (rootNode.children.isNotEmpty()) {
                    rootNode.detachAllChildren()
                }
            }
        }
    }

    fun applyPlayerSkin(playerSpatial: Spatial, skinPath: String) {
        try {
            val key = com.jme3.asset.TextureKey(skinPath, false)
            assetManager.deleteFromCache(key)
            val texture = assetManager.loadTexture(key).apply {
                magFilter = Texture.MagFilter.Nearest
                minFilter = Texture.MinFilter.NearestNoMipMaps
                setWrap(Texture.WrapMode.EdgeClamp)
            }

            playerSpatial.depthFirstTraversal { spatial ->
                if (spatial is Geometry) {
                    val newMat = com.jme3.material.Material(assetManager, "Common/MatDefs/Light/Lighting.j3md").apply {
                        setTexture("DiffuseMap", texture)
                        setBoolean("UseMaterialColors", true)
                        setColor("Diffuse", ColorRGBA.White)

                        setColor("Ambient", ColorRGBA(0.6f, 0.6f, 0.6f, 1.0f))
                    }

                    spatial.material = newMat

                    spatial.material.additionalRenderState.blendMode = com.jme3.material.RenderState.BlendMode.Alpha
                    spatial.material.setFloat("AlphaDiscardThreshold", 0.5f)
                    spatial.queueBucket = RenderQueue.Bucket.Opaque
                }
            }
        } catch (e: Exception) {
            println("Mc3D: Failed to apply skin $skinPath: ${e.message}")
        }
    }

    fun updatePlayerSkin(path: String) {
        enqueue {
            playerNode.children.firstOrNull()?.let { player ->
                val fileName = java.io.File(path).name
                applyPlayerSkin(player, fileName)
            }
        }
    }

    private fun setupMinecraftMaterial(spatial: Spatial, defaultAmbientColor: ColorRGBA) {
        spatial.depthFirstTraversal { s ->
            if (s is Geometry) {
                val oldMat = s.material

                val mainTexture = oldMat.getParam("BaseColorMap")?.value as? Texture
                    ?: oldMat.getParam("DiffuseMap")?.value as? Texture

                if (mainTexture != null) {
                    val combinedName = "${s.name} ${s.parent?.name ?: ""} ${oldMat.name ?: ""} ${mainTexture.name ?: ""} ${mainTexture.key?.name ?: ""}".lowercase()

                    val texName = when {
                        combinedName.contains("short_grass") -> "short_grass.png"
                        combinedName.contains("grass_block_top") -> "grass_block_top.png"
                        combinedName.contains("grass_block_side") -> "grass_block_side.png"
                        combinedName.contains("birch_leaves") -> "birch_leaves.png"
                        combinedName.contains("birch_log_top") -> "birch_log_top.png"
                        combinedName.contains("birch_log") -> "birch_log.png"
                        combinedName.contains("rose_bush_top") -> "rose_bush_top.png"
                        combinedName.contains("rose_bush_bottom") -> "rose_bush_bottom.png"
                        combinedName.contains("wildflowers_stem") -> "wildflowers_stem.png"
                        combinedName.contains("wildflowers") -> "wildflowers.png"
                        combinedName.contains("dandelion") -> "dandelion.png"
                        combinedName.contains("dirt") -> "dirt.png"
                        combinedName.contains("stone") -> "stone.png"
                        combinedName.contains("granite") -> "granite.png"
                        combinedName.contains("andesite") -> "andesite.png"
                        combinedName.contains("gravel") -> "gravel.png"
                        combinedName.contains("bush") -> "bush.png"
                        else -> null
                    }

                    val officialTexture = if (texName != null) {
                        try {
                            val key = com.jme3.asset.TextureKey(texName, false)
                            assetManager.deleteFromCache(key)
                            assetManager.loadTexture(key).apply {
                                magFilter = Texture.MagFilter.Nearest
                                minFilter = Texture.MinFilter.NearestNoMipMaps
                                setWrap(Texture.WrapMode.EdgeClamp)
                            }
                        } catch (e: Exception) {
                            println("Mc3D: Failed loading texture $texName: ${e.message}")
                            mainTexture
                        }
                    } else {
                        mainTexture
                    }

                    val newMat = com.jme3.material.Material(assetManager, "Common/MatDefs/Light/Lighting.j3md").apply {
                        setTexture("DiffuseMap", officialTexture)
                        setBoolean("UseMaterialColors", true)
                        setColor("Diffuse", ColorRGBA.White)

                        setColor("Ambient", defaultAmbientColor)
                    }

                    newMat.additionalRenderState.blendMode = com.jme3.material.RenderState.BlendMode.Alpha

                    newMat.setFloat("AlphaDiscardThreshold", 0.1f)

                    s.material = newMat
                    s.queueBucket = RenderQueue.Bucket.Opaque
                }
            }
        }
    }

    private fun makePixelated(spatial: Spatial) {
        spatial.depthFirstTraversal { s ->
            if (s is Geometry) {
                val mat = s.material
                for (p in mat.params) {
                    if (p is MatParamTexture) {
                        p.textureValue?.apply {
                            magFilter = Texture.MagFilter.Nearest
                            minFilter = Texture.MinFilter.NearestNoMipMaps
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun setupTheme() {
        playerPreviewBackground = MinecraftTheme.colors.controlBackground
    }

    suspend fun AnimComposer.playOnceThen(actionName: String, nextActionName: String) {
        val action = this.setCurrentAction(actionName, this.layerNames.firstOrNull(), false)

        delay((action?.length ?: 0.0).seconds)

        this.setCurrentAction(nextActionName)
    }

    suspend fun playOnceThen(actionName: String, nextActionName: String) {
        animComposer?.playOnceThen(actionName, nextActionName)
    }
}