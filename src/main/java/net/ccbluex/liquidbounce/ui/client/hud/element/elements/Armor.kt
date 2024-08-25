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
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.material.Material
import com.mojang.blaze3d.platform.GlStateManager.*
import org.lwjgl.opengl.GL11.glPopMatrix
import org.lwjgl.opengl.GL11.glPushMatrix

/**
 * CustomHUD Armor element
 *
 * Shows a horizontal display of current armor
 */
@ElementInfo(name = "Armor")
class Armor(x: Double = -8.0, y: Double = 57.0, scale: Float = 1F,
            side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side) {

    private val modeValue by ListValue("Alignment", arrayOf("Horizontal", "Vertical"), "Horizontal")

    /**
     * Draw element
     */
    override fun drawElement(): Border {
        if (!mc.interactionManager.currentGameMode.isCreative) {
            glPushMatrix()

            val renderItem = mc.renderItem
            val isInsideWater = mc.player.isInsideOfMaterial(Material.water)

            var x = 1
            var y = if (isInsideWater) -10 else 0

            for (index in 3 downTo 0) {
                val stack = mc.player.inventory.armorInventory[index] ?: continue

                renderItem.renderItemIntoGUI(stack, x, y)
                renderItem.renderItemOverlays(mc.fontRendererObj, stack, x, y)

                when (modeValue) {
                    "Horizontal" -> x += 18
                    "Vertical" -> y += 18
                }
            }

            enableAlpha()
            disableBlend()
            disableLighting()
            disableCull()
            glPopMatrix()
        }

        return when (modeValue) {
            "Horizontal" -> Border(0F, 0F, 72F, 17F)
            else -> Border(0F, 0F, 18F, 72F)
        }
    }
}