package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon

// Ported from FDPClient (https://github.com/Project-EZ4H/FDPClient)
// Original code is available in https://github.com/Project-EZ4H/FDPClient/blob/master/src/main/java/net/ccbluex/liquidbounce/features/module/modules/player/HackerDetector.kt
@ModuleInfo(name = "LightningDetector", description = "Check lightning spawn at.", category = ModuleCategory.WORLD)
class LightningDetector : Module()
{
	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		if (classProvider.isSPacketSpawnGlobalEntity(event.packet))
		{
			val packet = event.packet.asSPacketSpawnGlobalEntity()

			if (packet.type != 1) return

			LiquidBounce.hud.addNotification(Notification(NotificationIcon.INFORMATION, name, "X:" + packet.x + " Y:" + packet.y + " Z:" + packet.z, 10000))
		}
	}
}
