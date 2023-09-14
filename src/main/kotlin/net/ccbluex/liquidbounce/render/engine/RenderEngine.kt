/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.render.engine

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.interfaces.IMixinGameRenderer
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.font.GlyphPage
import net.ccbluex.liquidbounce.render.shaders.Shaders
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.Mat4
import net.ccbluex.liquidbounce.utils.math.toMat4
import net.minecraft.client.MinecraftClient
import org.lwjgl.opengl.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

class Layer(val renderTasks: ArrayList<RenderTask> = ArrayList(200))
class LayerSettings(val mvpMatrix: Mat4, val culling: Boolean, val depthTest: Boolean = false)

/**
 * Handles all rendering tasks.
 *
 * The tasks are grouped by layer, higher layers are rendered on top of smaller ones
 */
object RenderEngine : Listenable {
    /**
     * Uses the perspective of the minecraft camera; Coordinates match minecraft coordinates, culling enabled
     */
    const val CAMERA_VIEW_LAYER = 0

    /**
     * Uses the perspective of the minecraft camera without bobbing, culling enabled
     */
    const val CAMERA_VIEW_LAYER_WITHOUT_BOBBING = 1

    /**
     * Screen space, mirrored vertically, depth test enabled
     */
    const val SCREEN_SPACE_LAYER = 2

    /**
     * A layer to render minecraft-specific stuff, the render engine is barely used
     */
    const val MINECRAFT_INTERNAL_RENDER_TASK = 3

    /**
     * Projects the vertices on the screen. 1 unit = 1 px, backface culling enabled
     */
    const val HUD_LAYER = 4

    /**
     * How many layers is the render engine supposed to render?
     */
    private const val LAYER_CONT = 5

    /**
     * The table the tasks are stored it, grouped by layers
     */
    val renderTaskTable: Array<Layer> = Array(LAYER_CONT) { Layer() }

    /**
     * Contains runnables with tasks to run when the render engine ticks the next time
     */
    val deferredForRenderThread: LinkedBlockingQueue<Runnable> = LinkedBlockingQueue()

    /**
     * Used to recognize what GL version we are on
     */
    val openGlVersionRegex = Pattern.compile("(\\d+)\\.(\\d+)(\\.(\\d+))?(.*)")

    /**
     * Contains the MVP matrix used by MC for the current frame. Always initialized when [LiquidBounceRenderEvent] is dispatched.
     */
    lateinit var cameraMvp: Mat4

    val RENDERED_OUTLINES = AtomicInteger(0)

//    val renderHandler = handler<OverlayRenderEvent> {
//        this.cameraMvp = (MinecraftClient.getInstance().gameRenderer as IMixinGameRenderer).getCameraMVPMatrix(
//            it.tickDelta,
//            true
//        ).toMat4()
//
//        val outlines = RENDERED_OUTLINES.getAndSet(0)
//
//        if (outlines > 0) {
//            println(outlines)
//        }
//
//        EventManager.callEvent(EngineRenderEvent(it.tickDelta))
//
//        GL11.glLineWidth(1.0f)
//
//        render(it.tickDelta)
//
//        // Run the deferred tasks
//        while (true) {
//            val currentTask = deferredForRenderThread.poll() ?: break
//
//            currentTask.run()
//        }
//    }

    /**
     * Initialization
     */
    fun init() {
        Shaders.init()
        GlyphPage.init()
        Fonts.loadFonts()

        val versionString = GL11.glGetString(GL11.GL_VERSION)

        if (versionString == null) {
            logger.error("OpenGL didn't return a version string.")
            return
        }

        val matcher = openGlVersionRegex.matcher(versionString)

        if (!matcher.matches()) {
            logger.error("OpenGL returned an invalid version string: $versionString")
            return
        }

        val majorVersion = matcher.group(1).toInt()
        val minorVersion = matcher.group(2).toInt()
        val patchVersion = if (matcher.groupCount() >= 5) matcher.group(4)?.toInt() else null

        logger.info("Found out OpenGL version to be $majorVersion.$minorVersion${if (patchVersion != null) ".$patchVersion" else ""}.")
    }

    /**
     * Enqueues a task for rendering
     *
     * @param layer The layer it is supposed to be rendered on (See this class's description)
     */
    fun enqueueForRendering(layer: Int, task: RenderTask) {
        // this.renderTaskTable[layer].renderTasks.add(task)
    }

