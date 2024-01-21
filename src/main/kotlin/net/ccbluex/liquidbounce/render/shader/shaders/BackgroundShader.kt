/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.render.shader.shaders
import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.utils.client.mc
import org.lwjgl.opengl.GL20.*

class BackgroundShader : Shader("background.frag") {

    companion object {
        val BACKGROUND_SHADER = BackgroundShader()
    }

    private var time = 0f

    override fun setupUniforms() {
        setupUniform("iResolution")
        setupUniform("iTime")
    }

    override fun updateUniforms() {
        val resolutionID = getUniform("iResolution")
        if (resolutionID > -1) {
            glUniform2f(resolutionID, mc.window.width.toFloat(), mc.window.height.toFloat())
        }

        val timeID = getUniform("iTime")
        if (timeID > -1) {
            glUniform1f(timeID, time)
        }


        time += 0.003f // todo: deltaTime
    }
}
