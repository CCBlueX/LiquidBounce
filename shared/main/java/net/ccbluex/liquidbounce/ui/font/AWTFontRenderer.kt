/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import net.ccbluex.liquidbounce.injection.backend.WrapperImpl.classProvider
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * Generate new bitmap based font renderer
 */
@SideOnly(Side.CLIENT)
class AWTFontRenderer(val font: Font, startChar: Int = 0, stopChar: Int = 255, var loadingScreen: Boolean = false) : MinecraftInstance() {
    companion object {
        var assumeNonVolatile: Boolean = false
        val activeFontRenderers: ArrayList<AWTFontRenderer> = ArrayList()

        private var gcTicks: Int = 0
        private const val GC_TICKS = 600 // Start garbage collection every 600 frames
        private const val CACHED_FONT_REMOVAL_TIME = 30000 // Remove cached texts after 30s of not being used

        fun garbageCollectionTick() {
            if (gcTicks++ > GC_TICKS) {
                activeFontRenderers.forEach { it.collectGarbage() }

                gcTicks = 0
            }
        }
    }

    private fun collectGarbage() {
        val currentTime = System.currentTimeMillis()

        cachedStrings.filter { currentTime - it.value.lastUsage > CACHED_FONT_REMOVAL_TIME }.forEach {
            GL11.glDeleteLists(it.value.displayList, 1)

            it.value.deleted = true

            cachedStrings.remove(it.key)
        }
    }

    private var fontHeight = -1
    private val charLocations = arrayOfNulls<CharLocation>(stopChar)

    private val cachedStrings: HashMap<String, CachedFont> = HashMap()

    private var textureID = -1
    private var textureWidth = 0
    private var textureHeight = 0

    val height: Int
        get() = (fontHeight - 8) / 2

    init {
        renderBitmap(startChar, stopChar)

        activeFontRenderers.add(this)
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

        GL11.glPushMatrix()
        GL11.glScaled(scale, scale, scale)
        GL11.glTranslated(x * 2F, y * 2.0 - 2.0, 0.0)

        if (this.loadingScreen)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        else
            classProvider.getGlStateManager().bindTexture(textureID)

        val red: Float = (color shr 16 and 0xff) / 255F
        val green: Float = (color shr 8 and 0xff) / 255F
        val blue: Float = (color and 0xff) / 255F
        val alpha: Float = (color shr 24 and 0xff) / 255F

        GL11.glColor4f(red, green, blue, alpha)

        var currX = 0.0

        val cached: CachedFont? = cachedStrings[text]

        if (cached != null) {
            GL11.glCallList(cached.displayList)

            cached.lastUsage = System.currentTimeMillis()

            GL11.glPopMatrix()

            return
        }

        var list = -1

        if (assumeNonVolatile) {
            list = GL11.glGenLists(1)

            GL11.glNewList(list, GL11.GL_COMPILE_AND_EXECUTE)
        }

        GL11.glBegin(GL11.GL_QUADS)

        for (char in text.toCharArray()) {
            if (char.toInt() >= charLocations.size) {
                GL11.glEnd()

                // Ugly solution, because floating point numbers, but I think that shouldn't be that much of a problem
                GL11.glScaled(reverse, reverse, reverse)
                mc.fontRendererObj.drawString("$char", currX.toFloat() * scale.toFloat() + 1, 2f, color, false)
                currX += mc.fontRendererObj.getStringWidth("$char") * reverse

                GL11.glScaled(scale, scale, scale)

                if (this.loadingScreen)
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
                else
                    classProvider.getGlStateManager().bindTexture(textureID)

                GL11.glColor4f(red, green, blue, alpha)

                GL11.glBegin(GL11.GL_QUADS)
            } else {
                val fontChar = charLocations[char.toInt()] ?: continue

                drawChar(fontChar, currX.toFloat(), 0f)
                currX += fontChar.width - 8.0
            }
        }

        GL11.glEnd()

        if (assumeNonVolatile) {
            cachedStrings[text] = CachedFont(list, System.currentTimeMillis())
            GL11.glEndList()
        }

        GL11.glPopMatrix()
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

        GL11.glTexCoord2f(renderX, renderY)
        GL11.glVertex2f(x, y)
        GL11.glTexCoord2f(renderX, renderY + renderHeight)
        GL11.glVertex2f(x, y + height)
        GL11.glTexCoord2f(renderX + renderWidth, renderY + renderHeight)
        GL11.glVertex2f(x + width, y + height)
        GL11.glTexCoord2f(renderX + renderWidth, renderY)
        GL11.glVertex2f(x + width, y)
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

        for (c in text.toCharArray()) {
            val fontChar = charLocations[
                    if (c.toInt() < charLocations.size)
                        c.toInt()
                    else
                        '\u0003'.toInt()
            ] ?: continue

            width += fontChar.width - 8
        }

        return width / 2
    }

    fun delete() {
        if (textureID != -1) {
            GL11.glDeleteTextures(textureID)
            textureID = -1
        }

        activeFontRenderers.remove(this)
    }

    fun finalize() {
        delete()
    }

    /**
     * Data class for saving char location of the font image
     */
    private data class CharLocation(var x: Int, var y: Int, var width: Int, var height: Int)
}