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
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.PacketCounter
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_1
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * CustomHUD network-activity graph element
 *
 * Allows to draw custom network-activity graph
 */

@ElementInfo(name = "NetGraph")
class NetGraph(x: Double = 75.0, y: Double = 110.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{
	private val widthValue = IntegerValue("Width", 150, 100, 300)
	private val heightValue = IntegerValue("Height", 50, 30, 300)

	private val pingUpdatePeriodValue = IntegerValue("PingUpdatePeriod", 500, 50, 2000)

	private val pingyMultiplier = FloatValue("Ping-yMultiplier", 10F, 0.001F, 50F)
	private val pingThicknessValue = FloatValue("Ping-Thickness", 2F, 1F, 3F)
	private val pingColorRedValue = IntegerValue("Ping-R", 0, 0, 255)
	private val pingColorGreenValue = IntegerValue("Ping-G", 111, 0, 255)
	private val pingColorBlueValue = IntegerValue("Ping-B", 255, 0, 255)

	private val packetUpdatePeriodValue = IntegerValue("PacketsUpdatePeriod", 200, 50, 1000)
	private val packetCounterBufferValue = IntegerValue("PacketsCounterBuffer", 200, 50, 1000)

	private val incomingPacketsYMultiplier = FloatValue("IncomingPackets-MinYMultiplier", 1F, 0.001F, 50F)
	private val incomingPacketsThicknessValue = FloatValue("IncomingPackets-Thickness", 2F, 1F, 3F)
	private val incomingPacketsColorRedValue = IntegerValue("IncomingPackets-R", 255, 0, 255)
	private val incomingPacketsColorGreenValue = IntegerValue("IncomingPackets-G", 111, 0, 255)
	private val incomingPacketsColorBlueValue = IntegerValue("IncomingPackets-B", 0, 0, 255)
	private val incomingPacketsAverageValue = BoolValue("IncomingPackets-Average", true)
	private val incomingPacketsAverageThicknessValue = FloatValue("IncomingPackets-Average-Thickness", 3F, 1F, 3F)
	private val incomingPacketsPeakValue = BoolValue("IncomingPackets-Peak", true)

	private val outgoingPacketsYMultiplier = FloatValue("OutgoingPackets-MinYMultiplier", 1F, 0.001F, 50F)
	private val outgoingPacketsThicknessValue = FloatValue("OutgoingPackets-Thickness", 2F, 1F, 3F)
	private val outgoingPacketsColorRedValue = IntegerValue("OutgoingPackets-R", 255, 0, 255)
	private val outgoingPacketsColorGreenValue = IntegerValue("OutgoingPackets-G", 0, 0, 255)
	private val outgoingPacketsColorBlueValue = IntegerValue("OutgoingPackets-B", 111, 0, 255)
	private val outgoingPacketsAverageValue = BoolValue("OutgoingPackets-Average", true)
	private val outgoingPacketsAverageThicknessValue = FloatValue("OutgoingPackets-Average-Thickness", 3F, 1F, 3F)
	private val outgoingPacketsPeakValue = BoolValue("OutgoingPackets-Peak", true)

	private val textFont = FontValue("TextFont", Fonts.minecraftFont)

	private val pingUpdateTimer = MSTimer()
	private val packetUpdateTimer = MSTimer()

	private val pingList = ArrayList<Int>()

	private val incomingPacketsList = ArrayList<Int>()
	private var incomingPacketsAverage = 0.0
	private var incomingPacketsPeak = 0
	private var incomingPacketsPeakIndex = -1

	private val outgoingPacketsList = ArrayList<Int>()
	private var outgoingPacketsAverage = 0.0
	private var outgoingPacketsPeak = 0
	private var outgoingPacketsPeakIndex = -1

	private var lastTick = -1

	private var incomingAverageString = ""
	private var incomingPeakString = ""
	private var incomingPeakStringWidth = 0F

	private var outgoingAverageString = ""
	private var outgoingAverageStringWidth = 0F

	private var outgoingPeakString = ""
	private var outgoingPeakStringWidth = 0F

	override fun drawElement(): Border?
	{
		val thePlayer = mc.thePlayer ?: return null

		val width = widthValue.get()
		val height = heightValue.get().toFloat()
		val pingUpdatePeriod = pingUpdatePeriodValue.get().toLong()
		val packetUpdatePeriod = packetUpdatePeriodValue.get().toLong()
		val packetCounterBuffer = packetCounterBufferValue.get().toLong()

		val currentTick = thePlayer.ticksExisted
		val tickChanged = currentTick != lastTick

		if (if (pingUpdatePeriod == 50L) tickChanged else pingUpdateTimer.hasTimePassed(pingUpdatePeriod))
		{
			pingList.add(thePlayer.getPing())
			while (pingList.size > width) pingList.removeAt(0)

			pingUpdateTimer.reset()
		}

		val incomingPacketsAverageEnabled = incomingPacketsAverageValue.get()
		val outgoingPacketsAverageEnabled = outgoingPacketsAverageValue.get()

		val incomingPacketsPeakEnabled = incomingPacketsPeakValue.get()
		val outgoingPacketsPeakEnabled = outgoingPacketsPeakValue.get()

		val stringFont = textFont.get()
		val stringHeightHalf = stringFont.fontHeight * 0.5F

		if (if (packetUpdatePeriod == 50L) tickChanged else packetUpdateTimer.hasTimePassed(packetUpdatePeriod))
		{
			incomingPacketsList.add(PacketCounter.getPacketCount(PacketCounter.PacketType.INBOUND, packetCounterBuffer))
			while (incomingPacketsList.size > width) incomingPacketsList.removeAt(0)

			var incomingSum = 0
			var incomingPeak = 0
			var incomingPeakIndex = -1
			incomingPacketsList.forEachIndexed { index, i ->
				incomingSum += i
				if (i > incomingPeak)
				{
					incomingPeak = i
					incomingPeakIndex = index
				}
			}

			if (incomingPacketsAverageEnabled)
			{
				incomingPacketsAverage = incomingSum.toDouble() / width.toDouble()

				incomingAverageString = DECIMALFORMAT_1.format(incomingPacketsAverage)
			}

			incomingPacketsPeak = incomingPeak

			if (incomingPacketsPeakEnabled)
			{
				incomingPacketsPeakIndex = incomingPeakIndex

				incomingPeakString = "$incomingPeak"
				incomingPeakStringWidth = stringFont.getStringWidth(incomingPeakString) * 0.5F
			}

			outgoingPacketsList.add(PacketCounter.getPacketCount(PacketCounter.PacketType.OUTBOUND, packetCounterBuffer))
			while (outgoingPacketsList.size > width) outgoingPacketsList.removeAt(0)

			var outgoingSum = 0

			var outgoingPeak = 0
			var outgoingPeakIndex = -1
			outgoingPacketsList.forEachIndexed { index, i ->
				outgoingSum += i
				if (i > outgoingPeak)
				{
					outgoingPeak = i
					outgoingPeakIndex = index
				}
			}

			if (outgoingPacketsAverageEnabled)
			{
				outgoingPacketsAverage = outgoingSum.toDouble() / width.toDouble()

				outgoingAverageString = DECIMALFORMAT_1.format(outgoingPacketsAverage)
				outgoingAverageStringWidth = stringFont.getStringWidth(outgoingAverageString).toFloat()
			}

			outgoingPacketsPeak = outgoingPeak

			if (outgoingPacketsPeakEnabled)
			{
				outgoingPacketsPeakIndex = outgoingPeakIndex

				outgoingPeakString = "$outgoingPeak"
				outgoingPeakStringWidth = stringFont.getStringWidth(outgoingPeakString) * 0.5F
			}

			packetUpdateTimer.reset()
		}

		if (tickChanged) lastTick = currentTick

		val incomingPacketsColor = ColorUtils.createRGB(incomingPacketsColorRedValue.get(), incomingPacketsColorGreenValue.get(), incomingPacketsColorBlueValue.get(), 255)
		val outgoingPacketsColor = ColorUtils.createRGB(outgoingPacketsColorRedValue.get(), outgoingPacketsColorGreenValue.get(), outgoingPacketsColorBlueValue.get(), 255)

		val pingYMul = pingyMultiplier.get() * 0.1f
		val incomingPacketsMinYMul = incomingPacketsYMultiplier.get() * 0.1f
		val outgoingPacketsMinYMul = outgoingPacketsYMultiplier.get() * 0.1f

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glEnable(GL11.GL_LINE_SMOOTH)
		GL11.glLineWidth(pingThicknessValue.get())
		GL11.glDisable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(false)

		GL11.glBegin(GL11.GL_LINES)

		// Draw Ping
		run {
			val pingListSize = pingList.size

			val pingListStart = (if (pingListSize > width) pingListSize - width else 0)
			for (i in pingListStart until pingListSize - 1)
			{
				val pingY = pingList[i] * pingYMul
				val pingNextY = pingList[i + 1] * pingYMul

				RenderUtils.glColor(Color(pingColorRedValue.get(), pingColorGreenValue.get(), pingColorBlueValue.get(), 255))
				GL11.glVertex2f(i.toFloat() - pingListStart, height + 1 - pingY.coerceAtMost(height))
				GL11.glVertex2f(i + 1.0F - pingListStart, height + 1 - pingNextY.coerceAtMost(height))
			}
		}

		GL11.glEnd()
		GL11.glLineWidth(incomingPacketsThicknessValue.get())
		GL11.glBegin(GL11.GL_LINES)

		// Draw Inbound Packets
		run {
			val incomingPacketsListSize = incomingPacketsList.size

			val incomingPacketsListStart = if (incomingPacketsListSize > width) incomingPacketsListSize - width else 0

			val ymul = (height / incomingPacketsPeak.toFloat()).coerceAtLeast(incomingPacketsMinYMul)

			for (i in incomingPacketsListStart until incomingPacketsListSize - 1)
			{
				val incomingPacketsY = incomingPacketsList[i] * ymul
				val incomingPacketsNextY = incomingPacketsList[i + 1] * ymul

				RenderUtils.glColor(incomingPacketsColor)
				GL11.glVertex2f(i.toFloat() - incomingPacketsListStart, height + 1 - incomingPacketsY)
				GL11.glVertex2f(i + 1.0F - incomingPacketsListStart, height + 1 - incomingPacketsNextY)
			}
		}

		GL11.glEnd()

		if (incomingPacketsAverageEnabled)
		{
			GL11.glLineWidth(incomingPacketsAverageThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			// Draw Inbound Packets Average
			run {
				val ypos = height + 1.0 - (height / incomingPacketsPeak.toFloat()).coerceAtLeast(incomingPacketsMinYMul) * incomingPacketsAverage

				RenderUtils.glColor(incomingPacketsColor)
				GL11.glVertex2d(stringFont.getStringWidth(incomingAverageString) + 2.0, ypos)
				GL11.glVertex2d(width.toDouble(), ypos)
			}

			GL11.glEnd()
		}

		GL11.glLineWidth(outgoingPacketsThicknessValue.get())
		GL11.glBegin(GL11.GL_LINES)

		// Draw Outbound Packets
		run {
			val outgoingPacketsListSize = outgoingPacketsList.size

			val outgoingPacketsListStart = (if (outgoingPacketsListSize > width) outgoingPacketsListSize - width else 0)

			val ymul = (height / outgoingPacketsPeak.toFloat()).coerceAtLeast(outgoingPacketsMinYMul)

			for (i in outgoingPacketsListStart until outgoingPacketsListSize - 1)
			{
				val outgoingPacketsY = outgoingPacketsList[i] * ymul
				val outgoingPacketsNextY = outgoingPacketsList[i + 1] * ymul

				RenderUtils.glColor(outgoingPacketsColor)
				GL11.glVertex2f((i - outgoingPacketsListStart).toFloat(), height + 1 - outgoingPacketsY)
				GL11.glVertex2f(i + 1.0F - outgoingPacketsListStart, height + 1 - outgoingPacketsNextY)
			}
		}

		GL11.glEnd()

		if (outgoingPacketsAverageEnabled)
		{
			GL11.glLineWidth(outgoingPacketsAverageThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			// Draw Outbound Packets Average
			run {
				val ypos = height + 1 - (height / outgoingPacketsPeak.toFloat()).coerceAtLeast(outgoingPacketsMinYMul) * outgoingPacketsAverage

				RenderUtils.glColor(outgoingPacketsColor)
				GL11.glVertex2d(0.0, ypos)
				GL11.glVertex2d(width.toDouble() - outgoingAverageStringWidth - 2.0, ypos)
			}

			GL11.glEnd()
		}

		GL11.glEnable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_LINE_SMOOTH)
		GL11.glEnable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(true)
		GL11.glDisable(GL11.GL_BLEND)
		classProvider.glStateManager.resetColor()

		// Draw Inbound Packets Average
		if (incomingPacketsAverageEnabled) stringFont.drawString(incomingAverageString, 0F, height + 1 - (height / incomingPacketsPeak.toFloat()).coerceAtLeast(incomingPacketsMinYMul) * incomingPacketsAverage.toFloat() - stringHeightHalf, incomingPacketsColor, shadow = true)

		// Draw Outbound Packets Average
		if (outgoingPacketsAverageEnabled) stringFont.drawString(outgoingAverageString, width - outgoingAverageStringWidth, height + 1 - (height / outgoingPacketsPeak.toFloat()).coerceAtLeast(outgoingPacketsMinYMul) * outgoingPacketsAverage.toFloat() - stringHeightHalf, outgoingPacketsColor, shadow = true)

		if (incomingPacketsPeakEnabled)
		{
			val incomingPacketsListSize = incomingPacketsList.size

			val incomingPacketsListStart = if (incomingPacketsListSize > width) incomingPacketsListSize - width else 0

			stringFont.drawString(incomingPeakString, incomingPacketsPeakIndex.toFloat() - incomingPeakStringWidth - incomingPacketsListStart, 0F, incomingPacketsColor, shadow = true)
		}

		if (outgoingPacketsPeakEnabled)
		{
			val outgoingPacketsListSize = outgoingPacketsList.size

			val outgoingPacketsListStart = if (outgoingPacketsListSize > width) outgoingPacketsListSize - width else 0

			stringFont.drawString(outgoingPeakString, outgoingPacketsPeakIndex.toFloat() - outgoingPeakStringWidth - outgoingPacketsListStart, 0F, outgoingPacketsColor, shadow = true)
		}

		return Border(0F, 0F, width.toFloat(), height + 2)
	}
}
