/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.api.enums.MaterialType
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.opengl.GL11

/**
 * CustomHUD Armor element
 *
 * Shows a horizontal display of current armor
 */
@ElementInfo(name = "Armor")
class Armor(x: Double = -8.0, y: Double = 57.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{

    private val modeValue = ListValue("Alignment", arrayOf("Horizontal", "Vertical"), "Horizontal")

    private val preventConflictValue = BoolValue("PreventConflictWithBubbles", true)

    /**
     * Draw element
     */
    override fun drawElement(): Border?
    {
        if (mc.playerController.isNotCreative)
        {
            val thePlayer = mc.thePlayer ?: return null

            GL11.glPushMatrix()

            val renderItem = mc.renderItem

            val provider = classProvider

            // Prevents being conflicted with the bubbles
            val preventConflict = preventConflictValue.get() && thePlayer.isInsideOfMaterial(provider.getMaterialEnum(MaterialType.WATER))

            var x = 1
            var y = if (preventConflict) -10 else 0

            val mode = modeValue.get()

            val armorInventory = thePlayer.inventory.armorInventory
            (3 downTo 0).mapNotNull { armorInventory[it] }.forEach { stack ->
                renderItem.renderItemIntoGUI(stack, x, y)
                renderItem.renderItemOverlays(mc.fontRendererObj, stack, x, y)
                if (mode.equals("Horizontal", true)) x += 18
                else if (mode.equals("Vertical", true)) y += 18
            }

            val glStateManager = provider.glStateManager

            glStateManager.enableAlpha()
            glStateManager.disableBlend()
            glStateManager.disableLighting()
            glStateManager.disableCull()
            GL11.glPopMatrix()
        }

        return if (modeValue.get().equals("Horizontal", true)) Border(0F, 0F, 72F, 17F)
        else Border(0F, 0F, 18F, 72F)
    }
}
