/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.render.shader.Shader
import org.lwjgl.opengl.GL20.*
import java.io.Closeable

object GradientFontShader : Shader("gradient_font_shader.frag"), Closeable {
    var isInUse = false
        private set

    var strengthX = 0f
    var strengthY = 0f
    var offset = 0f
    var speed = 1f

    var maxColors = 4
    var colors: Array<FloatArray> = Array(maxColors) { floatArrayOf(0f, 0f, 0f, 1f) }

    override fun setupUniforms() {
        setupUniform("offset")
        setupUniform("strength")
        setupUniform("speed")
        setupUniform("maxColors")

        for (i in 0 until MAX_GRADIENT_COLORS) {
            try {
                setupUniform("colors[$i]")
            } catch (e: Exception) {
                LOGGER.error("${javaClass.name} setup uniforms error.", e)
            }
        }
    }

    override fun updateUniforms() {
        glUniform2f(getUniform("strength"), strengthX, strengthY)
        glUniform1f(getUniform("offset"), offset)
        glUniform1f(getUniform("speed"), speed)
        glUniform1i(getUniform("maxColors"), maxColors)

        for (i in 0 until maxColors) {
            try {
                glUniform4f(getUniform("colors[$i]"), colors[i][0], colors[i][1], colors[i][2], colors[i][3])
            } catch (e: Exception) {
                LOGGER.error("${javaClass.name} update uniforms error.", e)
            }
        }
    }

    override fun startShader() {
        super.startShader()
        isInUse = true
    }

    override fun stopShader() {
        super.stopShader()
        isInUse = false
    }

    override fun close() {
        if (isInUse)
            stopShader()
    }

    fun begin(enable: Boolean, x: Float, y: Float, maxColors: Int, gradient: List<FloatArray>, speed: Float, offset: Float): GradientFontShader {
        if (enable) {
            strengthX = x
            strengthY = y
            this.maxColors = maxColors
            colors = gradient.toTypedArray()
            this.speed = speed
            this.offset = offset

            startShader()
        }

        return this
    }
}