/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.TextEvent
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.resources.IResourceManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Font

class GameFontRenderer(font: Font) : FontRenderer(Minecraft.getMinecraft().gameSettings,
        ResourceLocation("textures/font/ascii.png"), if (ClassUtils.hasForge()) null else Minecraft.getMinecraft().textureManager, false) {

    var defaultFont = AWTFontRenderer(font)
    private var boldFont = AWTFontRenderer(font.deriveFont(Font.BOLD))
    private var italicFont = AWTFontRenderer(font.deriveFont(Font.ITALIC))
    private var boldItalicFont = AWTFontRenderer(font.deriveFont(Font.BOLD or Font.ITALIC))

    val height: Int
        get() = defaultFont.height / 2

    val size: Int
        get() = defaultFont.font.size

    init {
        FONT_HEIGHT = height
    }

    fun drawString(s: String, x: Float, y: Float, color: Int) = drawString(s, x, y, color, false)

    override fun drawStringWithShadow(text: String, x: Float, y: Float, color: Int) = drawString(text, x, y, color, true)

    fun drawCenteredString(s: String, x: Float, y: Float, color: Int, shadow: Boolean) = drawString(s, x - getStringWidth(s) / 2F, y, color, shadow)

    fun drawCenteredString(s: String, x: Float, y: Float, color: Int) =
            drawStringWithShadow(s, x - getStringWidth(s) / 2F, y, color)

    override fun drawString(text: String, x: Float, y: Float, color: Int, shadow: Boolean): Int {
        var currentText = text

        val event = TextEvent(currentText)
        LiquidBounce.eventManager.callEvent(event)
        currentText = event.text ?: return 0

        val currY = y - 3F
        if (shadow)
            drawText(currentText, x + 1f, currY + 1f, Color(0, 0, 0, 150).rgb, true)
        return drawText(currentText, x, currY, color, false)
    }

    private fun drawText(text: String?, x: Float, y: Float, colorHex: Int, ignoreColor: Boolean): Int {
        if (text == null)
            return 0
        if (text.isNullOrEmpty())
            return x.toInt()

        GlStateManager.translate(x - 1.5, y + 0.5, 0.0)
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        GlStateManager.enableTexture2D()

        var hexColor = colorHex
        if (hexColor and -67108864 == 0)
            hexColor = hexColor or -16777216

        val alpha: Int = (hexColor shr 24 and 0xff)

        if (text.contains("§")) {
            val parts = text.split("§")

            var currentFont = defaultFont

            var width = 0.0

            // Color code states
            var randomCase = false
            var bold = false
            var italic = false
            var strikeThrough = false
            var underline = false

            parts.forEachIndexed { index, part ->
                if (part.isEmpty())
                    return@forEachIndexed

                if (index == 0) {
                    currentFont.drawString(part, width, 0.0, hexColor)
                    width += currentFont.getStringWidth(part)
                } else {
                    val words = part.substring(1)
                    val type = part[0]

                    when (val colorIndex = getColorIndex(type)) {
                        in 0..15 -> {
                            if (!ignoreColor) {
                                hexColor = ColorUtils.hexColors[colorIndex] or (alpha shl 24)
                            }

                            bold = false
                            italic = false
                            randomCase = false
                            underline = false
                            strikeThrough = false
                        }
                        16 -> randomCase = true
                        17 -> bold = true
                        18 -> strikeThrough = true
                        19 -> underline = true
                        20 -> italic = true
                        21 -> {
                            hexColor = colorHex
                            if (hexColor and -67108864 == 0)
                                hexColor = hexColor or -16777216

                            bold = false
                            italic = false
                            randomCase = false
                            underline = false
                            strikeThrough = false
                        }
                    }

                    currentFont = if (bold && italic)
                        boldItalicFont
                    else if (bold)
                        boldFont
                    else if (italic)
                        italicFont
                    else
                        defaultFont

                    currentFont.drawString(if (randomCase) ColorUtils.randomMagicText(words) else words, width, 0.0, hexColor)

                    if (strikeThrough)
                        RenderUtils.drawLine(width / 2.0 + 1, currentFont.height / 3.0,
                                (width + currentFont.getStringWidth(words)) / 2.0 + 1, currentFont.height / 3.0,
                                FONT_HEIGHT / 16F)

                    if (underline)
                        RenderUtils.drawLine(width / 2.0 + 1, currentFont.height / 2.0,
                                (width + currentFont.getStringWidth(words)) / 2.0 + 1, currentFont.height / 2.0,
                                FONT_HEIGHT / 16F)

                    width += currentFont.getStringWidth(words)
                }
            }
        } else
            defaultFont.drawString(text, 0.0, 0.0, hexColor)

        GlStateManager.disableBlend()
        GlStateManager.translate(-(x - 1.5), -(y + 0.5), 0.0)
        GlStateManager.color(1f, 1f, 1f, 1f)

        return (x + getStringWidth(text)).toInt()
    }

    override fun getColorCode(charCode: Char) =
            ColorUtils.hexColors[getColorIndex(charCode)]

    override fun getStringWidth(text: String): Int {
        var currentText = text

        val event = TextEvent(currentText)
        LiquidBounce.eventManager.callEvent(event)
        currentText = event.text ?: return 0

        return if (currentText.contains("§")) {
            val parts = currentText.split("§")

            var currentFont = defaultFont
            var width = 0
            var bold = false
            var italic = false

            parts.forEachIndexed { index, part ->
                if (part.isEmpty())
                    return@forEachIndexed

                if (index == 0) {
                    width += currentFont.getStringWidth(part)
                } else {
                    val words = part.substring(1)
                    val type = part[0]
                    val colorIndex = getColorIndex(type)
                    when {
                        colorIndex < 16 -> {
                            bold = false
                            italic = false
                        }
                        colorIndex == 17 -> bold = true
                        colorIndex == 20 -> italic = true
                        colorIndex == 21 -> {
                            bold = false
                            italic = false
                        }
                    }

                    currentFont = if (bold && italic)
                        boldItalicFont
                    else if (bold)
                        boldFont
                    else if (italic)
                        italicFont
                    else
                        defaultFont

                    width += currentFont.getStringWidth(words)
                }
            }

            width / 2
        } else
            defaultFont.getStringWidth(currentText) / 2
    }

    override fun getCharWidth(character: Char) = getStringWidth(character.toString())

    override fun onResourceManagerReload(resourceManager: IResourceManager) {}

    override fun bindTexture(location: ResourceLocation?) {}

    companion object {
        @JvmStatic
        fun getColorIndex(type: Char): Int {
            return when (type) {
                in '0'..'9' -> type - '0'
                in 'a'..'f' -> type - 'a' + 10
                in 'k'..'o' -> type - 'k' + 16
                'r' -> 21
                else -> -1
            }
        }
    }
}