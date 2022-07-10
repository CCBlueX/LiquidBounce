/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.TextEvent
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20.glUseProgram
import java.awt.Font

class GameFontRenderer(font: Font) : FontRenderer(Minecraft.getMinecraft().gameSettings, ResourceLocation("textures/font/ascii.png"), Minecraft.getMinecraft().textureManager, false)
{

    val fontHeight: Int
    var defaultFont = AWTFontRenderer(font)
    private var boldFont = AWTFontRenderer(font.deriveFont(Font.BOLD))
    private var italicFont = AWTFontRenderer(font.deriveFont(Font.ITALIC))
    private var boldItalicFont = AWTFontRenderer(font.deriveFont(Font.BOLD or Font.ITALIC))

    val height: Int
        get() = defaultFont.height shr 1

    val size: Int
        get() = defaultFont.font.size

    init
    {
        fontHeight = height
    }

    fun drawString(s: String?, x: Float, y: Float, color: Int) = drawString(s, x, y, color, false)

    override fun drawStringWithShadow(text: String?, x: Float, y: Float, color: Int) = drawString(text, x, y, color, true)

    fun drawCenteredString(s: String, x: Float, y: Float, color: Int, shadow: Boolean) = drawString(s, x - getStringWidth(s) * 0.5f, y, color, shadow)

    fun drawCenteredString(s: String, x: Float, y: Float, color: Int) = drawStringWithShadow(s, x - getStringWidth(s) * 0.5F, y, color)

    override fun drawString(text: String?, x: Float, y: Float, color: Int, shadow: Boolean): Int
    {
        var currentText = text

        val event = TextEvent(currentText)
        LiquidBounce.eventManager.callEvent(event)
        currentText = event.text ?: return 0

        val currY = y - 3F

        val rainbow = RainbowFontShader.isInUse

        if (shadow)
        {
            glUseProgram(0)

            drawText(currentText, x + 1f, currY + 1f, -1778384896, true)
        }

        return drawText(currentText, x, currY, color, false, rainbow)
    }

    private fun drawText(text: String?, x: Float, y: Float, color: Int, ignoreColor: Boolean, rainbow: Boolean = false): Int
    {
        if (text == null) return 0
        if (text.isEmpty()) return x.toInt()

        val rainbowShaderId = RainbowFontShader.programId

        if (rainbow) glUseProgram(rainbowShaderId)

        GL11.glTranslated(x - 1.5, y + 0.5, 0.0)

        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        GlStateManager.enableTexture2D()

        var currentColor = color

        if (currentColor and -0x4000000 == 0) currentColor = currentColor or -16777216

        //		val defaultColor = currentColor

        val alpha: Int = (currentColor shr 24 and 0xff)

        if (text.contains("\u00A7"))
        {
            val parts = text.split("\u00A7")

            var currentFont = defaultFont

            var width = 0.0

            // Color code states
            var randomCase = false
            var bold = false
            var italic = false
            var strikeThrough = false
            var underline = false

            parts.withIndex().filter { it.value.isNotEmpty() }.forEach { (index, part) ->
                if (index == 0)
                {
                    currentFont.drawString(part, width, 0.0, currentColor)
                    width += currentFont.getStringWidth(part)
                }
                else
                {
                    val words = part.substring(1)
                    val type = part[0]

                    when (val colorIndex = getColorIndex(type))
                    {
                        in 0..15 ->
                        {
                            if (!ignoreColor)
                            {
                                currentColor = ColorUtils.hexColors[colorIndex] or (alpha shl 24)

                                if (rainbow) glUseProgram(0)
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

                        21 ->
                        {
                            currentColor = color

                            if (currentColor and -67108864 == 0) currentColor = currentColor or -16777216

                            if (rainbow) glUseProgram(rainbowShaderId)

                            bold = false
                            italic = false
                            randomCase = false
                            underline = false
                            strikeThrough = false
                        }
                    }

                    currentFont = if (bold && italic) boldItalicFont
                    else if (bold) boldFont
                    else if (italic) italicFont
                    else defaultFont

                    currentFont.drawString(if (randomCase) ColorUtils.randomMagicText(words) else words, width, 0.0, currentColor)

                    val quarterHeight = (fontHeight shr 4).toFloat()

                    if (strikeThrough) RenderUtils.drawLine(width * 0.5 + 1, currentFont.height / 3.0, (width + currentFont.getStringWidth(words)) * 0.5 + 1, currentFont.height / 3.0, quarterHeight)

                    if (underline)
                    {
                        val middleFontHeight = (currentFont.height shr 1).toDouble()
                        RenderUtils.drawLine(width * 0.5 + 1, middleFontHeight, (width + currentFont.getStringWidth(words)) * 0.5 + 1, middleFontHeight, quarterHeight)
                    }

                    width += currentFont.getStringWidth(words)
                }
            }
        }
        else
        { // Color code states
            defaultFont.drawString(text, 0.0, 0.0, currentColor)
        }

        GlStateManager.disableBlend()
        GL11.glTranslated(-(x - 1.5), -(y + 0.5), 0.0)
        RenderUtils.resetColor()

        return (x + getStringWidth(text)).toInt()
    }

    override fun getColorCode(charCode: Char) = ColorUtils.hexColors[getColorIndex(charCode)]

    override fun getStringWidth(text: String?): Int
    {
        var currentText = text

        val event = TextEvent(currentText)
        LiquidBounce.eventManager.callEvent(event)
        currentText = event.text ?: return 0

        return if (currentText.contains("\u00A7"))
        {
            val parts = currentText.split("\u00A7")

            var currentFont = defaultFont
            var width = 0
            var bold = false
            var italic = false

            parts.withIndex().filter { it.value.isNotEmpty() }.forEach { (index, part) ->
                if (index == 0) width += currentFont.getStringWidth(part)
                else
                {
                    val words = part.substring(1)
                    val type = part[0]
                    val colorIndex = getColorIndex(type)
                    when
                    {
                        colorIndex < 16 ->
                        {
                            bold = false
                            italic = false
                        }

                        colorIndex == 17 -> bold = true
                        colorIndex == 20 -> italic = true

                        colorIndex == 21 ->
                        {
                            bold = false
                            italic = false
                        }
                    }

                    currentFont = if (bold && italic) boldItalicFont
                    else if (bold) boldFont
                    else if (italic) italicFont
                    else defaultFont

                    width += currentFont.getStringWidth(words)
                }
            }

            width shr 1
        }
        else defaultFont.getStringWidth(currentText) shr 1
    }

    override fun getCharWidth(character: Char) = getStringWidth("$character")

    companion object
    {
        @JvmStatic
        fun getColorIndex(type: Char): Int
        {
            return when (type)
            {
                in '0'..'9' -> type - '0'
                in 'a'..'f' -> type - 'a' + 10
                in 'k'..'o' -> type - 'k' + 16
                'r' -> 21
                else -> -1
            }
        }
    }
}
