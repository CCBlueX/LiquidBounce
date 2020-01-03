package net.ccbluex.liquidbounce.ui.font

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.TextEvent
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

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class GameFontRenderer(font: Font) : FontRenderer(Minecraft.getMinecraft().gameSettings,
        ResourceLocation("textures/font/ascii.png"), Minecraft.getMinecraft().textureManager, false) {

    var defaultFont = FontRenderer(font)
    private var boldFont = FontRenderer(font.deriveFont(Font.BOLD))
    private var italicFont = FontRenderer(font.deriveFont(Font.ITALIC))
    private var boldItalicFont = FontRenderer(font.deriveFont(Font.BOLD or Font.ITALIC))

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
        LiquidBounce.CLIENT.eventManager.callEvent(event)
        currentText = event.text ?: return 0

        val currY = y - 3F
        if (shadow)
            drawText(currentText, x + 1f, currY + 1f, Color(0, 0, 0, 150).rgb, true)
        return drawText(currentText, x, currY, color, false)
    }

    private fun drawText(text: String?, x: Float, y: Float, colorHex: Int, ignoreColor: Boolean): Int {
        text ?: return 0

        if (text.isEmpty())
            return x.toInt()

        GL11.glPushMatrix()
        GL11.glTranslated(x - 1.5, y + 0.5, 0.0)
        GlStateManager.enableAlpha()
        val blend = GL11.glGetBoolean(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_TEXTURE_2D)

        var hexColor = colorHex
        if (hexColor and -67108864 == 0)
            hexColor = hexColor or -16777216

        val red: Float = (hexColor shr 16 and 0xff) / 255F
        val green: Float = (hexColor shr 8 and 0xff) / 255F
        val blue: Float = (hexColor and 0xff) / 255F
        val alpha: Float = (hexColor shr 24 and 0xff) / 255F

        val color = Color(red, green, blue, alpha)

        if (text.contains("§")) {
            val parts = text.split("§")

            var currentFont = defaultFont
            var currentColor = Color(red, green, blue, alpha)

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

                    val colorIndex = "0123456789abcdefklmnor".indexOf(type)
                    when (colorIndex) {
                        in 0..15 -> {
                            if (!ignoreColor) {
                                val colorCode = ColorUtils.hexColors[colorIndex]

                                currentColor = Color((colorCode shr 16) / 255F, (colorCode shr 8 and 0xff) / 255F,
                                        (colorCode and 0xff) / 255F, alpha)
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
                            bold = false
                            italic = false
                            randomCase = false
                            underline = false
                            strikeThrough = false

                            currentColor = color
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
            defaultFont.drawString(text, 0.0, 0.0, color)

        if (!blend)
            GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
        GL11.glColor4f(1F, 1F, 1F, 1F)

        return (x + getStringWidth(text)).toInt()
    }

    override fun getColorCode(charCode: Char) =
            ColorUtils.hexColors["0123456789abcdef".indexOf(charCode)]

    override fun getStringWidth(text: String): Int {
        var currentText = text

        val event = TextEvent(currentText)
        LiquidBounce.CLIENT.eventManager.callEvent(event)
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
                    val colorIndex = "0123456789abcdefklmnor".indexOf(type)
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
}