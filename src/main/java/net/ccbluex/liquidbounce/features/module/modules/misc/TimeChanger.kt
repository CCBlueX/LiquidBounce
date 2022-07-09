package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
@ModuleInfo(name = "TimeChanger", description = "", category = ModuleCategory.MISC)
class TimeChanger : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Fixed", "Normal"), "Fixed")
	private val timeValue = IntegerValue("Time", 1000, 0, 24000)
	private val timeChangeSpeedValue = IntegerValue("TimeChangeSpeed", 150, 0, 500)

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		mc.theWorld?.run { worldTime = if (modeValue.get().equals("Normal", ignoreCase = true)) (worldTime + timeChangeSpeedValue.get()) % 24000 else timeValue.get().toLong() }
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		if (classProvider.isSPacketTimeUpdate(event.packet)) event.cancelEvent()
	}
}
