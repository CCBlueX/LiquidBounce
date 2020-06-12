/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.Shader
import org.lwjgl.opengl.GL20
import java.io.Closeable

class RainbowShader : Shader("rainbow_shader.frag"), Closeable {
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
        GL20.glUniform2f(getUniform("strength"), strengthX, strengthY)
        GL20.glUniform1f(getUniform("offset"), offset)
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

    companion object {
        @JvmField
        val INSTANCE = RainbowShader()

        @Suppress("NOTHING_TO_INLINE")
        inline fun begin(enable: Boolean, x: Float, y: Float, offset: Float): RainbowShader {
            val instance = INSTANCE

            if (enable) {
                instance.strengthX = x
                instance.strengthY = y
                instance.offset = offset

                instance.startShader()
            }

            return instance
        }
    }
}