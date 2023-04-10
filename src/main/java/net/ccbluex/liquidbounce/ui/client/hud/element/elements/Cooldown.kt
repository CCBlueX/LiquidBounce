/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.CooldownHelper.getAttackCooldownProgress
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import java.awt.Color

/**
 * CustomHUD cooldown element
 *
 * Shows simulated attack cooldown
 */
@ElementInfo(name = "Cooldown")
class Cooldown(x: Double = 0.0, y: Double = -14.0, scale: Float = 1F,
               side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.MIDDLE)) : Element(x, y, scale, side) {

    /**
     * Draw element
     */
    override fun drawElement(): Border {
        val progress = getAttackCooldownProgress()

        if (progress < 1.0) {
            drawRect(-25f, 0f, 25f, 3f, Color(0, 0, 0, 150).rgb)
            drawRect(-25f, 0f, 25f - 50f * progress.toFloat(), 3f, Color(0, 111, 255, 200).rgb)
        }

        return Border(-25F, 0F, 25F, 3F)
    }
}