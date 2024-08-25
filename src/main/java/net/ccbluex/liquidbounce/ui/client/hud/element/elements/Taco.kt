/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.command.commands.TacoCommand.tacoToggle
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.Identifier

/**
 * TACO TACO TACO!!
 */
@ElementInfo(name = "Taco", priority = 1)
class Taco(x: Double = 2.0, y: Double = 441.0) : Element(x = x, y = y) {

    private val frameSpeed by FloatValue("frameSpeed", 50f, 0f..200f)
    private val animationSpeed by FloatValue("animationSpeed", 0.15f, 0.01f..1.0f)

    private var lastFrameTime = System.currentTimeMillis()
    private var image = 0
    private var running = 0f

    private val tacoTextures = arrayOf(
        Identifier("liquidbounce/taco/1.png"),
        Identifier("liquidbounce/taco/2.png"),
        Identifier("liquidbounce/taco/3.png"),
        Identifier("liquidbounce/taco/4.png"),
        Identifier("liquidbounce/taco/5.png"),
        Identifier("liquidbounce/taco/6.png"),
        Identifier("liquidbounce/taco/7.png"),
        Identifier("liquidbounce/taco/8.png"),
        Identifier("liquidbounce/taco/9.png"),
        Identifier("liquidbounce/taco/10.png"),
        Identifier("liquidbounce/taco/11.png"),
        Identifier("liquidbounce/taco/12.png")
    )

    override fun drawElement(): Border {
        if (tacoToggle)
            return Border(0F, 0F, 0F, 0F)

        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastFrameTime

        if (elapsedTime >= frameSpeed) {
            updateAnimation()
            lastFrameTime = currentTime
        }

        val scaledScreen = ScaledResolution(mc)

        running += animationSpeed * deltaTime
        RenderUtils.drawImage(tacoTextures[image], running.toInt(), 0, 64, 32)

        if (running > scaledScreen.scaledWidth) {
            running = -scaledScreen.scaledWidth / 4F
        }

        return Border(0F, 0F, 64F, 32F)
    }

    private fun updateAnimation() {
        image = (image + 1) % tacoTextures.size
    }

}