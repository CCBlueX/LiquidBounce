/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.Shader
import org.lwjgl.opengl.GL20.*
import java.io.Closeable

object GradientShader : Shader("gradient_shader.frag"), Closeable {
    var isInUse = false
        private set

    var strengthX = 0f
    var strengthY = 0f
    var offset = 0f
    var speed = 1f

    var color1: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    var color2: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    var color3: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    var color4: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)

    override fun setupUniforms() {
        setupUniform("offset")
        setupUniform("strength")
        setupUniform("color1")
        setupUniform("color2")
        setupUniform("color3")
        setupUniform("color4")
        setupUniform("speed")
    }

    override fun updateUniforms() {
        glUniform2f(getUniform("strength"), strengthX, strengthY)
        glUniform1f(getUniform("offset"), offset)

        glUniform4f(getUniform("color1"), color1[0], color1[1], color1[2], color1[3])
        glUniform4f(getUniform("color2"), color2[0], color2[1], color2[2], color2[3])
        glUniform4f(getUniform("color3"), color3[0], color3[1], color3[2], color3[3])
        glUniform4f(getUniform("color4"), color4[0], color4[1], color4[2], color4[3])
        glUniform1f(getUniform("speed"), speed)
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

    fun begin(enable: Boolean, x: Float, y: Float, gradient1: FloatArray, gradient2: FloatArray, gradient3: FloatArray, gradient4: FloatArray, speed: Float, offset: Float): GradientShader {
        if (enable) {
            strengthX = x
            strengthY = y
            color1 = gradient1
            color2 = gradient2
            color3 = gradient3
            color4 = gradient4
            this.speed = speed
            this.offset = offset

            startShader()
        }

        return this
    }
}