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
import net.ccbluex.liquidbounce.utils.extensions.ping
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_1
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.easeOutCubic
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11

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

	private val pingGroup = ValueGroup("Ping")
	private val pingUpdatePeriodValue = IntegerValue("UpdatePeriod", 500, 10, 2000, "PingUpdatePeriod")
	private val pingYMultiplier = FloatValue("Multiplier", 10F, 0.001F, 50F, "Ping-yMultiplier")
	private val pingThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "Ping-Thickness")
	private val pingColorValue = RGBColorValue("Color", 0, 111, 255, Triple("Ping-R", "Ping-G", "Ping-B"))

	private val packetsGroup = ValueGroup("Packets")
	private val packetsUpdatePeriodValue = IntegerValue("UpdatePeriod", 200, 10, 1000, "PacketsUpdatePeriod")
	private val packetCounterBufferValue = IntegerValue("CounterBuffer", 200, 50, 1000, "PacketsCounterBuffer")

	private val packetsIncomingGroup = ValueGroup("IncomingPackets")
	private val packetsIncomingPeakValue = BoolValue("Peak", true, "IncomingPackets-Peak")
	private val packetsIncomingScaleFadeSpeedValue = IntegerValue("ScaleFadeSpeed", 2, 1, 9)
	private val packetsIncomingYMultiplier = FloatValue("Multiplier", 1F, 0.001F, 50F, "IncomingPackets-MinYMultiplier")
	private val packetsIncomingThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "IncomingPackets-Thickness")
	private val packetsIncomingColorValue = RGBColorValue("Color", 255, 111, 0, Triple("IncomingPackets-R", "IncomingPackets-G", "IncomingPackets-B"))

	private val packetsIncomingAverageGroup = ValueGroup("Average")
	private val packetsIncomingAverageEnabledValue = BoolValue("Enabled", true, "IncomingPackets-Average")
	private val packetsIncomingAverageThicknessValue = FloatValue("Thickness", 3F, 1F, 3F, "IncomingPackets-Average-Thickness")

	private val packetsOutgoingGroup = ValueGroup("OutgoingPackets")
	private val packetsOutgoingPeakValue = BoolValue("Peak", true, "OutgoingPackets-Peak")
	private val packetsOutgoingScaleFadeSpeedValue = IntegerValue("ScaleFadeSpeed", 2, 1, 9)
	private val packetsOutgoingYMultiplier = FloatValue("YMultiplier", 1F, 0.001F, 50F, "OutgoingPackets-MinYMultiplier")
	private val packetsOutgoingThicknessValue = FloatValue("Thickness", 2F, 1F, 3F, "OutgoingPackets-Thickness")
	private val packetsOutgoingColorValue = RGBColorValue("Color", 255, 0, 111, Triple("OutgoingPackets-R", "OutgoingPackets-G", "OutgoingPackets-B"))

	private val packetsOutgoingAverageGroup = ValueGroup("Average")
	private val packetsOutgoingAverageEnabledValue = BoolValue("Enabled", true, "OutgoingPackets-Average")
	private val packetsOutgoingAverageThicknessValue = FloatValue("Thickness", 3F, 1F, 3F, "OutgoingPackets-Average-Thickness")

	private val textFont = FontValue("TextFont", Fonts.minecraftFont)

	private val pingUpdateTimer = MSTimer()
	private val packetUpdateTimer = MSTimer()

	private val pingList = ArrayList<Int>()

	private val incomingPacketsList = ArrayList<Int>()
	private var incomingPacketsAverage = 0.0f
	private var incomingPacketsPeak = 0
	private var incomingPacketsScale = 0f
	private var incomingPacketsPeakIndex = -1

	private val outgoingPacketsList = ArrayList<Int>()
	private var outgoingPacketsAverage = 0.0f
	private var outgoingPacketsPeak = 0
	private var outgoingPacketsScale = 0f
	private var outgoingPacketsPeakIndex = -1

	private var lastTick = -1

	private var incomingAverageString = ""
	private var incomingPeakString = ""
	private var incomingPeakStringWidth = 0F

	private var outgoingAverageString = ""
	private var outgoingAverageStringWidth = 0F

	private var outgoingPeakString = ""
	private var outgoingPeakStringWidth = 0F

	init
	{
		pingGroup.addAll(pingUpdatePeriodValue, pingYMultiplier, pingThicknessValue, pingColorValue)

		packetsIncomingAverageGroup.addAll(packetsIncomingAverageEnabledValue, packetsIncomingAverageThicknessValue)
		packetsIncomingGroup.addAll(packetsIncomingPeakValue, packetsIncomingScaleFadeSpeedValue, packetsIncomingYMultiplier, packetsIncomingThicknessValue, packetsIncomingColorValue, packetsIncomingAverageGroup)

		packetsOutgoingAverageGroup.addAll(packetsOutgoingAverageEnabledValue, packetsOutgoingAverageThicknessValue)
		packetsOutgoingGroup.addAll(packetsOutgoingPeakValue, packetsOutgoingScaleFadeSpeedValue, packetsOutgoingYMultiplier, packetsOutgoingThicknessValue, packetsOutgoingColorValue, packetsOutgoingAverageGroup)

		packetsGroup.addAll(packetsUpdatePeriodValue, packetCounterBufferValue, packetsIncomingGroup, packetsOutgoingGroup)
	}

	override fun drawElement(): Border?
	{
		val thePlayer = mc.thePlayer ?: return null

		val width = widthValue.get()
		val height = heightValue.get().toFloat()
		val pingUpdatePeriod = pingUpdatePeriodValue.get().toLong()
		val packetUpdatePeriod = packetsUpdatePeriodValue.get().toLong()
		val packetCounterBuffer = packetCounterBufferValue.get().toLong()

		val currentTick = thePlayer.ticksExisted
		val tickChanged = currentTick != lastTick

		if (if (pingUpdatePeriod == 50L) tickChanged else pingUpdateTimer.hasTimePassed(pingUpdatePeriod))
		{
			pingList.add(thePlayer.ping)
			while (pingList.size > width) pingList.removeAt(0)

			pingUpdateTimer.reset()
		}

		val incomingPacketsAverageEnabled = packetsIncomingAverageEnabledValue.get()
		val outgoingPacketsAverageEnabled = packetsOutgoingAverageEnabledValue.get()

		val incomingPacketsPeakEnabled = packetsIncomingPeakValue.get()
		val outgoingPacketsPeakEnabled = packetsOutgoingPeakValue.get()

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
				incomingPacketsAverage = incomingSum.toFloat() / width.toFloat()

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
				outgoingPacketsAverage = outgoingSum.toFloat() / width.toFloat()

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

		incomingPacketsScale = easeOutCubic(incomingPacketsScale, incomingPacketsPeak.toFloat(), packetsIncomingScaleFadeSpeedValue.get())
		outgoingPacketsScale = easeOutCubic(outgoingPacketsScale, outgoingPacketsPeak.toFloat(), packetsOutgoingScaleFadeSpeedValue.get())

		if (tickChanged) lastTick = currentTick

		val incomingPacketsColor = packetsIncomingColorValue.get()
		val outgoingPacketsColor = packetsOutgoingColorValue.get()

		val pingYMul = pingYMultiplier.get() * 0.1f
		val incomingPacketsMinYMul = packetsIncomingYMultiplier.get()
		val outgoingPacketsMinYMul = packetsOutgoingYMultiplier.get()

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

				RenderUtils.glColor(pingColorValue.get())
				GL11.glVertex2f(i.toFloat() - pingListStart, height + 1 - pingY.coerceAtMost(height))
				GL11.glVertex2f(i + 1.0F - pingListStart, height + 1 - pingNextY.coerceAtMost(height))
			}
		}

		GL11.glEnd()
		GL11.glLineWidth(packetsIncomingThicknessValue.get())
		GL11.glBegin(GL11.GL_LINES)

		// Draw Inbound Packets
		run {
			val incomingPacketsListSize = incomingPacketsList.size

			val incomingPacketsListStart = if (incomingPacketsListSize > width) incomingPacketsListSize - width else 0

			val ymul = (height / incomingPacketsScale).coerceAtMost(incomingPacketsMinYMul)

			for (i in incomingPacketsListStart until incomingPacketsListSize - 1)
			{
				val incomingPacketsY = incomingPacketsList[i] * ymul
				val incomingPacketsNextY = incomingPacketsList[i + 1] * ymul

				RenderUtils.glColor(incomingPacketsColor)
				GL11.glVertex2f(i.toFloat() - incomingPacketsListStart, height + 1 - incomingPacketsY.coerceAtMost(height))
				GL11.glVertex2f(i + 1.0F - incomingPacketsListStart, height + 1 - incomingPacketsNextY.coerceAtMost(height))
			}
		}

		GL11.glEnd()

		if (incomingPacketsAverageEnabled)
		{
			GL11.glLineWidth(packetsIncomingAverageThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			// Draw Inbound Packets Average
			run {
				val ypos = height + 1.0 - (height / incomingPacketsScale).coerceAtMost(incomingPacketsMinYMul) * incomingPacketsAverage

				RenderUtils.glColor(incomingPacketsColor)
				GL11.glVertex2d(stringFont.getStringWidth(incomingAverageString) + 2.0, ypos)
				GL11.glVertex2d(width.toDouble(), ypos)
			}

			GL11.glEnd()
		}

		GL11.glLineWidth(packetsOutgoingThicknessValue.get())
		GL11.glBegin(GL11.GL_LINES)

		// Draw Outbound Packets
		run {
			val outgoingPacketsListSize = outgoingPacketsList.size

			val outgoingPacketsListStart = (if (outgoingPacketsListSize > width) outgoingPacketsListSize - width else 0)

			val ymul = (height / outgoingPacketsScale).coerceAtMost(outgoingPacketsMinYMul)

			for (i in outgoingPacketsListStart until outgoingPacketsListSize - 1)
			{
				val outgoingPacketsY = outgoingPacketsList[i] * ymul
				val outgoingPacketsNextY = outgoingPacketsList[i + 1] * ymul

				RenderUtils.glColor(outgoingPacketsColor)
				GL11.glVertex2f((i - outgoingPacketsListStart).toFloat(), height + 1 - outgoingPacketsY.coerceAtMost(height))
				GL11.glVertex2f(i + 1.0F - outgoingPacketsListStart, height + 1 - outgoingPacketsNextY.coerceAtMost(height))
			}
		}

		GL11.glEnd()

		if (outgoingPacketsAverageEnabled)
		{
			GL11.glLineWidth(packetsOutgoingAverageThicknessValue.get())
			GL11.glBegin(GL11.GL_LINES)

			// Draw Outbound Packets Average
			run {
				val ypos = height + 1 - (height / outgoingPacketsScale).coerceAtMost(outgoingPacketsMinYMul) * outgoingPacketsAverage

				RenderUtils.glColor(outgoingPacketsColor)
				GL11.glVertex2f(0f, ypos)
				GL11.glVertex2f(width - outgoingAverageStringWidth - 2f, ypos)
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
		if (incomingPacketsAverageEnabled) stringFont.drawString(incomingAverageString, 0F, height + 1 - (height / incomingPacketsScale).coerceAtMost(incomingPacketsMinYMul) * incomingPacketsAverage - stringHeightHalf, incomingPacketsColor, shadow = true)

		// Draw Outbound Packets Average
		if (outgoingPacketsAverageEnabled) stringFont.drawString(outgoingAverageString, width - outgoingAverageStringWidth, height + 1 - (height / outgoingPacketsScale).coerceAtMost(outgoingPacketsMinYMul) * outgoingPacketsAverage - stringHeightHalf, outgoingPacketsColor, shadow = true)

		if (incomingPacketsPeakEnabled)
		{
			val incomingPacketsListSize = incomingPacketsList.size

			val incomingPacketsListStart = if (incomingPacketsListSize > width) incomingPacketsListSize - width else 0

			stringFont.drawString(incomingPeakString, incomingPacketsPeakIndex.toFloat() - incomingPeakStringWidth - incomingPacketsListStart, height + 1.0f - ((height / incomingPacketsScale).coerceAtMost(incomingPacketsMinYMul) * incomingPacketsPeak).coerceAtMost(height), incomingPacketsColor, shadow = true)
		}

		if (outgoingPacketsPeakEnabled)
		{
			val outgoingPacketsListSize = outgoingPacketsList.size

			val outgoingPacketsListStart = if (outgoingPacketsListSize > width) outgoingPacketsListSize - width else 0

			stringFont.drawString(outgoingPeakString, outgoingPacketsPeakIndex.toFloat() - outgoingPeakStringWidth - outgoingPacketsListStart, height + 1.0f - ((height / outgoingPacketsScale).coerceAtMost(outgoingPacketsMinYMul) * outgoingPacketsPeak).coerceAtMost(height), outgoingPacketsColor, shadow = true)
		}

		return Border(0F, 0F, width.toFloat(), height + 2)
	}
}
