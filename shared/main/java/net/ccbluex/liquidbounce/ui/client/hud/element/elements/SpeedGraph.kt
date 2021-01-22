/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "SpeedGraph")
class SpeedGraph(
	x: Double = 75.0, y: Double = 110.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)
) : Element(x, y, scale, side)
{
	private val widthValue = IntegerValue("Width", 150, 100, 300)
	private val heightValue = IntegerValue("Height", 50, 30, 150)

	private val speedyMultiplier = FloatValue("Speed-yMultiplier", 7F, 1F, 20F)
	private val speedThicknessValue = FloatValue("Speed-Thickness", 2F, 1F, 3F)
	private val speedColorRedValue = IntegerValue("Speed-R", 0, 0, 255)
	private val speedColorGreenValue = IntegerValue("Speed-G", 111, 0, 255)
	private val speedColorBlueValue = IntegerValue("Speed-B", 255, 0, 255)

	private val yspeedYMultiplier = FloatValue("YSpeed-yMultiplier", 7F, 1F, 20F)
	private val yspeedThicknessValue = FloatValue("YSpeed-Thickness", 2F, 1F, 3F)
	private val yspeedColorRedValue = IntegerValue("YSpeed-R", 111, 0, 255)
	private val yspeedColorGreenValue = IntegerValue("YSpeed-G", 0, 0, 255)
	private val yspeedColorBlueValue = IntegerValue("YSpeed-B", 255, 0, 255)

	private val speedList = ArrayList<Double>()
	private val yspeedList = ArrayList<Double>()
	private var lastTick = -1

	override fun drawElement(): Border?
	{
		val thePlayer = mc.thePlayer ?: return null

		val width = widthValue.get()
		val height = heightValue.get()

		if (lastTick != thePlayer.ticksExisted)
		{

			// Update speed

			lastTick = thePlayer.ticksExisted
			val x = thePlayer.posX
			val prevX = thePlayer.prevPosX
			val y = thePlayer.posY
			val prevY = thePlayer.prevPosY
			val z = thePlayer.posZ
			val prevZ = thePlayer.prevPosZ

			var speed = sqrt((z - prevZ) * (z - prevZ) + (x - prevX) * (x - prevX))
			if (speed < 0) speed = -speed

			val yspeed = abs(y - prevY)

			speedList.add(speed)
			yspeedList.add(yspeed)
			while (speedList.size > width) speedList.removeAt(0)
			while (yspeedList.size > width) yspeedList.removeAt(0)
		}

		val speedYMul = speedyMultiplier.get()
		val yspeedYMul = yspeedYMultiplier.get()

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glEnable(GL11.GL_LINE_SMOOTH)
		GL11.glLineWidth(speedThicknessValue.get())
		GL11.glDisable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(false)

		GL11.glBegin(GL11.GL_LINES)

		run {
			val speedListSize = speedList.size

			val speedListStart = (if (speedListSize > width) speedListSize - width else 0)
			for (i in speedListStart until speedListSize - 1)
			{
				val speedY = speedList[i] * 10 * speedYMul
				val speedNextY = speedList[i + 1] * 10 * speedYMul

				RenderUtils.glColor(Color(speedColorRedValue.get(), speedColorGreenValue.get(), speedColorBlueValue.get(), 255))
				GL11.glVertex2d(i.toDouble() - speedListStart, height + 1 - speedY.coerceAtMost(height.toDouble()))
				GL11.glVertex2d(i + 1.0 - speedListStart, height + 1 - speedNextY.coerceAtMost(height.toDouble()))
			}
		}

		GL11.glEnd()
		GL11.glLineWidth(yspeedThicknessValue.get())
		GL11.glBegin(GL11.GL_LINES)

		run {
			val yspeedListSize = yspeedList.size

			val yspeedListStart = (if (yspeedListSize > width) yspeedListSize - width else 0)
			for (i in yspeedListStart until yspeedListSize - 1)
			{
				val yspeedY = yspeedList[i] * 10 * yspeedYMul
				val yspeedNextY = yspeedList[i + 1] * 10 * yspeedYMul

				RenderUtils.glColor(Color(yspeedColorRedValue.get(), yspeedColorGreenValue.get(), yspeedColorBlueValue.get(), 255))
				GL11.glVertex2d(i.toDouble() - yspeedListStart, height + 1 - yspeedY.coerceAtMost(height.toDouble()))
				GL11.glVertex2d(i + 1.0 - yspeedListStart, height + 1 - yspeedNextY.coerceAtMost(height.toDouble()))
			}
		}

		GL11.glEnd()

		GL11.glEnable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_LINE_SMOOTH)
		GL11.glEnable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(true)
		GL11.glDisable(GL11.GL_BLEND)
		GlStateManager.resetColor()

		return Border(0F, 0F, width.toFloat(), height.toFloat() + 2)
	}
}
