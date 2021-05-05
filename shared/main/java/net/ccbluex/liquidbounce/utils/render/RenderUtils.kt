/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.api.enums.WDefaultVertexFormats
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.cos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.sin
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper.toRadians
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.client.gui.Gui
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import java.awt.Color

object RenderUtils : MinecraftInstance()
{
	private val glCapMap = hashMapOf<Int, Boolean>()
	private val DISPLAY_LISTS_2D = IntArray(4)

	val ICONS: IResourceLocation = classProvider.createResourceLocation("textures/gui/icons.png")

	@JvmStatic
	var deltaTime = 0

	@JvmStatic
	fun drawBlockBox(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, blockPos: WBlockPos, color: Color, outline: Boolean, hydraESP: Boolean)
	{
		val renderPartialTicks = mc.timer.renderPartialTicks

		val block = getBlock(theWorld, blockPos)

		val lastTickPosX = thePlayer.lastTickPosX
		val lastTickPosY = thePlayer.lastTickPosY
		val lastTickPosZ = thePlayer.lastTickPosZ

		val posX = lastTickPosX + (thePlayer.posX - lastTickPosX) * renderPartialTicks
		val posY = lastTickPosY + (thePlayer.posY - lastTickPosY) * renderPartialTicks
		val posZ = lastTickPosZ + (thePlayer.posZ - lastTickPosZ) * renderPartialTicks

		if (Backend.MINECRAFT_VERSION_MINOR < 12) block.setBlockBoundsBasedOnState(theWorld, blockPos)

		val axisAlignedBB = block.getSelectedBoundingBox(theWorld, theWorld.getBlockState(blockPos), blockPos).expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026).offset(-posX, -posY, -posZ)

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		enableGlCap(GL11.GL_BLEND)
		disableGlCap(GL11.GL_TEXTURE_2D, GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(false)

		glColor(color.red, color.green, color.blue, if (color.alpha == 255) if (outline) 26 else 35 else color.alpha)

		drawFilledBox(axisAlignedBB)

		if (outline)
		{
			GL11.glLineWidth(1.00f)
			enableGlCap(GL11.GL_LINE_SMOOTH)
			glColor(color)
			drawSelectionBoundingBox(axisAlignedBB, hydraESP)
		}

		resetColor()
		GL11.glDepthMask(true)
		resetCaps()
	}

