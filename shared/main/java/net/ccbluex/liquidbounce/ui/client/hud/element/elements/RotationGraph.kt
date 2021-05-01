/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs
import kotlin.math.hypot

/**
 * CustomHUD rotation-graph element
 *
 * Allows to draw custom rotation-graph
 */
@ElementInfo(name = "RotationGraph")
class RotationGraph(x: Double = 75.0, y: Double = 110.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{
	private val widthValue = IntegerValue("Width", 150, 100, 300)
	private val heightValue = IntegerValue("Height", 50, 30, 300)

	private val rotationyMultiplier = FloatValue("Rotation-yMultiplier", 2F, 0.5F, 5F)
	private val rotationThicknessValue = FloatValue("Rotation-Thickness", 2F, 1F, 3F)
	private val rotationColorRedValue = IntegerValue("Rotation-R", 0, 0, 255)
	private val rotationColorGreenValue = IntegerValue("Rotation-G", 255, 0, 255)
	private val rotationColorBlueValue = IntegerValue("Rotation-B", 72, 0, 255)

	private val yawMovementEnabled = BoolValue("YawMovement", false)
	private val yawMovementYMultiplier = FloatValue("YawMovement-yMultiplier", 2F, 0.5F, 5F)
	private val yawMovementThicknessValue = FloatValue("YawMovement-Thickness", 2F, 1F, 3F)
	private val yawMovementColorRedValue = IntegerValue("YawMovement-R", 0, 0, 255)
	private val yawMovementColorGreenValue = IntegerValue("YawMovement-G", 0, 0, 255)
	private val yawMovementColorBlueValue = IntegerValue("YawMovement-B", 255, 0, 255)

	private val pitchMovementEnabled = BoolValue("PitchMovement", false)
	private val pitchMovementYMultiplier = FloatValue("PitchMovement-yMultiplier", 2F, 0.5F, 5F)
	private val pitchMovementThicknessValue = FloatValue("PitchMovement-Thickness", 2F, 1F, 3F)
	private val pitchMovementColorRedValue = IntegerValue("PitchMovement-R", 111, 0, 255)
	private val pitchMovementColorGreenValue = IntegerValue("PitchMovement-G", 0, 0, 255)
	private val pitchMovementColorBlueValue = IntegerValue("PitchMovement-B", 255, 0, 255)

	private val rotationConsistencyEnabled = BoolValue("RotationConsistency", false)
	private val rotationConsistencyyMultiplier = FloatValue("RotationConsistency-yMultiplier", 2F, 0.5F, 5F)
	private val rotationConsistencyThicknessValue = FloatValue("RotationConsistency-Thickness", 2F, 1F, 3F)
	private val rotationConsistencyColorRedValue = IntegerValue("RotationConsistency-R", 0, 0, 255)
	private val rotationConsistencyColorGreenValue = IntegerValue("RotationConsistency-G", 255, 0, 255)
	private val rotationConsistencyColorBlueValue = IntegerValue("RotationConsistency-B", 72, 0, 255)

	private val yawConsistencyEnabled = BoolValue("YawConsistency", false)
	private val yawConsistencyyMultiplier = FloatValue("YawConsistency-yMultiplier", 2F, 0.5F, 5F)
	private val yawConsistencyThicknessValue = FloatValue("YawConsistency-Thickness", 2F, 1F, 3F)
	private val yawConsistencyColorRedValue = IntegerValue("YawConsistency-R", 0, 0, 255)
	private val yawConsistencyColorGreenValue = IntegerValue("YawConsistency-G", 255, 0, 255)
	private val yawConsistencyColorBlueValue = IntegerValue("YawConsistency-B", 180, 0, 255)

	private val pitchConsistencyEnabled = BoolValue("PitchConsistency", false)
	private val pitchConsistencyYMultiplier = FloatValue("PitchConsistency-yMultiplier", 2F, 0.5F, 5F)
	private val pitchConsistencyThicknessValue = FloatValue("PitchConsistency-Thickness", 2F, 1F, 3F)
	private val pitchConsistencyColorRedValue = IntegerValue("PitchConsistency-R", 0, 0, 255)
	private val pitchConsistencyColorGreenValue = IntegerValue("PitchConsistency-G", 180, 0, 255)
	private val pitchConsistencyColorBlueValue = IntegerValue("PitchConsistency-B", 255, 0, 255)

	private val rotationList = ArrayList<Float>()
	private val yawMovementList = ArrayList<Float>()
	private val pitchMovementList = ArrayList<Float>()
	private val rotationConsistencyList = ArrayList<Float>()
	private val yawConsistencyList = ArrayList<Float>()
	private val pitchConsistencyList = ArrayList<Float>()

	private var lastTick = -1

	private var lastRotation = 0.0F
	private var lastYawMovement = 0.0F
	private var lastPitchMovement = 0.0F

	override fun drawElement(): Border?
	{
		val thePlayer = mc.thePlayer ?: return null

		val width = widthValue.get()
		val height = heightValue.get().toFloat()

		val yawMovementEnabled = yawMovementEnabled.get()
		val pitchMovementEnabled = pitchMovementEnabled.get()

		val rotationConsistencyEnabled = rotationConsistencyEnabled.get()

		val yawConsistencyEnabled = yawConsistencyEnabled.get()
		val pitchConsistencyEnabled = pitchConsistencyEnabled.get()

		if (lastTick != thePlayer.ticksExisted)
		{
			// Update rotation

			lastTick = thePlayer.ticksExisted

			val serverRotation = RotationUtils.serverRotation
			val prevServerRotation = RotationUtils.lastServerRotation

			val yawMovement = abs(serverRotation.yaw - prevServerRotation.yaw)
			val pitchMovement = abs(serverRotation.pitch - prevServerRotation.pitch)

			val rotation = hypot(yawMovement, pitchMovement)

			val rotationConsistency = abs(rotation - lastRotation)

			val yawConsistency = abs(yawMovement - lastYawMovement)
			val pitchConsistency = abs(pitchMovement - lastPitchMovement)

			lastRotation = rotation
			lastYawMovement = yawMovement
			lastPitchMovement = pitchMovement

			rotationList.add(rotation)

			while (rotationList.size > width) rotationList.removeAt(0)

			if (yawMovementEnabled)
			{
				yawMovementList.add(yawMovement)

				while (yawMovementList.size > width) yawMovementList.removeAt(0)
			}

			if (pitchMovementEnabled)
			{
				pitchMovementList.add(pitchMovement)

				while (pitchMovementList.size > width) pitchMovementList.removeAt(0)
			}

			if (rotationConsistencyEnabled)
			{
				rotationConsistencyList.add(rotationConsistency)

				while (rotationConsistencyList.size > width) rotationConsistencyList.removeAt(0)
			}

			if (yawConsistencyEnabled)
			{
				yawConsistencyList.add(yawConsistency)

				while (yawConsistencyList.size > width) yawConsistencyList.removeAt(0)
			}

			if (pitchConsistencyEnabled)
			{
				pitchConsistencyList.add(pitchConsistency)

				while (pitchMovementList.size > width) pitchMovementList.removeAt(0)
			}
		}

		val rotationYMul = rotationyMultiplier.get()
		val yawMovementYMul = yawMovementYMultiplier.get()
		val pitchMovementYMul = pitchMovementYMultiplier.get()
		val rotationConsistencyYMul = rotationConsistencyyMultiplier.get()
		val yawConsistencyYMul = yawConsistencyyMultiplier.get()
		val pitchConsistencyYMul = pitchConsistencyYMultiplier.get()

		val rotationColor = Color(rotationColorRedValue.get(), rotationColorGreenValue.get(), rotationColorBlueValue.get())
		val yawMovementColor = Color(yawMovementColorRedValue.get(), yawMovementColorGreenValue.get(), yawMovementColorBlueValue.get())
		val pitchMovementColor = Color(pitchMovementColorRedValue.get(), pitchMovementColorGreenValue.get(), pitchMovementColorBlueValue.get())
		val rotationConsistencyColor = Color(rotationConsistencyColorRedValue.get(), rotationConsistencyColorGreenValue.get(), rotationConsistencyColorBlueValue.get())
		val yawConsistencyColor = Color(yawConsistencyColorRedValue.get(), yawConsistencyColorGreenValue.get(), yawConsistencyColorBlueValue.get())
		val pitchConsistencyColor = Color(pitchConsistencyColorRedValue.get(), pitchConsistencyColorGreenValue.get(), pitchConsistencyColorBlueValue.get())

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glEnable(GL11.GL_LINE_SMOOTH)
		GL11.glLineWidth(rotationThicknessValue.get())
		GL11.glDisable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(false)

		// Draw Rotation

		GL11.glBegin(GL11.GL_LINES)

		run {
			val rotationListSize = rotationList.size

			val rotationListStart = (if (rotationListSize > width) rotationListSize - width else 0)
			for (i in rotationListStart until rotationListSize - 1)
			{
				val rotationY = rotationList[i] * 10 * rotationYMul
				val rotationNextY = rotationList[i + 1] * 10 * rotationYMul

				RenderUtils.glColor(rotationColor)
				GL11.glVertex2f(i.toFloat() - rotationListStart, height + 1 - rotationY.coerceAtMost(height))
				GL11.glVertex2f(i + 1.0F - rotationListStart, height + 1 - rotationNextY.coerceAtMost(height))
			}
		}

		GL11.glEnd()

		if (yawMovementEnabled)
		{
			// Draw Yaw Movements

			GL11.glLineWidth(yawMovementThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			run {
				val yawMovementListSize = yawMovementList.size

				val yawMovementListStart = (if (yawMovementListSize > width) yawMovementListSize - width else 0)
				for (i in yawMovementListStart until yawMovementListSize - 1)
				{
					val yawMovementY = yawMovementList[i] * 10 * yawMovementYMul
					val yawMovementNextY = yawMovementList[i + 1] * 10 * yawMovementYMul

					RenderUtils.glColor(yawMovementColor)
					GL11.glVertex2f(i.toFloat() - yawMovementListStart, height + 1 - yawMovementY.coerceAtMost(height))
					GL11.glVertex2f(i + 1.0F - yawMovementListStart, height + 1 - yawMovementNextY.coerceAtMost(height))
				}
			}

			GL11.glEnd()
		}

		if (pitchMovementEnabled)
		{
			// Draw Pitch Movements

			GL11.glLineWidth(pitchMovementThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			run {
				val pitchMovementListSize = pitchMovementList.size

				val pitchMovementListStart = (if (pitchMovementListSize > width) pitchMovementListSize - width else 0)
				for (i in pitchMovementListStart until pitchMovementListSize - 1)
				{
					val pitchMovementY = pitchMovementList[i] * 10 * pitchMovementYMul
					val pitchMovementNextY = pitchMovementList[i + 1] * 10 * pitchMovementYMul

					RenderUtils.glColor(pitchMovementColor)
					GL11.glVertex2f(i.toFloat() - pitchMovementListStart, height + 1 - pitchMovementY.coerceAtMost(height))
					GL11.glVertex2f(i + 1.0F - pitchMovementListStart, height + 1 - pitchMovementNextY.coerceAtMost(height))
				}
			}

			GL11.glEnd()
		}

		// Draw Rotation Consistency

		GL11.glLineWidth(rotationConsistencyThicknessValue.get())
		GL11.glBegin(GL11.GL_LINES)

		run {
			val rotationConsistencyListSize = rotationConsistencyList.size

			val rotationConsistencyListStart = (if (rotationConsistencyListSize > width) rotationConsistencyListSize - width else 0)
			for (i in rotationConsistencyListStart until rotationConsistencyListSize - 1)
			{
				val rotationConsistencyY = rotationConsistencyList[i] * 10 * rotationConsistencyYMul
				val rotationConsistencyNextY = rotationConsistencyList[i + 1] * 10 * rotationConsistencyYMul

				RenderUtils.glColor(rotationConsistencyColor)
				GL11.glVertex2f(i.toFloat() - rotationConsistencyListStart, height + 1 - rotationConsistencyY.coerceAtMost(height))
				GL11.glVertex2f(i + 1.0F - rotationConsistencyListStart, height + 1 - rotationConsistencyNextY.coerceAtMost(height))
			}
		}

		GL11.glEnd()

		// Draw Yaw Consistency
		if (yawConsistencyEnabled)
		{
			GL11.glLineWidth(yawConsistencyThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			run {
				val yawConsistencyListSize = yawConsistencyList.size

				val yawConsistencyListStart = (if (yawConsistencyListSize > width) yawConsistencyListSize - width else 0)
				for (i in yawConsistencyListStart until yawConsistencyListSize - 1)
				{
					val yawConsistencyY = yawConsistencyList[i] * 10 * yawConsistencyYMul
					val yawConsistencyNextY = yawConsistencyList[i + 1] * 10 * yawConsistencyYMul

					RenderUtils.glColor(yawConsistencyColor)
					GL11.glVertex2f(i.toFloat() - yawConsistencyListStart, height + 1 - yawConsistencyY.coerceAtMost(height))
					GL11.glVertex2f(i + 1.0F - yawConsistencyListStart, height + 1 - yawConsistencyNextY.coerceAtMost(height))
				}
			}

			GL11.glEnd()
		}

		// Draw Pitch Consistency
		if (pitchConsistencyEnabled)
		{
			GL11.glLineWidth(pitchConsistencyThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			run {
				val pitchConsistencyListSize = pitchConsistencyList.size

				val pitchConsistencyListStart = (if (pitchConsistencyListSize > width) pitchConsistencyListSize - width else 0)
				for (i in pitchConsistencyListStart until pitchConsistencyListSize - 1)
				{
					val pitchConsistencyY = pitchConsistencyList[i] * 10 * pitchConsistencyYMul
					val pitchConsistencyNextY = pitchConsistencyList[i + 1] * 10 * pitchConsistencyYMul

					RenderUtils.glColor(pitchConsistencyColor)
					GL11.glVertex2f(i.toFloat() - pitchConsistencyListStart, height + 1 - pitchConsistencyY.coerceAtMost(height))
					GL11.glVertex2f(i + 1.0F - pitchConsistencyListStart, height + 1 - pitchConsistencyNextY.coerceAtMost(height))
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
