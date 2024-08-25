/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawLine
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GradientFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.render.GlStateManager.*
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUseProgram
import java.awt.Color
import java.awt.Font

class GameFontRenderer(font: Font) : FontRenderer(mc.gameSettings, Identifier("textures/font/ascii.png"), mc.textureManager, false) {

    val fontHeight: Int
    val defaultFont = AWTFontRenderer(font)
    private val boldFont = AWTFontRenderer(font.deriveFont(Font.BOLD))
    private val italicFont = AWTFontRenderer(font.deriveFont(Font.ITALIC))
    private val boldItalicFont = AWTFontRenderer(font.deriveFont(Font.BOLD or Font.ITALIC))

    val height
        get() = defaultFont.height / 2

    val size
        get() = defaultFont.font.size

    init {
        fontHeight = height
    }

    fun drawString(s: String, x: Float, y: Float, color: Int) = drawString(s, x, y, color, false)

    override fun drawStringWithShadow(text: String, x: Float, y: Float, color: Int) = drawString(text, x, y, color, true)

    fun drawCenteredString(s: String, x: Float, y: Float, color: Int, shadow: Boolean) = drawString(s, x - getStringWidth(s) / 2F, y, color, shadow)

    fun drawCenteredString(s: String, x: Float, y: Float, color: Int) =
            drawStringWithShadow(s, x - getStringWidth(s) / 2F, y, color)

    override fun drawString(text: String, x: Float, y: Float, color: Int, shadow: Boolean): Int {
        val currentText = NameProtect.handleTextMessage(text)

        val currY = y - 3F

        val rainbow = RainbowFontShader.isInUse

        val gradient = GradientFontShader.isInUse

        if (shadow) {
            glUseProgram(0)

            drawText(currentText, x + 1f, currY + 1f, Color(0, 0, 0, 150).rgb, true)
        }

        return drawText(currentText, x, currY, color, false, rainbow, gradient)
    }

    private fun drawText(text: String, x: Float, y: Float, color: Int, ignoreColor: Boolean, rainbow: Boolean = false, gradient: Boolean = false): Int {
        if (text.isEmpty())
            return x.toInt()

        val rainbowShaderId = RainbowFontShader.programId

        if (rainbow)
            glUseProgram(rainbowShaderId)

        val gradientShaderId = GradientFontShader.programId

        if (gradient)
            glUseProgram(gradientShaderId)

        glTranslated(x - 1.5, y + 0.5, 0.0)
        enableAlpha()
        enableBlend()
        tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        enableTexture2D()

        var currentColor = color

        if (currentColor and -0x4000000 == 0)
            currentColor = currentColor or -16777216

        val alpha = (currentColor shr 24 and 0xff)

        if ("§" in text) {
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
                    currentFont.drawString(part, width, 0.0, currentColor)
                    width += currentFont.getStringWidth(part)
                } else {
                    val words = part.substring(1)
                    val type = part[0]

                    when (val colorIndex = getColorIndex(type)) {
                        in 0..15 -> {
                            if (!ignoreColor) {
                                currentColor = ColorUtils.hexColors[colorIndex] or (alpha shl 24)

                                if (rainbow)
                                    glUseProgram(0)

                                if (gradient)
                                    glUseProgram(0)
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
                            currentColor = color

                            if (currentColor and -67108864 == 0)
                                currentColor = currentColor or -16777216

                            if (rainbow)
                                glUseProgram(rainbowShaderId)

                            if (gradient)
                                glUseProgram(gradientShaderId)

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

                    currentFont.drawString(if (randomCase) ColorUtils.randomMagicText(words) else words, width, 0.0, currentColor)

                    if (strikeThrough)
                        drawLine(width / 2.0 + 1, currentFont.height / 3.0,
                                (width + currentFont.getStringWidth(words)) / 2.0 + 1, currentFont.height / 3.0,
                                fontHeight / 16F)

                    if (underline)
                        drawLine(width / 2.0 + 1, currentFont.height / 2.0,
                                (width + currentFont.getStringWidth(words)) / 2.0 + 1, currentFont.height / 2.0,
                                fontHeight / 16F)

                    width += currentFont.getStringWidth(words)
                }
            }
        } else {
            // Color code states
            defaultFont.drawString(text, 0.0, 0.0, currentColor)
        }

        disableBlend()
        glTranslated(-(x - 1.5), -(y + 0.5), 0.0)
        glColor4f(1f, 1f, 1f, 1f)

        return (x + getStringWidth(text)).toInt()
    }

    override fun getColorCode(charCode: Char) = ColorUtils.hexColors[getColorIndex(charCode)]

    override fun getStringWidth(text: String): Int {
        val currentText = NameProtect.handleTextMessage(text)

        return if ("§" in currentText) {
            val parts = currentText.split("§")

            var currentFont = defaultFont
            var width = 0
            var bold = false
            var italic = false

            parts.forEachIndexed { index, part ->
                if (part.isEmpty()) {
                    return@forEachIndexed
                }

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
        } else {
            defaultFont.getStringWidth(currentText) / 2
        }
    }

    override fun getCharWidth(character: Char) = getStringWidth(character.toString())

    companion object {
        fun getColorIndex(type: Char) =
            when (type) {
                in '0'..'9' -> type - '0'
                in 'a'..'f' -> type - 'a' + 10
                in 'k'..'o' -> type - 'k' + 16
                'r' -> 21
                else -> -1
            }
    }
}