/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

object RenderUtils : MinecraftInstance() {
    private val glCapMap = mutableMapOf<Int, Boolean>()
    private val DISPLAY_LISTS_2D = IntArray(4)
    var deltaTime = 0

    init {
        for (i in DISPLAY_LISTS_2D.indices) {
            DISPLAY_LISTS_2D[i] = glGenLists(1)
        }

        glNewList(DISPLAY_LISTS_2D[0], GL_COMPILE)
        quickDrawRect(-7f, 2f, -4f, 3f)
        quickDrawRect(4f, 2f, 7f, 3f)
        quickDrawRect(-7f, 0.5f, -6f, 3f)
        quickDrawRect(6f, 0.5f, 7f, 3f)
        glEndList()
        glNewList(DISPLAY_LISTS_2D[1], GL_COMPILE)
        quickDrawRect(-7f, 3f, -4f, 3.3f)
        quickDrawRect(4f, 3f, 7f, 3.3f)
        quickDrawRect(-7.3f, 0.5f, -7f, 3.3f)
        quickDrawRect(7f, 0.5f, 7.3f, 3.3f)
        glEndList()
        glNewList(DISPLAY_LISTS_2D[2], GL_COMPILE)
        quickDrawRect(4f, -20f, 7f, -19f)
        quickDrawRect(-7f, -20f, -4f, -19f)
        quickDrawRect(6f, -20f, 7f, -17.5f)
        quickDrawRect(-7f, -20f, -6f, -17.5f)
        glEndList()
        glNewList(DISPLAY_LISTS_2D[3], GL_COMPILE)
        quickDrawRect(7f, -20f, 7.3f, -17.5f)
        quickDrawRect(-7.3f, -20f, -7f, -17.5f)
        quickDrawRect(4f, -20.3f, 7.3f, -20f)
        quickDrawRect(-7.3f, -20.3f, -4f, -20f)
        glEndList()
    }

    fun drawBlockBox(blockPos: BlockPos, color: Color, outline: Boolean) {
        val renderManager = mc.renderManager
        val timer = mc.timer

        val x = blockPos.x - renderManager.renderPosX
        val y = blockPos.y - renderManager.renderPosY
        val z = blockPos.z - renderManager.renderPosZ

        var axisAlignedBB = AxisAlignedBB.fromBounds(x, y, z, x + 1.0, y + 1.0, z + 1.0)
        val block = getBlock(blockPos)
        if (block != null) {
            val player = mc.thePlayer
            val posX = player.lastTickPosX + (player.posX - player.lastTickPosX) * timer.renderPartialTicks.toDouble()
            val posY = player.lastTickPosY + (player.posY - player.lastTickPosY) * timer.renderPartialTicks.toDouble()
            val posZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * timer.renderPartialTicks.toDouble()
            block.setBlockBoundsBasedOnState(mc.theWorld, blockPos)
            axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
                .expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
                .offset(-posX, -posY, -posZ)
        }

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        enableGlCap(GL_BLEND)
        disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST)
        glDepthMask(false)
        glColor(color.red, color.green, color.blue, if (color.alpha != 255) color.alpha else if (outline) 26 else 35)
        drawFilledBox(axisAlignedBB)

        if (outline) {
            glLineWidth(1f)
            enableGlCap(GL_LINE_SMOOTH)
            glColor(color)
            drawSelectionBoundingBox(axisAlignedBB)
        }

