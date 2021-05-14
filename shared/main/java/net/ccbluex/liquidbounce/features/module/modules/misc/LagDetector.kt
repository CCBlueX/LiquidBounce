/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "LagDetector", description = "Detects network issues and notify it visually.", category = ModuleCategory.MISC)
class LagDetector : Module()
{
	private val thresholdMSValue = IntegerValue("ThresholdMS", 1000, 200, 2000)

	private val notificationYOffsetValue = FloatValue("NotificationYOffset", -14F, -50F, 50F)

	companion object
	{
		private val packetTimer = MSTimer()

		fun onPacketReceived()
		{
			packetTimer.reset()
		}
	}

	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		if (!packetTimer.hasTimePassed(thresholdMSValue.get().toLong())) return

		val info = "Lag Detected: ${packetTimer.getTime()}"

		val scaledResolution = classProvider.createScaledResolution(mc)

		val middleScreenX = scaledResolution.scaledWidth shr 1
		val middleScreenY = scaledResolution.scaledHeight shr 1

		val stringWidthHalf = Fonts.font40.getStringWidth(info) * 0.5F
		val yoffset = notificationYOffsetValue.get()

		RenderUtils.drawBorderedRect(middleScreenX - stringWidthHalf - 2F, middleScreenY + yoffset - 2F, middleScreenX + stringWidthHalf + 2F, middleScreenY + yoffset + 9F, 3F, -16777216, -16777216)

		classProvider.glStateManager.resetColor()
		Fonts.font40.drawString(info, middleScreenX - stringWidthHalf, middleScreenY + yoffset, 0xFF0000, false)
	}

	override val tag: String?
		get() = if (packetTimer.hasTimePassed(thresholdMSValue.get().toLong())) "\u00A74\u00A7o${packetTimer.getTime()}" else null
}
