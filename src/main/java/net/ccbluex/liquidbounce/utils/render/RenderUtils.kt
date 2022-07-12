/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.Maps
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object RenderUtils : MinecraftInstance()
{
    private val glCapMap = hashMapOf<Int, Boolean>()
    private val DISPLAY_LISTS_2D = IntArray(4)

    val ICONS: ResourceLocation = ResourceLocation("textures/gui/icons.png")

    @JvmStatic
    var frameTime = 0

    @JvmStatic
    fun drawBlockBox(theWorld: World, thePlayer: Entity, blockPos: BlockPos, color: Int, outlineColor: Int, hydraESP: Boolean, partialTicks: Float)
    {
        val block = theWorld.getBlock(blockPos)

        val lastTickPosX = thePlayer.lastTickPosX
        val lastTickPosY = thePlayer.lastTickPosY
        val lastTickPosZ = thePlayer.lastTickPosZ

        val posX = lastTickPosX + (thePlayer.posX - lastTickPosX) * partialTicks
        val posY = lastTickPosY + (thePlayer.posY - lastTickPosY) * partialTicks
        val posZ = lastTickPosZ + (thePlayer.posZ - lastTickPosZ) * partialTicks

        block.setBlockBoundsBasedOnState(theWorld, blockPos)

        val axisAlignedBB = block.getSelectedBoundingBox(theWorld, blockPos).expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026).offset(-posX, -posY, -posZ)

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        enableGlCap(GL11.GL_BLEND)
        disableGlCap(GL11.GL_TEXTURE_2D, GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        glColor(color)

        drawFilledBox(axisAlignedBB)

        if (outlineColor shr 24 > 0)
        {
            GL11.glLineWidth(1.00f)
            enableGlCap(GL11.GL_LINE_SMOOTH)
            glColor(outlineColor)
            drawSelectionBoundingBox(axisAlignedBB, hydraESP)
        }

        resetColor()
        GL11.glDepthMask(true)
        resetCaps()
    }

    @JvmStatic
    fun drawSelectionBoundingBox(boundingBox: AxisAlignedBB, hydraESP: Boolean)
    {
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)

        val minX = boundingBox.minX
        val minY = boundingBox.minY
        val minZ = boundingBox.minZ
        val maxX = boundingBox.maxX
        val maxY = boundingBox.maxY
        val maxZ = boundingBox.maxZ

        // Lower Rectangle
        worldrenderer.pos(minX, minY, minZ).endVertex()
        worldrenderer.pos(minX, minY, maxZ).endVertex()
        worldrenderer.pos(maxX, minY, maxZ).endVertex()
        worldrenderer.pos(maxX, minY, minZ).endVertex()
        worldrenderer.pos(minX, minY, minZ).endVertex()

        // Upper Rectangle
        worldrenderer.pos(minX, maxY, minZ).endVertex()
        worldrenderer.pos(minX, maxY, maxZ).endVertex()
        worldrenderer.pos(maxX, maxY, maxZ).endVertex()
        worldrenderer.pos(maxX, maxY, minZ).endVertex()
        worldrenderer.pos(minX, maxY, minZ).endVertex()

        // Upper Rectangle
        worldrenderer.pos(minX, maxY, maxZ).endVertex()
        worldrenderer.pos(minX, minY, maxZ).endVertex()
        worldrenderer.pos(maxX, minY, maxZ).endVertex()
        worldrenderer.pos(maxX, maxY, maxZ).endVertex()
        worldrenderer.pos(maxX, maxY, minZ).endVertex()
        worldrenderer.pos(maxX, minY, minZ).endVertex()

        if (hydraESP)
        {
            tessellator.draw()

            worldrenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)

            // From min, min, min
            worldrenderer.pos(minX, minY, minZ).endVertex()
            worldrenderer.pos(maxX, maxY, minZ).endVertex()
            worldrenderer.pos(minX, minY, minZ).endVertex()
            worldrenderer.pos(minX, maxY, maxZ).endVertex()
            worldrenderer.pos(minX, minY, minZ).endVertex()
            worldrenderer.pos(maxX, maxY, maxZ).endVertex()
            worldrenderer.pos(minX, minY, minZ).endVertex()
            worldrenderer.pos(maxX, minY, maxZ).endVertex()
            worldrenderer.pos(minX, minY, minZ).endVertex()

            // From max, min, min
            worldrenderer.pos(maxX, minY, minZ).endVertex()
            worldrenderer.pos(minX, maxY, minZ).endVertex()
            worldrenderer.pos(maxX, minY, minZ).endVertex()
            worldrenderer.pos(maxX, maxY, maxZ).endVertex()
            worldrenderer.pos(maxX, minY, minZ).endVertex()
            worldrenderer.pos(minX, minY, maxZ).endVertex()
            worldrenderer.pos(maxX, minY, minZ).endVertex()
            worldrenderer.pos(minX, maxY, maxZ).endVertex()
            worldrenderer.pos(maxX, minY, minZ).endVertex()

            // From min, min, max
            worldrenderer.pos(minX, minY, maxZ).endVertex()
            worldrenderer.pos(minX, maxY, minZ).endVertex()
            worldrenderer.pos(minX, minY, maxZ).endVertex()
            worldrenderer.pos(maxX, maxY, maxZ).endVertex()
            worldrenderer.pos(minX, minY, maxZ).endVertex()
            worldrenderer.pos(maxX, minY, minZ).endVertex()
            worldrenderer.pos(minX, minY, maxZ).endVertex()
            worldrenderer.pos(maxX, maxY, minZ).endVertex()
            worldrenderer.pos(minX, minY, maxZ).endVertex()

            // From max, min, max
            worldrenderer.pos(maxX, minY, maxZ).endVertex()
            worldrenderer.pos(minX, maxY, maxZ).endVertex()
            worldrenderer.pos(maxX, minY, maxZ).endVertex()
            worldrenderer.pos(maxX, maxY, minZ).endVertex()
            worldrenderer.pos(maxX, minY, maxZ).endVertex()
            worldrenderer.pos(minX, minY, minZ).endVertex()
            worldrenderer.pos(maxX, minY, maxZ).endVertex()
            worldrenderer.pos(minX, maxY, minZ).endVertex()
            worldrenderer.pos(maxX, minY, maxZ).endVertex()

            // From min, max, min
            worldrenderer.pos(minX, maxY, minZ).endVertex()
            worldrenderer.pos(maxX, maxY, maxZ).endVertex()
            worldrenderer.pos(maxX, maxY, minZ).endVertex()
            worldrenderer.pos(minX, maxY, maxZ).endVertex()
        }

        tessellator.draw()
    }

    @JvmStatic
    fun drawEntityBox(entity: Entity, boxColor: Int, outlineColor: Int, drawHydraESP: Boolean, partialTicks: Float)
    {
        val renderManager = mc.renderManager

        val posX = entity.posX
        val posY = entity.posY
        val posZ = entity.posZ

        val lastTickPosX = entity.lastTickPosX
        val lastTickPosY = entity.lastTickPosY
        val lastTickPosZ = entity.lastTickPosZ

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        enableGlCap(GL11.GL_BLEND)
        disableGlCap(GL11.GL_TEXTURE_2D, GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        val x = lastTickPosX + (posX - lastTickPosX) * partialTicks - renderManager.renderPosX
        val y = lastTickPosY + (posY - lastTickPosY) * partialTicks - renderManager.renderPosY
        val z = lastTickPosZ + (posZ - lastTickPosZ) * partialTicks - renderManager.renderPosZ

        val entityBox = entity.entityBoundingBox

        val axisAlignedBB = AxisAlignedBB(entityBox.minX - posX + x - 0.05, entityBox.minY - posY + y, entityBox.minZ - posZ + z - 0.05, entityBox.maxX - posX + x + 0.05, entityBox.maxY - posY + y + 0.15, entityBox.maxZ - posZ + z + 0.05)

        if (outlineColor shr 24 and 0xFF > 0) // outlineColor.alpha > 0
        {
            GL11.glLineWidth(1.00f)
            enableGlCap(GL11.GL_LINE_SMOOTH)
            glColor(outlineColor)
            drawSelectionBoundingBox(axisAlignedBB, drawHydraESP)
        }

        glColor(boxColor)

        drawFilledBox(axisAlignedBB)

        resetColor()
        GL11.glDepthMask(true)

        resetCaps()
    }

    @JvmStatic
    fun drawAxisAlignedBB(axisAlignedBB: AxisAlignedBB, color: Int)
    {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glLineWidth(2.00f)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        glColor(color)
        drawFilledBox(axisAlignedBB)

        resetColor()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
    }

    @JvmStatic
    fun drawPlatform(y: Double, color: Int, size: Double)
    {
        val renderManager = mc.renderManager
        val renderY = y - renderManager.renderPosY

        drawAxisAlignedBB(AxisAlignedBB(size, renderY + 0.02, size, -size, renderY, -size), color)
    }

    @JvmStatic
    fun drawPlatform(entity: Entity, color: Int, partialTicks: Float)
    {
        val renderManager = mc.renderManager

        val posX = entity.posX
        val posY = entity.posY
        val posZ = entity.posZ

        val lastTickPosX = entity.lastTickPosX
        val lastTickPosY = entity.lastTickPosY
        val lastTickPosZ = entity.lastTickPosZ

        val x = lastTickPosX + (posX - lastTickPosX) * partialTicks - renderManager.renderPosX
        val y = lastTickPosY + (posY - lastTickPosY) * partialTicks - renderManager.renderPosY
        val z = lastTickPosZ + (posZ - lastTickPosZ) * partialTicks - renderManager.renderPosZ

        val axisAlignedBB = entity.entityBoundingBox.offset(-posX, -posY, -posZ).offset(x, y, z)

        drawAxisAlignedBB(AxisAlignedBB(axisAlignedBB.minX, axisAlignedBB.maxY + 0.2, axisAlignedBB.minZ, axisAlignedBB.maxX, axisAlignedBB.maxY + 0.26, axisAlignedBB.maxZ), color)
    }

    @JvmStatic
    fun drawFilledBox(axisAlignedBB: AxisAlignedBB)
    {
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        worldRenderer.begin(7, DefaultVertexFormats.POSITION)
        val minX = axisAlignedBB.minX
        val minY = axisAlignedBB.minY
        val minZ = axisAlignedBB.minZ
        val maxX = axisAlignedBB.maxX
        val maxY = axisAlignedBB.maxY
        val maxZ = axisAlignedBB.maxZ

        worldRenderer.pos(minX, minY, minZ).endVertex()
        worldRenderer.pos(minX, maxY, minZ).endVertex()
        worldRenderer.pos(maxX, minY, minZ).endVertex()
        worldRenderer.pos(maxX, maxY, minZ).endVertex()
        worldRenderer.pos(maxX, minY, maxZ).endVertex()
        worldRenderer.pos(maxX, maxY, maxZ).endVertex()
        worldRenderer.pos(minX, minY, maxZ).endVertex()
        worldRenderer.pos(minX, maxY, maxZ).endVertex()
        worldRenderer.pos(maxX, maxY, minZ).endVertex()
        worldRenderer.pos(maxX, minY, minZ).endVertex()
        worldRenderer.pos(minX, maxY, minZ).endVertex()
        worldRenderer.pos(minX, minY, minZ).endVertex()
        worldRenderer.pos(minX, maxY, maxZ).endVertex()
        worldRenderer.pos(minX, minY, maxZ).endVertex()
        worldRenderer.pos(maxX, maxY, maxZ).endVertex()
        worldRenderer.pos(maxX, minY, maxZ).endVertex()
        worldRenderer.pos(minX, maxY, minZ).endVertex()
        worldRenderer.pos(maxX, maxY, minZ).endVertex()
        worldRenderer.pos(maxX, maxY, maxZ).endVertex()
        worldRenderer.pos(minX, maxY, maxZ).endVertex()
        worldRenderer.pos(minX, maxY, minZ).endVertex()
        worldRenderer.pos(minX, maxY, maxZ).endVertex()
        worldRenderer.pos(maxX, maxY, maxZ).endVertex()
        worldRenderer.pos(maxX, maxY, minZ).endVertex()
        worldRenderer.pos(minX, minY, minZ).endVertex()
        worldRenderer.pos(maxX, minY, minZ).endVertex()
        worldRenderer.pos(maxX, minY, maxZ).endVertex()
        worldRenderer.pos(minX, minY, maxZ).endVertex()
        worldRenderer.pos(minX, minY, minZ).endVertex()
        worldRenderer.pos(minX, minY, maxZ).endVertex()
        worldRenderer.pos(maxX, minY, maxZ).endVertex()
        worldRenderer.pos(maxX, minY, minZ).endVertex()
        worldRenderer.pos(minX, minY, minZ).endVertex()
        worldRenderer.pos(minX, maxY, minZ).endVertex()
        worldRenderer.pos(minX, minY, maxZ).endVertex()
        worldRenderer.pos(minX, maxY, maxZ).endVertex()
        worldRenderer.pos(maxX, minY, maxZ).endVertex()
        worldRenderer.pos(maxX, maxY, maxZ).endVertex()
        worldRenderer.pos(maxX, minY, minZ).endVertex()
        worldRenderer.pos(maxX, maxY, minZ).endVertex()
        worldRenderer.pos(minX, maxY, maxZ).endVertex()
        worldRenderer.pos(minX, minY, maxZ).endVertex()
        worldRenderer.pos(minX, maxY, minZ).endVertex()
        worldRenderer.pos(minX, minY, minZ).endVertex()
        worldRenderer.pos(maxX, maxY, minZ).endVertex()
        worldRenderer.pos(maxX, minY, minZ).endVertex()
        worldRenderer.pos(maxX, maxY, maxZ).endVertex()
        worldRenderer.pos(maxX, minY, maxZ).endVertex()
        tessellator.draw()
    }

    @JvmStatic
    fun quickDrawRect(x: Float, y: Float, x2: Float, y2: Float)
    {
        GL11.glBegin(GL11.GL_QUADS)

        GL11.glVertex2f(x2, y)
        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x, y2)
        GL11.glVertex2f(x2, y2)

        GL11.glEnd()
    }

    @JvmStatic
    fun drawRect(x: Float, y: Float, x2: Float, y2: Float, color: Int)
    {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)

        glColor(color)

        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(x2, y)
        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x, y2)
        GL11.glVertex2f(x2, y2)
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
    }

    @JvmStatic
    fun drawRect(x: Int, y: Int, x2: Int, y2: Int, color: Int)
    {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)

        glColor(color)

        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2i(x2, y)
        GL11.glVertex2i(x, y)
        GL11.glVertex2i(x, y2)
        GL11.glVertex2i(x2, y2)
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
    }

    /**
     * Like [.drawRect], but without setup
     */
    @JvmStatic
    fun quickDrawRect(x: Float, y: Float, x2: Float, y2: Float, color: Int)
    {
        glColor(color)

        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2f(x2, y)
        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x, y2)
        GL11.glVertex2f(x2, y2)
        GL11.glEnd()
    }

    @JvmStatic
    fun drawRect(x: Float, y: Float, x2: Float, y2: Float, color: Color)
    {
        drawRect(x, y, x2, y2, color.rgb)
    }

    @JvmStatic
    fun drawBorderedRect(x: Float, y: Float, x2: Float, y2: Float, width: Float, borderColor: Int, rectColor: Int)
    {
        drawRect(x, y, x2, y2, rectColor)
        drawBorder(x, y, x2, y2, width, borderColor)
    }

    @JvmStatic
    fun drawBorder(x: Float, y: Float, x2: Float, y2: Float, width: Float, color1: Int)
    {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)

        glColor(color1)

        GL11.glLineWidth(width)

        GL11.glBegin(GL11.GL_LINE_LOOP)
        GL11.glVertex2f(x2, y)
        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x, y2)
        GL11.glVertex2f(x2, y2)
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
    }

    @JvmStatic
    fun quickDrawBorderedRect(x: Float, y: Float, x2: Float, y2: Float, width: Float, borderColor: Int, rectColor: Int)
    {
        quickDrawRect(x, y, x2, y2, rectColor)

        glColor(borderColor)

        GL11.glLineWidth(width)

        GL11.glBegin(GL11.GL_LINE_LOOP)
        GL11.glVertex2f(x2, y)
        GL11.glVertex2f(x, y)
        GL11.glVertex2f(x, y2)
        GL11.glVertex2f(x2, y2)
        GL11.glEnd()
    }

    @JvmStatic
    fun drawLoadingCircle(x: Float, y: Float)
    {
        repeat(4) {
            val rot = (System.nanoTime() * 0.0000002 * it % 360).toInt()
            drawCircle(x, y, it * 10f, rot - 180, rot)
        }
    }

    @JvmStatic
    private fun drawCircle(x: Float, y: Float, radius: Float, start: Int, end: Int)
    {
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)

        glColor(Color.WHITE)

        GL11.glEnable(GL11.GL_LINE_SMOOTH)

        GL11.glLineWidth(2.00f)

        GL11.glBegin(GL11.GL_LINE_STRIP)

        var current = end.toFloat()

        while (current >= start)
        {
            val radians = current.toRadians
            GL11.glVertex2f(x + radians.cos * (radius * 1.001f), y + radians.sin * (radius * 1.001f))
            current -= 360 / 90.0f
        }

        GL11.glEnd()

        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    @JvmStatic
    fun drawFilledCircle(x: Int, y: Int, radius: Float, color: Int)
    {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)

        GL11.glBegin(GL11.GL_TRIANGLE_FAN)

        val sections = 50
        val dAngle = 2.0f * PI / sections

        glColor(color)

        repeat(sections) {
            val circleX = radius * (it * dAngle).sin
            val circleY = radius * (it * dAngle).cos
            GL11.glVertex2f(x + circleX, y + circleY)
        }

        resetColor()

        GL11.glEnd()

        GL11.glPopAttrib()
    }

    @JvmStatic
    fun drawImage(image: ResourceLocation, x: Int, y: Int, width: Int, height: Int)
    {
        drawImage(image, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
    }

    @JvmStatic
    fun drawImage(image: ResourceLocation, x: Float, y: Float, width: Float, height: Float)
    {
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDepthMask(false)
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)

        resetColor()

        mc.textureManager.bindTexture(image)

        drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, width, height, width, height)

        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
    }

    /**
     * Draws a textured rectangle at z = 0.
     * Args: x, y, u, v, width, height, textureWidth, textureHeight
     */
    @JvmStatic
    fun drawModalRectWithCustomSizedTexture(x: Float, y: Float, u: Float, v: Float, width: Float, height: Float, textureWidth: Float, textureHeight: Float)
    {
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer

        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX)

        val xD = x.toDouble()
        val yD = y.toDouble()
        val uD = u.toDouble()
        val vD = v.toDouble()

        val reverseTileWidth = 1.0f / textureWidth
        val reverseTileHeight = 1.0f / textureHeight

        worldrenderer.pos(xD, yD + height, 0.0).tex(uD * reverseTileWidth, (vD + height) * reverseTileHeight).endVertex()
        worldrenderer.pos(xD + width, yD + height, 0.0).tex((uD + width) * reverseTileWidth, (vD + height) * reverseTileHeight).endVertex()
        worldrenderer.pos(xD + width, yD, 0.0).tex((uD + width) * reverseTileWidth, vD * reverseTileHeight).endVertex()
        worldrenderer.pos(xD, yD, 0.0).tex(uD * reverseTileWidth, vD * reverseTileHeight).endVertex()
        tessellator.draw()
    }

    @JvmStatic
    fun drawScaledCustomSizeModalRect(x: Float, y: Float, u: Float, v: Float, uWidth: Float, vHeight: Float, width: Float, height: Float, tileWidth: Float, tileHeight: Float)
    {
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer

        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX)

        val xD = x.toDouble()
        val yD = y.toDouble()
        val uD = u.toDouble()
        val vD = v.toDouble()

        val reverseTileWidth = 1.0f / tileWidth
        val reverseTileHeight = 1.0f / tileHeight

        worldrenderer.pos(xD, yD + height, 0.0).tex(uD * reverseTileWidth, (vD + vHeight) * reverseTileHeight).endVertex()
        worldrenderer.pos(xD + width, yD + height, 0.0).tex((uD + uWidth) * reverseTileWidth, (vD + vHeight) * reverseTileHeight).endVertex()
        worldrenderer.pos(xD + width, yD, 0.0).tex((uD + uWidth) * reverseTileWidth, vD * reverseTileHeight).endVertex()
        worldrenderer.pos(xD, yD, 0.0).tex(uD * reverseTileWidth, vD * reverseTileHeight).endVertex()
        tessellator.draw()
    }

    @JvmStatic
    fun glColor(red: Int, green: Int, blue: Int, alpha: Int)
    {
        GL11.glColor4f(red / 255F, green / 255F, blue / 255F, alpha / 255F)
    }

    @JvmStatic
    fun glColor(color: Color)
    {
        glColor(color.red, color.green, color.blue, color.alpha)
    }

    @JvmStatic
    fun glColor(hex: Int)
    {
        glColor(hex shr 16 and 0xFF, hex shr 8 and 0xFF, hex and 0xFF, hex shr 24 and 0xFF)
    }

    @JvmStatic
    fun resetColor()
    {
        glColor(255, 255, 255, 255)
    }

    @JvmStatic
    fun draw2D(entity: Entity, posX: Double, posY: Double, posZ: Double, color: Int, backgroundColor: Int)
    {
        GL11.glPushMatrix()
        GL11.glTranslated(posX, posY, posZ)
        GL11.glRotated(-mc.renderManager.playerViewY.toDouble(), 0.00, 1.00, 0.00)
        GL11.glScaled(-0.1, -0.1, 0.1)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glDepthMask(true)

        glColor(color)

        GL11.glCallList(DISPLAY_LISTS_2D[0])

        glColor(backgroundColor)

        GL11.glCallList(DISPLAY_LISTS_2D[1])
        GL11.glTranslated(0.0, 21 + -(entity.entityBoundingBox.maxY - entity.entityBoundingBox.minY) * 12, 0.0)

        glColor(color)

        GL11.glCallList(DISPLAY_LISTS_2D[2])

        glColor(backgroundColor)

        GL11.glCallList(DISPLAY_LISTS_2D[3])

        // Stop render
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }

    @JvmStatic
    fun draw2D(blockPos: BlockPos, color: Int, backgroundColor: Int)
    {
        val renderManager = mc.renderManager
        val posX = blockPos.x + 0.5 - renderManager.renderPosX
        val posY = blockPos.y - renderManager.renderPosY
        val posZ = blockPos.z + 0.5 - renderManager.renderPosZ

        GL11.glPushMatrix()
        GL11.glTranslated(posX, posY, posZ)
        GL11.glRotated(-renderManager.playerViewY.toDouble(), 0.00, 1.00, 0.00)
        GL11.glScaled(-0.1, -0.1, 0.1)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glDepthMask(true)

        glColor(color)

        GL11.glCallList(DISPLAY_LISTS_2D[0])

        glColor(backgroundColor)

        GL11.glCallList(DISPLAY_LISTS_2D[1])
        GL11.glTranslated(0.0, 9.0, 0.0)

        glColor(color)

        GL11.glCallList(DISPLAY_LISTS_2D[2])

        glColor(backgroundColor)

        GL11.glCallList(DISPLAY_LISTS_2D[3])

        // Stop render
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }

    @JvmStatic
    fun renderNameTag(nameTag: String, x: Double, y: Double, z: Double)
    {
        val renderManager = mc.renderManager

        GL11.glPushMatrix()
        GL11.glTranslated(x - renderManager.renderPosX, y - renderManager.renderPosY, z - renderManager.renderPosZ)
        GL11.glNormal3f(0.00f, 1.00f, 0.00f)
        GL11.glRotatef(-mc.renderManager.playerViewY, 0.00f, 1.00f, 0.00f)
        GL11.glRotatef(mc.renderManager.playerViewX, 1.00f, 0.00f, 0.00f)
        GL11.glScalef(-0.05f, -0.05f, 0.05f)

        setGlCap(GL11.GL_LIGHTING, false)
        setGlCap(GL11.GL_DEPTH_TEST, false)
        setGlCap(GL11.GL_BLEND, true)

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        val width = Fonts.font35.getStringWidth(nameTag) shr 1

        drawRect(-width - 1, -1, width + 1, Fonts.font35.fontHeight, Int.MIN_VALUE)
        Fonts.font35.drawString(nameTag, -width.toFloat(), 1.5f, -1, true)

        resetCaps()
        resetColor()
        GL11.glPopMatrix()
    }

    @JvmStatic
    fun drawLine(x: Double, y: Double, x1: Double, y1: Double, width: Float)
    {
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glLineWidth(width)

        GL11.glBegin(GL11.GL_LINES)
        GL11.glVertex2d(x, y)
        GL11.glVertex2d(x1, y1)
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
    }

    @JvmStatic
    fun makeScissorBox(x: Float, y: Float, x2: Float, y2: Float)
    {
        val scaledResolution = ScaledResolution(mc)
        val factor = scaledResolution.scaleFactor

        GL11.glScissor((x * factor).toInt(), ((scaledResolution.scaledHeight - y2) * factor).toInt(), ((x2 - x) * factor).toInt(), ((y2 - y) * factor).toInt())
    }

    /**
     * GL CAP MANAGER
     *
     *
     * TODO: Remove gl cap manager and replace by something better
     */
    @JvmStatic
    fun resetCaps()
    {
        glCapMap.forEach { setGlState(it.key, it.value) }
    }

    @JvmStatic
    fun enableGlCap(cap: Int)
    {
        setGlCap(cap, true)
    }

    @JvmStatic
    fun enableGlCap(vararg caps: Int)
    {
        for (cap in caps) setGlCap(cap, true)
    }

    // fun disableGlCap(cap: Int)
    // {
    // 	setGlCap(cap, true)
    // }

    @JvmStatic
    fun disableGlCap(vararg caps: Int)
    {
        for (cap in caps) setGlCap(cap, false)
    }

    @JvmStatic
    fun setGlCap(cap: Int, state: Boolean)
    {
        glCapMap[cap] = GL11.glGetBoolean(cap)
        setGlState(cap, state)
    }

    @JvmStatic
    fun setGlState(cap: Int, state: Boolean)
    {
        if (state) GL11.glEnable(cap) else GL11.glDisable(cap)
    }

    @JvmStatic
    fun drawFoVCircle(fov: Float)
    {
        if (mc.gameSettings.thirdPersonView > 0) return

        val scaledResolution = ScaledResolution(mc)

        drawCircle((scaledResolution.scaledWidth shr 1).toFloat(), (scaledResolution.scaledHeight shr 1).toFloat(), fov * 6.0f / (mc.gameSettings.fovSetting / 70.0f), 0, 360)
    }

    @JvmStatic
    fun drawHorizontalLine(startX: Int, endX: Int, y: Int, color: Int)
    {
        drawRect(min(startX, endX), y, max(startX, endX) + 1, y + 1, color)
    }

    @JvmStatic
    fun drawItemStack(itemStack: ItemStack, posX: Int, posY: Int)
    {
        GlStateManager.enableRescaleNormal()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        RenderHelper.enableGUIStandardItemLighting()

        mc.renderItem.renderItemAndEffectIntoGUI(itemStack, posX, posY)
        renderItemOverlays(Fonts.minecraftFont, itemStack, posX, posY)

        RenderHelper.disableStandardItemLighting()
    }

    @JvmStatic
    fun drawRadius(radius: Float, loops: Float, lineWidth: Float, color: Int)
    {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glLineWidth(lineWidth)
        glColor(color)
        GL11.glRotatef(90F, 1F, 0F, 0F)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        var i = 0F
        while (i <= 360F)
        {
            GL11.glVertex2f(i.toRadians.cos * radius, i.toRadians.sin * radius)
            i += loops
        }

        GL11.glEnd()
        resetColor()
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
    }

    @JvmStatic
    fun renderItemEnchantments(font: FontRenderer, stack: ItemStack, x: Int, y: Int)
    {
        val texts = EnchantmentHelper.getEnchantments(stack).asSequence().map { (id, lvl) -> Maps.ENCHANTMENT_SHORT_NAME[id]?.let { "${it.lowercase(Locale.getDefault())}$lvl" } ?: "" }.chunked(2).take(3)

        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()

        val scale = 0.45f
        val reverseScale = 1f / scale

        val textX = x * reverseScale
        val textY = y * reverseScale
        val fontHeight = font.FONT_HEIGHT + 2

        GL11.glScalef(scale, scale, scale)
        texts.forEachIndexed { index, text -> font.drawString(text.joinToString(separator = " "), textX, textY + index * fontHeight, 16777215, false) }
        GL11.glScalef(reverseScale, reverseScale, reverseScale)

        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }

    @JvmStatic
    fun renderItemOverlays(font: FontRenderer, stack: ItemStack, x: Int, y: Int)
    {
        if (stack.stackSize != 1)
        {
            val text = "${if (stack.stackSize < 1) "\u00A7c" else ""}${stack.stackSize}"

            GlStateManager.disableLighting()
            GlStateManager.disableDepth()
            GlStateManager.disableBlend()

            font.drawStringWithShadow(text, x + 19 - 2 - font.getStringWidth(text), y + 6 + 3, 16777215)

            GlStateManager.enableLighting()
            GlStateManager.enableDepth()
        }

        stack.item?.let { item ->
            if (item.showDurabilityBar(stack))
            {
                val bar = item.getDurabilityForDisplay(stack)
                val j = (13.0 - bar * 13.0).roundToInt()
                val i = (255.0 - bar * 255.0).roundToInt()

                GlStateManager.disableLighting()
                GlStateManager.disableDepth()
                GlStateManager.disableTexture2D()
                GlStateManager.disableAlpha()
                GlStateManager.disableBlend()

                val tessellator = Tessellator.getInstance()

                // TODO: Change to drawRect()
                val worldrenderer = tessellator.worldRenderer
                draw(worldrenderer, x + 2, y + 13, 13, 2, 0, 0, 0, 255)
                draw(worldrenderer, x + 2, y + 13, 12, 1, (255 - i) / 4, 64, 0, 255)
                draw(worldrenderer, x + 2, y + 13, j, 1, 255 - i, i, 0, 255)

                GlStateManager.enableAlpha()
                GlStateManager.enableTexture2D()
                GlStateManager.enableLighting()
                GlStateManager.enableDepth()
            }
        }

        renderItemEnchantments(font, stack, x + 2, y + 2)
    }

    private fun draw(renderer: WorldRenderer, x: Int, y: Int, width: Int, height: Int, red: Int, green: Int, blue: Int, alpha: Int)
    {
        renderer.begin(7, DefaultVertexFormats.POSITION_COLOR)
        renderer.pos(x.toDouble(), y.toDouble(), 0.0).color(red, green, blue, alpha).endVertex()
        renderer.pos(x.toDouble(), (y + height).toDouble(), 0.0).color(red, green, blue, alpha).endVertex()
        renderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0).color(red, green, blue, alpha).endVertex()
        renderer.pos((x + width).toDouble(), y.toDouble(), 0.0).color(red, green, blue, alpha).endVertex()
        Tessellator.getInstance().draw()
    }

    init
    {
        for (i in DISPLAY_LISTS_2D.indices) DISPLAY_LISTS_2D[i] = GL11.glGenLists(1)

        GL11.glNewList(DISPLAY_LISTS_2D[0], GL11.GL_COMPILE)
        quickDrawRect(-7.00f, 2.00f, -4.00f, 3.00f)
        quickDrawRect(4.00f, 2.00f, 7.00f, 3.00f)
        quickDrawRect(-7.00f, 0.5f, -6.00f, 3.00f)
        quickDrawRect(6.00f, 0.5f, 7.00f, 3.00f)
        GL11.glEndList()

        GL11.glNewList(DISPLAY_LISTS_2D[1], GL11.GL_COMPILE)
        quickDrawRect(-7.00f, 3.00f, -4.00f, 3.3f)
        quickDrawRect(4.00f, 3.00f, 7.00f, 3.3f)
        quickDrawRect(-7.3f, 0.5f, -7.00f, 3.3f)
        quickDrawRect(7.00f, 0.5f, 7.3f, 3.3f)
        GL11.glEndList()

        GL11.glNewList(DISPLAY_LISTS_2D[2], GL11.GL_COMPILE)
        quickDrawRect(4.00f, -20.00f, 7.00f, -19.00f)
        quickDrawRect(-7.00f, -20.00f, -4.00f, -19.00f)
        quickDrawRect(6.00f, -20.00f, 7.00f, -17.5f)
        quickDrawRect(-7.00f, -20.00f, -6.00f, -17.5f)
        GL11.glEndList()

        GL11.glNewList(DISPLAY_LISTS_2D[3], GL11.GL_COMPILE)
        quickDrawRect(7.00f, -20.00f, 7.3f, -17.5f)
        quickDrawRect(-7.3f, -20.00f, -7.00f, -17.5f)
        quickDrawRect(4.00f, -20.3f, 7.3f, -20.00f)
        quickDrawRect(-7.3f, -20.3f, -4.00f, -20.00f)
        GL11.glEndList()
    }
}
