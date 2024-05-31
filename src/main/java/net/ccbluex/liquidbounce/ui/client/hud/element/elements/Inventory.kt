/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils

import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorder
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect2
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color


@ElementInfo(name = "Inventory")
class Inventory : Element(300.0, 50.0) {

    private val font by FontValue("Font", Fonts.font35)
    private val title by ListValue("Title", arrayOf("Center", "Left", "Right", "None"), "Left")
    private val titleRainbow by BoolValue("TitleRainbow", false) { title != "None" }
        private val titleRed by IntegerValue("TitleRed", 255, 0..255) { title != "None" && !titleRainbow }
        private val titleGreen by IntegerValue("TitleGreen", 255, 0..255) { title != "None" && !titleRainbow }
        private val titleBlue by IntegerValue("TitleBlue", 255, 0..255) { title != "None" && !titleRainbow }

    private val roundedRectRadius by FloatValue("Rounded-Radius", 3F, 0F..5F)

    private val borderValue by BoolValue("Border", true)
    private val borderRainbow by BoolValue("BorderRainbow", false) { borderValue }
        private val borderRed by IntegerValue("Border-R", 255, 0..255) { borderValue && !borderRainbow }
        private val borderGreen by IntegerValue("Border-G", 255, 0..255) { borderValue && !borderRainbow }
        private val borderBlue by IntegerValue("Border-B", 255, 0..255) { borderValue && !borderRainbow }

    private val backgroundAlpha by IntegerValue("Background-Alpha", 150, 0..255)

    private val width = 174F
    private val height = 66F
    private val padding = 6F

    override fun drawElement(): Border {
        val font = font
        val startY = if (title != "None") -(padding + font.FONT_HEIGHT) else 0F
        val borderColor = if (borderRainbow) ColorUtils.rainbow() else Color(borderRed, borderGreen, borderBlue)
        val titleColor = if (titleRainbow) ColorUtils.rainbow() else Color(titleRed, titleGreen, titleBlue)

        // draw rect and borders
        drawRoundedRect2(0F, startY, width, height, Color(0,0,0, backgroundAlpha), roundedRectRadius)
        if (borderValue) {
            drawBorder(0f, startY, width, height, 3f, borderColor.rgb)
            drawRect(0F, 0f, width, 1f, borderColor)
        }
        // Reset color
        resetColor()
        glColor4f(1F, 1F, 1F, 1F)


        val invDisplayName = mc.thePlayer.inventory.displayName.formattedText
        when (title.lowercase()) {
            "center" -> font.drawString(invDisplayName, width / 2 - font.getStringWidth(invDisplayName) / 2F, -(font.FONT_HEIGHT).toFloat(), titleColor.rgb, false)
            "left" -> font.drawString(invDisplayName, padding, -(font.FONT_HEIGHT).toFloat(), titleColor.rgb, false)
            "right" -> font.drawString(invDisplayName, width - padding - font.getStringWidth(invDisplayName), -(font.FONT_HEIGHT).toFloat(), titleColor.rgb, false)
        }

        // render items
        enableGUIStandardItemLighting()
        renderInv(9, 17, 6, 6, font)
        renderInv(18, 26, 6, 24, font)
        renderInv(27, 35, 6, 42, font)
        disableStandardItemLighting()
        enableAlpha()
        disableBlend()
        disableLighting()

        return Border(0F, startY, width, height)
    }

    /**
     * render single line of inventory
     * @param endSlot slot+9
     */
    private fun renderInv(slot: Int, endSlot: Int, x: Int, y: Int, font: FontRenderer) {
        var xOffset = x
        for (i in slot..endSlot) {
            xOffset += 18
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack ?: continue

            mc.renderItem.renderItemAndEffectIntoGUI(stack, xOffset - 18, y)
            mc.renderItem.renderItemOverlays(font, stack, xOffset - 18, y)
        }
    }
}