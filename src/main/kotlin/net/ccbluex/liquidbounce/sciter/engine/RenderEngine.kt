package net.ccbluex.liquidbounce.renderer.engine

import net.ccbluex.liquidbounce.event.*

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

    val renderHandler = handler<RenderHudEvent> {
        EventManager.callEvent(LiquidBounceRenderEvent())

        render()
    }

    /**
     * Initialization
     */
    fun init() {

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
     * Draws all enqueued render tasks.
     */
    fun render() {
        val lvl = OpenGLLevel.OpenGL1_2

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