        glColor4f(1f, 1f, 1f, 1f)
        glDepthMask(true)
        resetCaps()
    }

    fun drawSelectionBoundingBox(boundingBox: AxisAlignedBB) {
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)

        // Lower Rectangle
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex()
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex()
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex()
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex()
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).endVertex()

        // Upper Rectangle
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex()
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex()
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex()
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex()
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).endVertex()

        // Upper Rectangle
        worldRenderer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).endVertex()
        worldRenderer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).endVertex()
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).endVertex()
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).endVertex()
        worldRenderer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).endVertex()
        worldRenderer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).endVertex()
        tessellator.draw()
    }

    fun drawEntityBox(entity: Entity, color: Color, outline: Boolean) {
        val renderManager = mc.renderManager
        val timer = mc.timer
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        enableGlCap(GL_BLEND)
        disableGlCap(GL_TEXTURE_2D, GL_DEPTH_TEST)
        glDepthMask(false)
        val x = (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks
                - renderManager.renderPosX)
        val y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks
                - renderManager.renderPosY)
        val z = (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks
                - renderManager.renderPosZ)
        val entityBox = entity.hitBox
        val axisAlignedBB = AxisAlignedBB.fromBounds(
            entityBox.minX - entity.posX + x - 0.05,
            entityBox.minY - entity.posY + y,
            entityBox.minZ - entity.posZ + z - 0.05,
            entityBox.maxX - entity.posX + x + 0.05,
            entityBox.maxY - entity.posY + y + 0.15,
            entityBox.maxZ - entity.posZ + z + 0.05
        )
        if (outline) {
            glLineWidth(1f)
            enableGlCap(GL_LINE_SMOOTH)
            glColor(color.red, color.green, color.blue, 95)
            drawSelectionBoundingBox(axisAlignedBB)
        }
        glColor(color.red, color.green, color.blue, if (outline) 26 else 35)
        drawFilledBox(axisAlignedBB)
        glColor4f(1f, 1f, 1f, 1f)
        glDepthMask(true)
        resetCaps()
    }

    fun drawAxisAlignedBB(axisAlignedBB: AxisAlignedBB, color: Color) {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glLineWidth(2f)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)
        glColor(color)
        drawFilledBox(axisAlignedBB)
        glColor4f(1f, 1f, 1f, 1f)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDisable(GL_BLEND)
    }

    fun drawPlatform(y: Double, color: Color, size: Double) {
        val renderManager = mc.renderManager
        val renderY = y - renderManager.renderPosY
        drawAxisAlignedBB(AxisAlignedBB.fromBounds(size, renderY + 0.02, size, -size, renderY, -size), color)
    }

    fun drawPlatform(entity: Entity, color: Color) {
        val renderManager = mc.renderManager
        val timer = mc.timer
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
        val axisAlignedBB = entity.entityBoundingBox
            .offset(-entity.posX, -entity.posY, -entity.posZ)
            .offset(x, y, z)
        drawAxisAlignedBB(
            AxisAlignedBB.fromBounds(
                axisAlignedBB.minX,
                axisAlignedBB.maxY + 0.2,
                axisAlignedBB.minZ,
                axisAlignedBB.maxX,
                axisAlignedBB.maxY + 0.26,
                axisAlignedBB.maxZ
            ), color
        )
    }

    fun drawFilledBox(axisAlignedBB: AxisAlignedBB) {
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(7, DefaultVertexFormats.POSITION)
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).endVertex()
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).endVertex()
        tessellator.draw()
    }

    fun quickDrawRect(x: Float, y: Float, x2: Float, y2: Float) {
        glBegin(GL_QUADS)
        glVertex2d(x2.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y2.toDouble())
        glVertex2d(x2.toDouble(), y2.toDouble())
        glEnd()
    }

    fun drawRect(x: Float, y: Float, x2: Float, y2: Float, color: Int) {
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glColor(color)
        glBegin(GL_QUADS)
        glVertex2f(x2, y)
        glVertex2f(x, y)
        glVertex2f(x, y2)
        glVertex2f(x2, y2)
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
    }

    fun drawRect(x: Int, y: Int, x2: Int, y2: Int, color: Int) {
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glColor(color)
        glBegin(GL_QUADS)
        glVertex2i(x2, y)
        glVertex2i(x, y)
        glVertex2i(x, y2)
        glVertex2i(x2, y2)
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
    }

    /**
     * Like [.drawRect], but without setup
     */
    fun quickDrawRect(x: Float, y: Float, x2: Float, y2: Float, color: Int) {
        glColor(color)
        glBegin(GL_QUADS)
        glVertex2d(x2.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y2.toDouble())
        glVertex2d(x2.toDouble(), y2.toDouble())
        glEnd()
    }

    fun drawRect(x: Float, y: Float, x2: Float, y2: Float, color: Color) = drawRect(x, y, x2, y2, color.rgb)

    fun drawBorderedRect(x: Float, y: Float, x2: Float, y2: Float, width: Float, color1: Int, color2: Int) {
        drawRect(x, y, x2, y2, color2)
        drawBorder(x, y, x2, y2, width, color1)
    }

    fun drawBorderedRect(x: Int, y: Int, x2: Int, y2: Int, width: Int, borderColor: Int, rectColor: Int) {
        drawRect(x, y, x2, y2, rectColor)
        drawBorder(x, y, x2, y2, width, borderColor)
    }

    fun drawBorder(x: Float, y: Float, x2: Float, y2: Float, width: Float, color: Int) {
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glColor(color)
        glLineWidth(width)
        glBegin(GL_LINE_LOOP)
        glVertex2d(x2.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y2.toDouble())
        glVertex2d(x2.toDouble(), y2.toDouble())
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
    }

    fun drawBorder(x: Int, y: Int, x2: Int, y2: Int, width: Int, color: Int) {
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glColor(color)
        glLineWidth(width.toFloat())
        glBegin(GL_LINE_LOOP)
        glVertex2i(x2, y)
        glVertex2i(x, y)
        glVertex2i(x, y2)
        glVertex2i(x2, y2)
        glEnd()
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)
    }

    fun quickDrawBorderedRect(x: Float, y: Float, x2: Float, y2: Float, width: Float, color1: Int, color2: Int) {
        quickDrawRect(x, y, x2, y2, color2)
        glColor(color1)
        glLineWidth(width)
        glBegin(GL_LINE_LOOP)
        glVertex2d(x2.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y.toDouble())
        glVertex2d(x.toDouble(), y2.toDouble())
        glVertex2d(x2.toDouble(), y2.toDouble())
        glEnd()
    }

    fun drawLoadingCircle(x: Float, y: Float) {
        for (i in 0..3) {
            val rot = (System.nanoTime() / 5000000 * i % 360).toInt()
            drawCircle(x, y, (i * 10).toFloat(), rot - 180, rot)
        }
    }

    fun drawCircle(x: Float, y: Float, radius: Float, start: Int, end: Int) {
        enableBlend()
        disableTexture2D()
        tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor(Color.WHITE)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(2f)
        glBegin(GL_LINE_STRIP)
        var i = end.toFloat()
        while (i >= start) {
            val rad = i.toRadians()
            glVertex2f(
                x + cos(rad) * (radius * 1.001f),
                y + sin(rad) * (radius * 1.001f)
            )
            i -= 360 / 90f
        }
        glEnd()
        glDisable(GL_LINE_SMOOTH)
        enableTexture2D()
        disableBlend()
    }

    fun drawFilledCircle(xx: Int, yy: Int, radius: Float, color: Color) {
        val sections = 50
        val dAngle = 2 * Math.PI / sections
        var x: Float
        var y: Float
        glPushAttrib(GL_ENABLE_BIT)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glBegin(GL_TRIANGLE_FAN)
        for (i in 0 until sections) {
            x = (radius * sin(i * dAngle)).toFloat()
            y = (radius * cos(i * dAngle)).toFloat()
            glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
            glVertex2f(xx + x, yy + y)
        }
        glColor4f(1f, 1f, 1f, 1f)
        glEnd()
        glPopAttrib()
    }

    fun drawImage(image: ResourceLocation?, x: Int, y: Int, width: Int, height: Int) {
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDepthMask(false)
        GL14.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(1f, 1f, 1f, 1f)
        mc.textureManager.bindTexture(image)
        drawModalRectWithCustomSizedTexture(
            x.toFloat(),
            y.toFloat(),
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            width.toFloat(),
            height.toFloat()
        )
        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    /**
     * Draws a textured rectangle at z = 0. Args: x, y, u, v, width, height, textureWidth, textureHeight
     */
    fun drawModalRectWithCustomSizedTexture(
        x: Float,
        y: Float,
        u: Float,
        v: Float,
        width: Float,
        height: Float,
        textureWidth: Float,
        textureHeight: Float
    ) {
        val f = 1f / textureWidth
        val f1 = 1f / textureHeight
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
        worldrenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0)
            .tex((u * f).toDouble(), ((v + height) * f1).toDouble()).endVertex()
        worldrenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0)
            .tex(((u + width) * f).toDouble(), ((v + height) * f1).toDouble()).endVertex()
        worldrenderer.pos((x + width).toDouble(), y.toDouble(), 0.0)
            .tex(((u + width) * f).toDouble(), (v * f1).toDouble()).endVertex()
        worldrenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex((u * f).toDouble(), (v * f1).toDouble()).endVertex()
        tessellator.draw()
    }

    /**
     * Draws a textured rectangle at the stored z-value. Args: x, y, u, v, width, height.
     */
    fun drawTexturedModalRect(x: Int, y: Int, textureX: Int, textureY: Int, width: Int, height: Int, zLevel: Float) {
        val f = 0.00390625f
        val f1 = 0.00390625f
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
        worldrenderer.pos(x.toDouble(), (y + height).toDouble(), zLevel.toDouble()).tex((textureX.toFloat() * f).toDouble(), ((textureY + height).toFloat() * f1).toDouble()).endVertex()
        worldrenderer.pos((x + width).toDouble(), (y + height).toDouble(), zLevel.toDouble()).tex(((textureX + width).toFloat() * f).toDouble(), ((textureY + height).toFloat() * f1).toDouble()).endVertex()
        worldrenderer.pos((x + width).toDouble(), y.toDouble(), zLevel.toDouble()).tex(((textureX + width).toFloat() * f).toDouble(), (textureY.toFloat() * f1).toDouble()).endVertex()
        worldrenderer.pos(x.toDouble(), y.toDouble(), zLevel.toDouble()).tex((textureX.toFloat() * f).toDouble(), (textureY.toFloat() * f1).toDouble()).endVertex()
        tessellator.draw()
    }

    fun glColor(red: Int, green: Int, blue: Int, alpha: Int) =
        glColor4f(red / 255f, green / 255f, blue / 255f, alpha / 255f)

    fun glColor(color: Color) = glColor(color.red, color.green, color.blue, color.alpha)

    private fun glColor(hex: Int) =
        glColor(hex shr 16 and 0xFF, hex shr 8 and 0xFF, hex and 0xFF, hex shr 24 and 0xFF)

    fun draw2D(entity: EntityLivingBase, posX: Double, posY: Double, posZ: Double, color: Int, backgroundColor: Int) {
        glPushMatrix()
        glTranslated(posX, posY, posZ)
        glRotated(-mc.renderManager.playerViewY.toDouble(), 0.0, 1.0, 0.0)
        glScaled(-0.1, -0.1, 0.1)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDepthMask(true)
        glColor(color)
        glCallList(DISPLAY_LISTS_2D[0])
        glColor(backgroundColor)
        glCallList(DISPLAY_LISTS_2D[1])
        glTranslated(0.0, 21 + -(entity.entityBoundingBox.maxY - entity.entityBoundingBox.minY) * 12, 0.0)
        glColor(color)
        glCallList(DISPLAY_LISTS_2D[2])
        glColor(backgroundColor)
        glCallList(DISPLAY_LISTS_2D[3])

        // Stop render
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glPopMatrix()
    }

    fun draw2D(blockPos: BlockPos, color: Int, backgroundColor: Int) {
        val renderManager = mc.renderManager
        val posX = blockPos.x + 0.5 - renderManager.renderPosX
        val posY = blockPos.y - renderManager.renderPosY
        val posZ = blockPos.z + 0.5 - renderManager.renderPosZ
        glPushMatrix()
        glTranslated(posX, posY, posZ)
        glRotated(-mc.renderManager.playerViewY.toDouble(), 0.0, 1.0, 0.0)
        glScaled(-0.1, -0.1, 0.1)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDepthMask(true)
        glColor(color)
        glCallList(DISPLAY_LISTS_2D[0])
        glColor(backgroundColor)
        glCallList(DISPLAY_LISTS_2D[1])
        glTranslated(0.0, 9.0, 0.0)
        glColor(color)
        glCallList(DISPLAY_LISTS_2D[2])
        glColor(backgroundColor)
        glCallList(DISPLAY_LISTS_2D[3])

        // Stop render
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glPopMatrix()
    }

    fun renderNameTag(string: String, x: Double, y: Double, z: Double) {
        val renderManager = mc.renderManager
        glPushMatrix()
        glTranslated(x - renderManager.renderPosX, y - renderManager.renderPosY, z - renderManager.renderPosZ)
        glNormal3f(0f, 1f, 0f)
        glRotatef(-mc.renderManager.playerViewY, 0f, 1f, 0f)
        glRotatef(mc.renderManager.playerViewX, 1f, 0f, 0f)
        glScalef(-0.05f, -0.05f, 0.05f)
        setGlCap(GL_LIGHTING, false)
        setGlCap(GL_DEPTH_TEST, false)
        setGlCap(GL_BLEND, true)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        val width = Fonts.font35.getStringWidth(string) / 2
        drawRect(-width - 1, -1, width + 1, Fonts.font35.FONT_HEIGHT, Int.MIN_VALUE)
        Fonts.font35.drawString(string, -width.toFloat(), 1.5f, Color.WHITE.rgb, true)
        resetCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }

    fun drawLine(x: Double, y: Double, x1: Double, y1: Double, width: Float) {
        glDisable(GL_TEXTURE_2D)
        glLineWidth(width)
        glBegin(GL_LINES)
        glVertex2d(x, y)
        glVertex2d(x1, y1)
        glEnd()
        glEnable(GL_TEXTURE_2D)
    }

    fun makeScissorBox(x: Float, y: Float, x2: Float, y2: Float) {
        val scaledResolution = ScaledResolution(mc)
        val factor = scaledResolution.scaleFactor
        glScissor(
            (x * factor).toInt(),
            ((scaledResolution.scaledHeight - y2) * factor).toInt(),
            ((x2 - x) * factor).toInt(),
            ((y2 - y) * factor).toInt()
        )
    }

    /**
     * GL CAP MANAGER
     *
     *
     * TODO: Remove gl cap manager and replace by something better
     */

    fun resetCaps() = glCapMap.forEach { (cap, state) -> setGlState(cap, state) }

    fun enableGlCap(cap: Int) = setGlCap(cap, true)

    fun enableGlCap(vararg caps: Int) {
        for (cap in caps) setGlCap(cap, true)
    }

    fun disableGlCap(cap: Int) = setGlCap(cap, true)

    fun disableGlCap(vararg caps: Int) {
        for (cap in caps) setGlCap(cap, false)
    }

    fun setGlCap(cap: Int, state: Boolean) {
        glCapMap[cap] = glGetBoolean(cap)
        setGlState(cap, state)
    }

    fun setGlState(cap: Int, state: Boolean) = if (state) glEnable(cap) else glDisable(cap)

    fun drawScaledCustomSizeModalRect(
        x: Int,
        y: Int,
        u: Float,
        v: Float,
        uWidth: Int,
        vHeight: Int,
        width: Int,
        height: Int,
        tileWidth: Float,
        tileHeight: Float
    ) {
        val f = 1f / tileWidth
        val f1 = 1f / tileHeight
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0)
            .tex((u * f).toDouble(), ((v + vHeight.toFloat()) * f1).toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0)
            .tex(((u + uWidth.toFloat()) * f).toDouble(), ((v + vHeight.toFloat()) * f1).toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), y.toDouble(), 0.0)
            .tex(((u + uWidth.toFloat()) * f).toDouble(), (v * f1).toDouble()).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex((u * f).toDouble(), (v * f1).toDouble()).endVertex()
        tessellator.draw()
    }
}