    /**
     * @see enqueueForRendering
     */
    fun enqueueForRendering(layer: Int, task: Array<RenderTask>) {
        // this.renderTaskTable[layer].renderTasks.addAll(task)
    }

    /**
     * Runs the [runnable] on the next rendering tick. This can be used to run something on a
     * thread with OpenGL context
     *
     * Thread safe.
     */
    fun runOnGlContext(runnable: Runnable) {
        this.deferredForRenderThread.add(runnable)
    }

    /**
     * Draws all enqueued render tasks.
     */
    fun render(tickDelta: Float) {
        // Get bound VAO
        val oldVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING)
        // Get bound shader
        val oldShader = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM)
        // Get bound texture
        val oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        // Get bound framebuffer
        val oldFramebuffer = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING)
        // Get bound array buffer
        val oldArrayBuffer = GL15.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING)
        // Get bound element array buffer
        val oldElementArrayBuffer = GL15.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING)

        // Get blend state
        val oldBlendState = GL11.glGetBoolean(GL11.GL_BLEND)

        if (!oldBlendState) {
            GL11.glEnable(GL11.GL_BLEND)
        }

        for ((idx, layer) in renderTaskTable.withIndex()) {
            // Don't calculate mvp matrices for empty layers
            if (layer.renderTasks.isEmpty()) {
                continue
            }

            val settings = getSettingsForLayer(idx, tickDelta)

            if (settings.culling) {
                GL11.glEnable(GL11.GL_CULL_FACE)
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE)
            }

            if (settings.depthTest) {
                GL11.glEnable(GL11.GL_DEPTH_TEST)
            } else {
                GL11.glDisable(GL11.GL_DEPTH_TEST)
            }

            for (renderTask in layer.renderTasks) {
                renderTask.initRendering(settings.mvpMatrix)
                renderTask.draw()
                renderTask.cleanupRendering()
            }

            layer.renderTasks.clear()
        }

        GL32.glBindVertexArray(oldVAO)
        GL20.glUseProgram(oldShader)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTexture)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, oldFramebuffer)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldArrayBuffer)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, oldElementArrayBuffer)

        if (!oldBlendState) {
            GL11.glDisable(GL11.GL_BLEND)
        }
    }

    fun getSettingsForLayer(layer: Int, tickDelta: Float): LayerSettings {
        return when (layer) {
            CAMERA_VIEW_LAYER -> LayerSettings(
                (MinecraftClient.getInstance().gameRenderer as IMixinGameRenderer).getCameraMVPMatrix(
                    tickDelta,
                    true
                ).toMat4(),
                true
            )

            CAMERA_VIEW_LAYER_WITHOUT_BOBBING -> LayerSettings(
                (MinecraftClient.getInstance().gameRenderer as IMixinGameRenderer).getCameraMVPMatrix(
                    tickDelta,
                    false
                ).toMat4(),
                true
            )

            SCREEN_SPACE_LAYER -> {
                val aspectRatio = mc.window.width.toFloat() / mc.window.height.toFloat()

                LayerSettings(
                    Mat4.orthograpicProjectionMatrix(-aspectRatio, -1.0f, aspectRatio, 1.0f, -1.0f, 1.0f),
                    true,
                    depthTest = true
                )
            }

            HUD_LAYER -> LayerSettings(
                Mat4.orthograpicProjectionMatrix(
                    0.0f,
                    0.0f,
                    MinecraftClient.getInstance().window.framebufferWidth.toFloat(),
                    MinecraftClient.getInstance().window.framebufferHeight.toFloat(),
                    -1.0f,
                    1.0f
                ),
                true
            )

            MINECRAFT_INTERNAL_RENDER_TASK -> LayerSettings(
                Mat4.orthograpicProjectionMatrix(
                    0.0f,
                    0.0f,
                    MinecraftClient.getInstance().window.scaledWidth.toFloat(),
                    MinecraftClient.getInstance().window.scaledHeight.toFloat(),
                    -200.0f,
                    200.0f
                ),
                false
            )

            else -> throw UnsupportedOperationException("Unknown layer")
        }
    }

    override fun handleEvents(): Boolean = true
}