	@JvmStatic
	fun drawSelectionBoundingBox(boundingBox: IAxisAlignedBB, hydraESP: Boolean)
	{
		val provider = classProvider

		val tessellator = provider.tessellatorInstance
		val worldrenderer = tessellator.worldRenderer
		worldrenderer.begin(GL11.GL_LINE_STRIP, provider.getVertexFormatEnum(WDefaultVertexFormats.POSITION))

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

			worldrenderer.begin(GL11.GL_LINE_STRIP, provider.getVertexFormatEnum(WDefaultVertexFormats.POSITION))

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
	fun drawEntityBox(entity: IEntity, color: Color, outline: Boolean, drawHydraESP: Boolean)
	{
		val renderManager = mc.renderManager
		val renderPartialTicks = mc.timer.renderPartialTicks

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

		val x = lastTickPosX + (posX - lastTickPosX) * renderPartialTicks - renderManager.renderPosX
		val y = lastTickPosY + (posY - lastTickPosY) * renderPartialTicks - renderManager.renderPosY
		val z = lastTickPosZ + (posZ - lastTickPosZ) * renderPartialTicks - renderManager.renderPosZ

		val entityBox = entity.entityBoundingBox

		val axisAlignedBB = classProvider.createAxisAlignedBB(entityBox.minX - posX + x - 0.05, entityBox.minY - posY + y, entityBox.minZ - posZ + z - 0.05, entityBox.maxX - posX + x + 0.05, entityBox.maxY - posY + y + 0.15, entityBox.maxZ - posZ + z + 0.05)

		val red = color.red
		val green = color.green
		val blue = color.blue

		if (outline)
		{
			GL11.glLineWidth(1.00f)
			enableGlCap(GL11.GL_LINE_SMOOTH)
			glColor(red, green, blue, 95)
			drawSelectionBoundingBox(axisAlignedBB, drawHydraESP)
		}

		glColor(red, green, blue, if (outline) 26 else 35)

		drawFilledBox(axisAlignedBB)

		resetColor()
		GL11.glDepthMask(true)

		resetCaps()
	}

	@JvmStatic
	fun drawAxisAlignedBB(axisAlignedBB: IAxisAlignedBB, color: Color)
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
	fun drawPlatform(y: Double, color: Color, size: Double)
	{
		val renderManager = mc.renderManager
		val renderY = y - renderManager.renderPosY

		drawAxisAlignedBB(classProvider.createAxisAlignedBB(size, renderY + 0.02, size, -size, renderY, -size), color)
	}

	@JvmStatic
	fun drawPlatform(entity: IEntity, color: Color)
	{
		val renderManager = mc.renderManager
		val renderPartialTicks = mc.timer.renderPartialTicks

		val posX = entity.posX
		val posY = entity.posY
		val posZ = entity.posZ

		val lastTickPosX = entity.lastTickPosX
		val lastTickPosY = entity.lastTickPosY
		val lastTickPosZ = entity.lastTickPosZ

		val x = lastTickPosX + (posX - lastTickPosX) * renderPartialTicks - renderManager.renderPosX
		val y = lastTickPosY + (posY - lastTickPosY) * renderPartialTicks - renderManager.renderPosY
		val z = lastTickPosZ + (posZ - lastTickPosZ) * renderPartialTicks - renderManager.renderPosZ

		val axisAlignedBB = entity.entityBoundingBox.offset(-posX, -posY, -posZ).offset(x, y, z)

		drawAxisAlignedBB(classProvider.createAxisAlignedBB(axisAlignedBB.minX, axisAlignedBB.maxY + 0.2, axisAlignedBB.minZ, axisAlignedBB.maxX, axisAlignedBB.maxY + 0.26, axisAlignedBB.maxZ), color)
	}

	@JvmStatic
	fun drawFilledBox(axisAlignedBB: IAxisAlignedBB)
	{
		val provider = classProvider

		val tessellator = provider.tessellatorInstance
		val worldRenderer = tessellator.worldRenderer

		worldRenderer.begin(7, provider.getVertexFormatEnum(WDefaultVertexFormats.POSITION))
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
	fun drawBorderedRect(x: Float, y: Float, x2: Float, y2: Float, width: Float, color1: Int, color2: Int)
	{
		drawRect(x, y, x2, y2, color2)
		drawBorder(x, y, x2, y2, width, color1)
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
		val glStateManager = classProvider.glStateManager

		glStateManager.enableBlend()
		glStateManager.disableTexture2D()
		glStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)

		glColor(Color.WHITE)

		GL11.glEnable(GL11.GL_LINE_SMOOTH)

		GL11.glLineWidth(2.00f)

		GL11.glBegin(GL11.GL_LINE_STRIP)

		var current = end.toFloat()
		val func = functions

		while (current >= start)
		{
			val radians = toRadians(current)
			GL11.glVertex2f(x + func.cos(radians) * (radius * 1.001f), y + func.sin(radians) * (radius * 1.001f))
			current -= 360 / 90.0f
		}

		GL11.glEnd()

		GL11.glDisable(GL11.GL_LINE_SMOOTH)
		glStateManager.enableTexture2D()
		glStateManager.disableBlend()
	}

	@JvmStatic
	fun drawFilledCircle(x: Int, y: Int, radius: Float, color: Color)
	{
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glDisable(GL11.GL_TEXTURE_2D)
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		GL11.glEnable(GL11.GL_LINE_SMOOTH)

		GL11.glBegin(GL11.GL_TRIANGLE_FAN)

		val sections = 50
		val dAngle = 2.0f * WMathHelper.PI / sections

		glColor(color)

		repeat(sections) {
			val circleX = radius * sin(it * dAngle)
			val circleY = radius * cos(it * dAngle)
			GL11.glVertex2f(x + circleX, y + circleY)
		}

		resetColor()

		GL11.glEnd()

		GL11.glPopAttrib()
	}

	@JvmStatic
	fun drawImage(image: IResourceLocation, x: Int, y: Int, width: Int, height: Int)
	{
		GL11.glDisable(GL11.GL_DEPTH_TEST)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glDepthMask(false)
		GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)

		resetColor()

		mc.textureManager.bindTexture(image)

		val xF = x.toFloat()
		val yF = y.toFloat()
		val widthF = width.toFloat()
		val heightF = height.toFloat()

		drawModalRectWithCustomSizedTexture(xF, yF, 0f, 0f, widthF, heightF, widthF, heightF)

		GL11.glDepthMask(true)
		GL11.glDisable(GL11.GL_BLEND)
		GL11.glEnable(GL11.GL_DEPTH_TEST)
	}

