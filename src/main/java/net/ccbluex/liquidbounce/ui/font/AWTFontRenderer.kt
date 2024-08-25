/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.GlStateManager.bindTexture
import net.minecraft.client.texture.TextureUtil
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * Generate new bitmap based font renderer
 */
@SideOnly(Side.CLIENT)
class AWTFontRenderer(val font: Font, startChar: Int = 0, stopChar: Int = 255, private var loadingScreen: Boolean = false) : MinecraftInstance() {
    companion object {
        var assumeNonVolatile = false
        val activeTextRenderers = mutableListOf<AWTFontRenderer>()

        private var gcTicks = 0
        private const val GC_TICKS = 600 // Start garbage collection every 600 frames
        private const val CACHED_FONT_REMOVAL_TIME = 30000 // Remove cached texts after 30s of not being used

        fun garbageCollectionTick() {
            if (gcTicks++ > GC_TICKS) {
                activeTextRenderers.forEach { it.collectGarbage() }

                gcTicks = 0
            }
        }
    }

    private fun collectGarbage() {
        val currentTime = System.currentTimeMillis()

        cachedStrings.filter { currentTime - it.value.lastUsage > CACHED_FONT_REMOVAL_TIME }.forEach {
            glDeleteLists(it.value.displayList, 1)

            it.value.deleted = true

            cachedStrings.remove(it.key)
        }
    }

    private var fontHeight = -1
    private val charLocations = arrayOfNulls<CharLocation>(stopChar)

    private val cachedStrings = mutableMapOf<String, CachedFont>()

    private var textureID = -1
    private var textureWidth = 0
    private var textureHeight = 0

    val height
        get() = (fontHeight - 8) / 2

    init {
        renderBitmap(startChar, stopChar)

        activeTextRenderers += this
    }

    /**
     * Allows you to draw a string with the target font
     *
     * @param text  to render
     * @param x     location for target position
     * @param y     location for target position
     * @param color of the text
     */
    fun drawString(text: String, x: Double, y: Double, color: Int) {
        val scale = 0.25
        val reverse = 1 / scale

        glPushMatrix()
        glScaled(scale, scale, scale)
        glTranslated(x * 2F, y * 2.0 - 2.0, 0.0)

        if (loadingScreen) {
            glBindTexture(GL_TEXTURE_2D, textureID)
        } else {
            bindTexture(textureID)
        }

        val red = (color shr 16 and 0xff) / 255F
        val green = (color shr 8 and 0xff) / 255F
        val blue = (color and 0xff) / 255F
        val alpha = (color shr 24 and 0xff) / 255F

        glColor4f(red, green, blue, alpha)

        var currX = 0.0f
        var unicodeWidth = 0.0f

        val cached = cachedStrings[text]

        if (cached != null) {
            glCallList(cached.displayList)
            cached.lastUsage = System.currentTimeMillis()
            glPopMatrix()
            return
        }

        var list = -1

        if (assumeNonVolatile) {
            list = glGenLists(1)

            glNewList(list, GL_COMPILE_AND_EXECUTE)
        }

        glBegin(GL_QUADS)

        for (char in text.toCharArray()) {
            val fontChar = charLocations.getOrNull(char.code)

            if (fontChar == null) {
                glEnd()

                GlStateManager.resetColor()

                GlStateManager.pushMatrix()
                glScaled(reverse, reverse, reverse)
                val fontScaling = font.size / 32.0

                glScaled(fontScaling, fontScaling, 0.0)
                mc.fontRendererObj.posY = 1.0f
                mc.fontRendererObj.posX = (currX / 4) + unicodeWidth
                val width = mc.fontRendererObj.renderUnicodeChar(char, false)
                    .coerceAtLeast(0.0f) // A few characters have a negative width due to not being supported by the minecraft font renderer
                unicodeWidth += width

                if (loadingScreen) {
                    glBindTexture(GL_TEXTURE_2D, textureID)
                } else {
                    bindTexture(textureID)
                }
                GlStateManager.popMatrix()

                glBegin(GL_QUADS)
            } else {
                drawChar(fontChar, currX + (unicodeWidth * 4), 0f)
                currX += fontChar.width - 8.0f
            }
        }

        glEnd()

        if (assumeNonVolatile) {
            cachedStrings[text] = CachedFont(list, System.currentTimeMillis())
            glEndList()
        }

        glPopMatrix()
    }

