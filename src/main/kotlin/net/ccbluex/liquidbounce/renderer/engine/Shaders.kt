package net.ccbluex.liquidbounce.renderer.engine

import net.ccbluex.liquidbounce.utils.resourceToString

/**
 * Here, all common shaders are registered.
 */
object Shaders {
    /**
     * Used for [ColoredPrimitiveRenderTask]
     */
    var primitiveColorShader: ShaderProgram? = null

    /**
     * Initializes all common shaders. Please only call this function if OpenGL 2.0 is supported.
     *
     * @throws IllegalStateException When one of the program fails to initialize
     */
    fun init() {
        try {
            primitiveColorShader = ShaderProgram(
                resourceToString("/assets/liquidbounce/shaders/primitive.vert"),
                resourceToString("/assets/liquidbounce/shaders/primitive.frag")
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize common shader programs", e)
        }
    }
}
