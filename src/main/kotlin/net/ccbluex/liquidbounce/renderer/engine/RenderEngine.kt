/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.renderer.engine

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.renderer.engine.font.GlyphPage
import org.lwjgl.opengl.GL11
import java.util.concurrent.LinkedBlockingQueue

class Layer(val renderTasks: ArrayList<RenderTask> = ArrayList(200))

/**
 * Handles all rendering tasks.
 *
 * The tasks are grouped by layer, higher layers are rendered on top of smaller ones
 */
object RenderEngine : Listenable {
    /**
     * How many layers is the render engine supposed to render?
     */
    const val layerCount = 5

    /**
     * The table the tasks are stored it, grouped by layers
     */
    val renderTaskTable: Array<Layer> = Array(layerCount) { Layer() }

    /**
     * Contains runnables with tasks to run when the render engine ticks the next time
     */
    val deferredForRenderThread: LinkedBlockingQueue<Runnable> = LinkedBlockingQueue()

    val renderHandler = handler<RenderHudEvent> {
        EventManager.callEvent(LiquidBounceRenderEvent())

        render()

        // Run the deferred tasks
        while (true) {
            val currentTask = deferredForRenderThread.poll() ?: break

            currentTask.run()
        }
    }

    /**
     * Initialization
     */
    fun init() {
        Shaders.init()
        GlyphPage.init()
    }

    /**
     * Enqueues a task for rendering
     *
     * @param layer The layer it is suppose to be rendered on (See this class's description)
     */
    fun enqueueForRendering(layer: Int, task: RenderTask) {
        this.renderTaskTable[layer].renderTasks.add(task)
    }

    /**
     * @see enqueueForRendering
     */
    fun enqueueForRendering(layer: Int, task: Array<RenderTask>) {
        this.renderTaskTable[layer].renderTasks.addAll(task)
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
    fun render() {
        val lvl = OpenGLLevel.OpenGL4_3

        GL11.glEnable(GL11.GL_ALPHA_TEST)
        GL11.glEnable(GL11.GL_BLEND)

        for (layer in renderTaskTable) {
            for (renderTask in layer.renderTasks) {
                renderTask.initRendering(lvl)
                renderTask.draw(lvl)
                renderTask.cleanupRendering(lvl)
            }

            layer.renderTasks.clear()
        }
    }

    override fun handleEvents(): Boolean = true
}