	/**
	 * Draws a textured rectangle at z = 0. Args: x, y, u, v, width, height, textureWidth, textureHeight
	 */
	@JvmStatic
	fun drawModalRectWithCustomSizedTexture(x: Float, y: Float, u: Float, v: Float, width: Float, height: Float, textureWidth: Float, textureHeight: Float)
	{
		val provider = classProvider

		val tessellator = provider.tessellatorInstance
		val worldrenderer = tessellator.worldRenderer

		worldrenderer.begin(7, provider.getVertexFormatEnum(WDefaultVertexFormats.POSITION_TEX))

		val xD = x.toDouble()
		val yD = y.toDouble()
		val uD = u.toDouble()
		val vD = v.toDouble()

		val widthPercentage = 1.0f / textureWidth
		val heightPercentage = 1.0f / textureHeight

		worldrenderer.pos(xD, yD + height, 0.0).tex(uD * widthPercentage, (vD + height) * heightPercentage).endVertex()
		worldrenderer.pos(xD + width, yD + height, 0.0).tex((uD + width) * widthPercentage, (vD + height) * heightPercentage).endVertex()
		worldrenderer.pos(xD + width, yD, 0.0).tex((uD + width) * widthPercentage, vD * heightPercentage).endVertex()
		worldrenderer.pos(xD, yD, 0.0).tex(uD * widthPercentage, vD * heightPercentage).endVertex()
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
	fun draw2D(entity: IEntity, posX: Double, posY: Double, posZ: Double, color: Int, backgroundColor: Int)
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
	fun draw2D(blockPos: WBlockPos, color: Int, backgroundColor: Int)
	{
		val renderManager = mc.renderManager
		val posX = blockPos.x + 0.5 - renderManager.renderPosX
		val posY = blockPos.y - renderManager.renderPosY
		val posZ = blockPos.z + 0.5 - renderManager.renderPosZ

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
		val scaledResolution = classProvider.createScaledResolution(mc)
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
	fun drawScaledCustomSizeModalRect(x: Int, y: Int, u: Float, v: Float, uWidth: Int, vHeight: Int, width: Int, height: Int, tileWidth: Float, tileHeight: Float)
	{
		val provider = classProvider

		val tessellator = provider.tessellatorInstance
		val worldrenderer = tessellator.worldRenderer

		worldrenderer.begin(7, provider.getVertexFormatEnum(WDefaultVertexFormats.POSITION_TEX))

		val xD = x.toDouble()
		val yD = y.toDouble()
		val uD = u.toDouble()
		val vD = v.toDouble()

		val widthPercentage = 1.0f / tileWidth
		val heightPercentage = 1.0f / tileHeight

		worldrenderer.pos(xD, yD + height, 0.0).tex(uD * widthPercentage, (vD + vHeight) * heightPercentage).endVertex()
		worldrenderer.pos(xD + width, yD + height, 0.0).tex((uD + uWidth) * widthPercentage, (vD + vHeight) * heightPercentage).endVertex()
		worldrenderer.pos(xD + width, yD, 0.0).tex((uD + uWidth) * widthPercentage, vD * heightPercentage).endVertex()
		worldrenderer.pos(xD, yD, 0.0).tex(uD * widthPercentage, vD * heightPercentage).endVertex()
		tessellator.draw()
	}

	@JvmStatic
	fun drawFoVCircle(fov: Float)
	{
		if (mc.gameSettings.thirdPersonView > 0) return

		val scaledResolution = classProvider.createScaledResolution(mc)

		drawCircle((scaledResolution.scaledWidth shr 1).toFloat(), (scaledResolution.scaledHeight shr 1).toFloat(), fov * 6.0f / (mc.gameSettings.fovSettings / 70.0f), 0, 360)
	}

	@JvmStatic
	fun drawHorizontalLine(startX: Int, endX: Int, y: Int, color: Int)
	{
		var _startX = startX
		var _endX = endX

		if (_endX < _startX)
		{
			val i = _startX
			_startX = _endX
			_endX = i
		}

		Gui.drawRect(_startX, y, _endX + 1, y + 1, color)
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
