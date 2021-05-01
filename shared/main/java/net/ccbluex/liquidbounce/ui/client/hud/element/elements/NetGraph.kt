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
import net.ccbluex.liquidbounce.utils.PPSCounter
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.FloatValue
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

	private val pingUpdatePeriodValue = IntegerValue("PingUpdatePeriod", 500, 100, 2000)

	private val pingyMultiplier = FloatValue("Ping-yMultiplier", 10F, 0.001F, 50F)
	private val pingThicknessValue = FloatValue("Ping-Thickness", 2F, 1F, 3F)
	private val pingColorRedValue = IntegerValue("Ping-R", 0, 0, 255)
	private val pingColorGreenValue = IntegerValue("Ping-G", 111, 0, 255)
	private val pingColorBlueValue = IntegerValue("Ping-B", 255, 0, 255)

	private val packetUpdatePeriodValue = IntegerValue("PacketsUpdatePeriod", 200, 100, 1000)

	private val inPacketYMultiplier = FloatValue("InboundPacket-yMultiplier", 1F, 0.001F, 50F)
	private val inPacketThicknessValue = FloatValue("InboundPacket-Thickness", 2F, 1F, 3F)
	private val inPacketColorRedValue = IntegerValue("InboundPacket-R", 255, 0, 255)
	private val inPacketColorGreenValue = IntegerValue("InboundPacket-G", 111, 0, 255)
	private val inPacketColorBlueValue = IntegerValue("InboundPacket-B", 0, 0, 255)

	private val outPacketYMultiplier = FloatValue("OutboundPacket-yMultiplier", 1F, 0.001F, 50F)
	private val outPacketThicknessValue = FloatValue("OutboundPacket-Thickness", 2F, 1F, 3F)
	private val outPacketColorRedValue = IntegerValue("OutboundPacket-R", 255, 0, 255)
	private val outPacketColorGreenValue = IntegerValue("OutboundPacket-G", 0, 0, 255)
	private val outPacketColorBlueValue = IntegerValue("OutboundPacket-B", 111, 0, 255)

	private val pingUpdateTimer = MSTimer()
	private val packetUpdateTimer = MSTimer()

	private val pingList = ArrayList<Int>()
	private val inPacketList = ArrayList<Int>()
	private val outPacketList = ArrayList<Int>()

	override fun drawElement(): Border?
	{
		val thePlayer = mc.thePlayer ?: return null

		val width = widthValue.get()
		val height = heightValue.get().toFloat()
		val pingUpdatePeriod = pingUpdatePeriodValue.get().toLong()
		val packetUpdatePeriod = packetUpdatePeriodValue.get().toLong()

		if (pingUpdateTimer.hasTimePassed(pingUpdatePeriod))
		{
			val ping = thePlayer.getPing()

			pingList.add(ping)
			while (pingList.size > width) pingList.removeAt(0)

			pingUpdateTimer.reset()
		}

		if (packetUpdateTimer.hasTimePassed(packetUpdatePeriod))
		{
			val inboundPackets = PPSCounter.getPacketCount(PPSCounter.BoundType.INBOUND, packetUpdatePeriod)
			val outboundPackets = PPSCounter.getPacketCount(PPSCounter.BoundType.OUTBOUND, packetUpdatePeriod)

			inPacketList.add(inboundPackets)
			while (inPacketList.size > width) inPacketList.removeAt(0)
			outPacketList.add(outboundPackets)
			while (outPacketList.size > width) outPacketList.removeAt(0)

			packetUpdateTimer.reset()
		}

		val pingYMul = pingyMultiplier.get() * 0.1f
		val inPacketYMul = inPacketYMultiplier.get() * 0.1f
		val outPacketYMul = outPacketYMultiplier.get() * 0.1f

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
		GL11.glLineWidth(inPacketThicknessValue.get())
		GL11.glBegin(GL11.GL_LINES)

		// Draw Inbound Packets
		run {
			val inPacketListSize = inPacketList.size

			val inPacketListStart = (if (inPacketListSize > width) inPacketListSize - width else 0)
			for (i in inPacketListStart until inPacketListSize - 1)
			{
				val inPacketY = inPacketList[i] * inPacketYMul
				val inPacketNextY = inPacketList[i + 1] * inPacketYMul

				RenderUtils.glColor(Color(inPacketColorRedValue.get(), inPacketColorGreenValue.get(), inPacketColorBlueValue.get(), 255))
				GL11.glVertex2f(i.toFloat() - inPacketListStart, height + 1 - inPacketY.coerceAtMost(height))
				GL11.glVertex2f(i + 1.0F - inPacketListStart, height + 1 - inPacketNextY.coerceAtMost(height))
			}
		}

		GL11.glEnd()
		GL11.glLineWidth(outPacketThicknessValue.get())
		GL11.glBegin(GL11.GL_LINES)

		// Draw Outbound Packets
		run {
			val outPacketListSize = outPacketList.size

			val outPacketListStart = (if (outPacketListSize > width) outPacketListSize - width else 0)
			for (i in outPacketListStart until outPacketListSize - 1)
			{
				val outPacketY = outPacketList[i] * outPacketYMul
				val outPacketNextY = outPacketList[i + 1] * outPacketYMul

				RenderUtils.glColor(Color(outPacketColorRedValue.get(), outPacketColorGreenValue.get(), outPacketColorBlueValue.get(), 255))
				GL11.glVertex2f((i - outPacketListStart).toFloat(), height + 1 - outPacketY.coerceAtMost(height))
				GL11.glVertex2f(i + 1.0F - outPacketListStart, height + 1 - outPacketNextY.coerceAtMost(height))
			}
		}

		GL11.glEnd()

		GL11.glEnable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_LINE_SMOOTH)
		GL11.glEnable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(true)
		GL11.glDisable(GL11.GL_BLEND)
		classProvider.glStateManager.resetColor()

		return Border(0F, 0F, width.toFloat(), height + 2)
	}
}
