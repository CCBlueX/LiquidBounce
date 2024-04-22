/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockBush
import net.minecraft.item.ItemBlock
import org.lwjgl.opengl.GL11
import java.awt.Color

@ElementInfo(name = "BlockCounter")
class BlockCounter(x: Double = 520.0, y: Double = 245.0) : Element(x = x, y = y) {

    private val onScaffold by BoolValue("ScaffoldOnly", true)

    private val roundedRectRadius by FloatValue("Rounded-Radius", 2F, 0F..5F)

    private val backgroundMode by ListValue("Background-Color", arrayOf("Custom", "Rainbow"), "Custom")
    private val backgroundRed by IntegerValue("Background-R", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundGreen by IntegerValue("Background-G", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundBlue by IntegerValue("Background-B", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundAlpha by IntegerValue("Background-Alpha", 255, 0..255) { backgroundMode == "Custom" }

    private val borderMode by ListValue("Border-Color", arrayOf("Custom", "Rainbow"), "Custom")
    private val borderRed by IntegerValue("Border-R", 0, 0..255) { borderMode == "Custom" }
    private val borderGreen by IntegerValue("Border-G", 0, 0..255) { borderMode == "Custom" }
    private val borderBlue by IntegerValue("Border-B", 0, 0..255) { borderMode == "Custom" }
    private val borderAlpha by IntegerValue("Border-Alpha", 255, 0..255) { borderMode == "Custom" }

    private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }
    private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }

    override fun drawElement(): Border {
        if ((Scaffold.handleEvents() && onScaffold) || !onScaffold) {

            GL11.glPushMatrix()

            if (BlockOverlay.handleEvents() && BlockOverlay.info && BlockOverlay.currentBlock != null)
                GL11.glTranslatef(0f, 15f, 0f)

            val info = "Blocks: §7$blocksAmount"

            val backgroundCustomColor = Color(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha).rgb
            val borderCustomColor = Color(borderRed, borderGreen, borderBlue, borderAlpha).rgb

            val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
            val rainbowX = if (rainbowX == 0f) 0f else 1f / rainbowX
            val rainbowY = if (rainbowY == 0f) 0f else 1f / rainbowY

            RainbowShader.begin(backgroundMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                RenderUtils.drawRoundedBorderRect(
                    0F,
                    0F,
                    Fonts.font40.getStringWidth(info) + 5F,
                    16f,
                    3F,
                    when (backgroundMode) {
                        "Rainbow" -> 0
                        else -> backgroundCustomColor
                    },
                    borderCustomColor,
                    roundedRectRadius
                )
            }

            Fonts.font40.drawString(
                info, 3, 5, Color.WHITE.rgb
            )

            GL11.glPopMatrix()
        }

        return Border(0F, 0F, 58F, 15F)
    }

    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue
                val item = stack.item
                if (item is ItemBlock) {
                    val block = item.block
                    val heldItem = mc.thePlayer?.heldItem
                    if (heldItem != null && heldItem == stack || block !in InventoryUtils.BLOCK_BLACKLIST && block !is BlockBush) {
                        amount += stack.stackSize
                    }
                }
            }
            return amount
        }
}