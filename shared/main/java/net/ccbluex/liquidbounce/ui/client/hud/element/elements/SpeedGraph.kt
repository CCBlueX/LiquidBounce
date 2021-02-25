/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import kotlin.math.hypot

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "SpeedGraph")
class SpeedGraph(x: Double = 75.0, y: Double = 110.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{
	private val widthValue = IntegerValue("Width", 150, 100, 300)
	private val heightValue = IntegerValue("Height", 50, 30, 300)

	private val speedyMultiplier = FloatValue("Speed-yMultiplier", 7F, 1F, 20F)
	private val speedThicknessValue = FloatValue("Speed-Thickness", 2F, 1F, 3F)
	private val speedColorRedValue = IntegerValue("Speed-R", 0, 0, 255)
	private val speedColorGreenValue = IntegerValue("Speed-G", 255, 0, 255)
	private val speedColorBlueValue = IntegerValue("Speed-B", 72, 0, 255)

	private val yspeedYMultiplier = FloatValue("YSpeed-yMultiplier", 7F, 1F, 20F)
	private val yspeedYPos = FloatValue("YSpeed-yPos", 20F, 0F, 150F)
	private val yspeedThicknessValue = FloatValue("YSpeed-Thickness", 2F, 1F, 3F)
	private val yspeedColorRedValue = IntegerValue("YSpeed-R", 0, 0, 255)
	private val yspeedColorGreenValue = IntegerValue("YSpeed-G", 0, 0, 255)
	private val yspeedColorBlueValue = IntegerValue("YSpeed-B", 255, 0, 255)

	private val timerEnabled = BoolValue("Timer", true)
	private val timerYMultiplier = FloatValue("Timer-yMultiplier", 7F, 1F, 20F)
	private val timerThicknessValue = FloatValue("Timer-Thickness", 2F, 1F, 3F)
	private val timerColorRedValue = IntegerValue("Timer-R", 111, 0, 255)
	private val timerColorGreenValue = IntegerValue("Timer-G", 0, 0, 255)
	private val timerColorBlueValue = IntegerValue("Timer-B", 255, 0, 255)

	private val motionEnabled = BoolValue("Motion", true)
	private val motionyMultiplier = FloatValue("Motion-yMultiplier", 7F, 1F, 20F)
	private val motionThicknessValue = FloatValue("Motion-Thickness", 2F, 1F, 3F)
	private val motionColorRedValue = IntegerValue("Motion-R", 0, 0, 255)
	private val motionColorGreenValue = IntegerValue("Motion-G", 255, 0, 255)
	private val motionColorBlueValue = IntegerValue("Motion-B", 180, 0, 255)

	private val ymotionEnabled = BoolValue("YMotion", true)
	private val ymotionYMultiplier = FloatValue("YMotion-yMultiplier", 7F, 1F, 20F)
	private val ymotionYPos = FloatValue("YMotion-yPos", 20F, 0F, 150F)
	private val ymotionThicknessValue = FloatValue("YMotion-Thickness", 2F, 1F, 3F)
	private val ymotionColorRedValue = IntegerValue("YMotion-R", 0, 0, 255)
	private val ymotionColorGreenValue = IntegerValue("YMotion-G", 180, 0, 255)
	private val ymotionColorBlueValue = IntegerValue("YMotion-B", 255, 0, 255)

	private val speedList = ArrayList<Double>()
	private val yspeedList = ArrayList<Double>()
	private val timerList = ArrayList<Float>()
	private val motionList = ArrayList<Double>()
	private val ymotionList = ArrayList<Double>()
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

			val speed = hypot((x - prevX), (z - prevZ))
			val yspeed = y - prevY
			val timer = mc.timer.timerSpeed
			val motion = hypot(thePlayer.motionX, thePlayer.motionZ)
			val ymotion = thePlayer.motionY

			speedList.add(speed)
			yspeedList.add(yspeed)
			timerList.add(timer)
			motionList.add(motion)
			ymotionList.add(ymotion)
			while (speedList.size > width) speedList.removeAt(0)
			while (yspeedList.size > width) yspeedList.removeAt(0)
			while (timerList.size > width) timerList.removeAt(0)
			while (motionList.size > width) motionList.removeAt(0)
			while (ymotionList.size > width) ymotionList.removeAt(0)
		}

		val speedYMul = speedyMultiplier.get()
		val yspeedYMul = yspeedYMultiplier.get()
		val timerYMul = timerYMultiplier.get()
		val motionYMul = motionyMultiplier.get()
		val ymotionYMul = ymotionYMultiplier.get()

		val speedColor = Color(speedColorRedValue.get(), speedColorGreenValue.get(), speedColorBlueValue.get())
		val yspeedColor = Color(yspeedColorRedValue.get(), yspeedColorGreenValue.get(), yspeedColorBlueValue.get())
		val timerColor = Color(timerColorRedValue.get(), timerColorGreenValue.get(), timerColorBlueValue.get())
		val motionColor = Color(motionColorRedValue.get(), motionColorGreenValue.get(), motionColorBlueValue.get())
		val ymotionColor = Color(ymotionColorRedValue.get(), ymotionColorGreenValue.get(), ymotionColorBlueValue.get())

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glEnable(GL11.GL_LINE_SMOOTH)
		GL11.glLineWidth(speedThicknessValue.get())
		GL11.glDisable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(false)

		// Draw Speed
		GL11.glBegin(GL11.GL_LINES)

		run {
			val speedListSize = speedList.size

			val speedListStart = (if (speedListSize > width) speedListSize - width else 0)
			for (i in speedListStart until speedListSize - 1)
			{
				val speedY = speedList[i] * 10 * speedYMul
				val speedNextY = speedList[i + 1] * 10 * speedYMul

				RenderUtils.glColor(speedColor)
				GL11.glVertex2d(i.toDouble() - speedListStart, height + 1 - speedY.coerceAtMost(height.toDouble()))
				GL11.glVertex2d(i + 1.0 - speedListStart, height + 1 - speedNextY.coerceAtMost(height.toDouble()))
			}
		}

		GL11.glEnd()

		// Draw YSpeed
		GL11.glLineWidth(yspeedThicknessValue.get())
		GL11.glBegin(GL11.GL_LINES)

		run {
			val yspeedListSize = yspeedList.size
			val ypos = yspeedYPos.get().toDouble()

			val yspeedListStart = (if (yspeedListSize > width) yspeedListSize - width else 0)
			for (i in yspeedListStart until yspeedListSize - 1)
			{
				val yspeedY = yspeedList[i] * 10 * yspeedYMul
				val yspeedNextY = yspeedList[i + 1] * 10 * yspeedYMul

				RenderUtils.glColor(yspeedColor)
				GL11.glVertex2d(i.toDouble() - yspeedListStart, height + 1 - ypos - yspeedY.coerceAtLeast(-ypos).coerceAtMost(height.toDouble() - ypos))
				GL11.glVertex2d(i + 1.0 - yspeedListStart, height + 1 - ypos - yspeedNextY.coerceAtLeast(-ypos).coerceAtMost(height.toDouble() - ypos))
			}
		}

		GL11.glEnd()

		// Draw Timer
		if (timerEnabled.get())
		{
			GL11.glLineWidth(timerThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			run {
				val timerListSize = timerList.size

				val timerListStart = (if (timerListSize > width) timerListSize - width else 0)
				for (i in timerListStart until timerListSize - 1)
				{
					val timerY = timerList[i] * 10 * timerYMul
					val timerNextY = timerList[i + 1] * 10 * timerYMul

					RenderUtils.glColor(timerColor)
					GL11.glVertex2f(i.toFloat() - timerListStart, height + 1 - timerY.coerceAtMost(height.toFloat()))
					GL11.glVertex2f(i + 1.0F - timerListStart, height + 1 - timerNextY.coerceAtMost(height.toFloat()))
				}
			}

			GL11.glEnd()
		}

		// Draw Motion
		if (motionEnabled.get())
		{
			GL11.glLineWidth(motionThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			run {
				val motionListSize = motionList.size

				val motionListStart = (if (motionListSize > width) motionListSize - width else 0)
				for (i in motionListStart until motionListSize - 1)
				{
					val motionY = motionList[i] * 10 * motionYMul
					val motionNextY = motionList[i + 1] * 10 * motionYMul

					RenderUtils.glColor(motionColor)
					GL11.glVertex2d(i.toDouble() - motionListStart, height + 1 - motionY.coerceAtMost(height.toDouble()))
					GL11.glVertex2d(i + 1.0 - motionListStart, height + 1 - motionNextY.coerceAtMost(height.toDouble()))
				}
			}

			GL11.glEnd()
		}

		// Draw YMotion
		if (ymotionEnabled.get())
		{
			GL11.glLineWidth(ymotionThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			run {
				val ymotionListSize = ymotionList.size
				val ypos = ymotionYPos.get().toDouble()

				val ymotionListStart = (if (ymotionListSize > width) ymotionListSize - width else 0)
				for (i in ymotionListStart until ymotionListSize - 1)
				{
					val ymotionY = ymotionList[i] * 10 * ymotionYMul
					val ymotionNextY = ymotionList[i + 1] * 10 * ymotionYMul

					RenderUtils.glColor(ymotionColor)
					GL11.glVertex2d(i.toDouble() - ymotionListStart, height + 1 - ypos - ymotionY.coerceAtLeast(-ypos).coerceAtMost(height.toDouble() - ypos))
					GL11.glVertex2d(i + 1.0 - ymotionListStart, height + 1 - ypos - ymotionNextY.coerceAtLeast(-ypos).coerceAtMost(height.toDouble() - ypos))
				}
			}

			GL11.glEnd()
		}

		GL11.glEnable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_LINE_SMOOTH)
		GL11.glEnable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(true)
		GL11.glDisable(GL11.GL_BLEND)
		classProvider.glStateManager.resetColor()

		return Border(0F, 0F, width.toFloat(), height + 2.0f)
	}
}
