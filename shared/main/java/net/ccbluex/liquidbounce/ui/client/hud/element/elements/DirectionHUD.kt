/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.RGBColorValue
import kotlin.math.floor

/**
 * CustomHUD Armor element
 *
 * Shows a direction hud
 */
@ElementInfo(name = "DirectionHUD")
class DirectionHUD(x: Double = -8.0, y: Double = 57.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.UP)) : Element(x, y, scale, side)
{
    private val COMPASS_IMAGE = classProvider.createResourceLocation("${LiquidBounce.CLIENT_NAME.toLowerCase()}/directionhud/compass.png")

    private val arrowColorValue = RGBColorValue("ArrowColor", 255, 0, 0)

    /**
     * Draw element
     */
    override fun drawElement(): Border?
    {
        val thePlayer = mc.thePlayer ?: return null

        mc.textureManager.bindTexture(COMPASS_IMAGE)
        RenderUtils.resetColor()

        val direction = floor(thePlayer.rotationYaw * 256.0f / 360.0f + 0.5f).toInt() and 0xFF
        RenderUtils.drawModalRectWithCustomSizedTexture(0f, 0f, direction - if (direction >= 128) 128f else 0f, if (direction >= 128) 12f else 0f, 65f, 12f, 256f, 256f)
        RenderUtils.drawRect(32f, 0f, 33f, 12f, arrowColorValue.get())

        RenderUtils.resetColor()

        return Border(0F, 0F, 65F, 12F)
    }
}
