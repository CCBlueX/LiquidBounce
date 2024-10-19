/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.Shader
import org.lwjgl.opengl.GL20.glUniform1f
import org.lwjgl.opengl.GL20.glUniform2f
import java.io.Closeable

object RainbowShader : Shader("rainbow_shader.frag"), Closeable {
    var isInUse = false
        private set

    var strengthX = 0f
    var strengthY = 0f
    var offset = 0f

    override fun setupUniforms() {
        setupUniform("offset")
        setupUniform("strength")
    }

    override fun updateUniforms() {
        glUniform2f(getUniform("strength"), strengthX, strengthY)
        glUniform1f(getUniform("offset"), offset)
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

    @JvmStatic
    fun begin(enable: Boolean, x: Float, y: Float, offset: Float): RainbowShader {
        if (enable) {
            strengthX = x
            strengthY = y
            this.offset = offset

            startShader()
        }

        return this
    }
}