    /**
     * Draw char from texture to display
     *
     * @param char target font char to render
     * @param x        target position x to render
     * @param y        target position y to render
     */
    private fun drawChar(char: CharLocation, x: Float, y: Float) {
        val width = char.width.toFloat()
        val height = char.height.toFloat()
        val srcX = char.x.toFloat()
        val srcY = char.y.toFloat()
        val renderX = srcX / textureWidth
        val renderY = srcY / textureHeight
        val renderWidth = width / textureWidth
        val renderHeight = height / textureHeight

        glTexCoord2f(renderX, renderY)
        glVertex2f(x, y)
        glTexCoord2f(renderX, renderY + renderHeight)
        glVertex2f(x, y + height)
        glTexCoord2f(renderX + renderWidth, renderY + renderHeight)
        glVertex2f(x + width, y + height)
        glTexCoord2f(renderX + renderWidth, renderY)
        glVertex2f(x + width, y)
    }

    /**
     * Render font chars to a bitmap
     */
    private fun renderBitmap(startChar: Int, stopChar: Int) {
        val fontImages = arrayOfNulls<BufferedImage>(stopChar)
        var rowHeight = 0
        var charX = 0
        var charY = 0

        for (targetChar in startChar until stopChar) {
            val fontImage = drawCharToImage(targetChar.toChar())
            val fontChar = CharLocation(charX, charY, fontImage.width, fontImage.height)

            if (fontChar.height > fontHeight)
                fontHeight = fontChar.height
            if (fontChar.height > rowHeight)
                rowHeight = fontChar.height

            charLocations[targetChar] = fontChar
            fontImages[targetChar] = fontImage

            charX += fontChar.width

            if (charX > 2048) {
                if (charX > textureWidth)
                    textureWidth = charX

                charX = 0
                charY += rowHeight
                rowHeight = 0
            }
        }
        textureHeight = charY + rowHeight

        val bufferedImage = BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics2D = bufferedImage.graphics as Graphics2D
        graphics2D.font = font
        graphics2D.color = Color(255, 255, 255, 0)
        graphics2D.fillRect(0, 0, textureWidth, textureHeight)
        graphics2D.color = Color.white

        for (targetChar in startChar until stopChar)
            if (fontImages[targetChar] != null && charLocations[targetChar] != null)
                graphics2D.drawImage(fontImages[targetChar], charLocations[targetChar]!!.x, charLocations[targetChar]!!.y,
                        null)

        textureID = TextureUtil.uploadTextureImageAllocate(TextureUtil.glGenTextures(), bufferedImage, true,
                true)
    }

    /**
     * Draw a char to a buffered image
     *
     * @param ch char to render
     * @return image of the char
     */
    private fun drawCharToImage(ch: Char): BufferedImage {
        val graphics2D = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).graphics as Graphics2D

        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics2D.font = font

        val fontMetrics = graphics2D.fontMetrics

        var charWidth = fontMetrics.charWidth(ch) + 8
        if (charWidth <= 0)
            charWidth = 7

        var charHeight = fontMetrics.height + 3
        if (charHeight <= 0)
            charHeight = font.size

        val fontImage = BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB)
        val graphics = fontImage.graphics as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.font = font
        graphics.color = Color.WHITE
        graphics.drawString(ch.toString(), 3, 1 + fontMetrics.ascent)

        return fontImage
    }

    /**
     * Calculate the string width of a text
     *
     * @param text for width calculation
     * @return the width of the text
     */
    fun getStringWidth(text: String): Int {
        var width = 0
        var mcWidth = 0

        val fontScaling = font.size / 32.0

        for (c in text.toCharArray()) {
            val fontChar = charLocations.getOrNull(c.code)

            if (fontChar == null) {
                mcWidth += ((mc.fontRendererObj.getCharWidth(c) + 8) * fontScaling)
                    .coerceAtLeast(0.0)
                    .roundToInt()
            } else {
                width += fontChar.width - 8
            }
        }

        return (width / 2) + mcWidth
    }

    fun delete() {
        if (textureID != -1) {
            glDeleteTextures(textureID)
            textureID = -1
        }

        activeTextRenderers.remove(this)
    }

    fun finalize() {
        delete()
    }

    /**
     * Data class for saving char location of the font image
     */
    private data class CharLocation(var x: Int, var y: Int, var width: Int, var height: Int)
}