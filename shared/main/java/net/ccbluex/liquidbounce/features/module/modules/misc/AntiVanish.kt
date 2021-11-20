/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.IntegerValue

// Original code available in https://github.com/Project-EZ4H/FDPClient/blob/master/src/main/java/net/ccbluex/liquidbounce/features/module/modules/misc/AntiVanish.kt
@ModuleInfo(name = "AntiVanish", description = "Anti player vanish", category = ModuleCategory.MISC)
class AntiVanish : Module()
{
	private val notifyDelayValue = IntegerValue("NotifyDelay", 5000, 500, 10000)

	private val notifyTimer = MSTimer()

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val theWorld = mc.theWorld ?: return
		val packet = event.packet

		if (classProvider.isSPacketEntityEffect(packet))
		{
			if (theWorld.getEntityByID(packet.asSPacketEntityEffect().entityId) == null) vanish()
		}
		else if (classProvider.isSPacketEntity(packet) && packet.asSPacketEntity().getEntity(theWorld) == null) vanish()
	}

	private fun vanish()
	{
		if (notifyTimer.hasTimePassed(notifyDelayValue.get().toLong())) LiquidBounce.hud.addNotification(Notification(NotificationIcon.VANISH, name, "A player is vanished!", 1000))
		notifyTimer.reset()
	}
}
