/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.api.MinecraftVersion
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils

@ModuleInfo(name = "PotionSaver", description = "Freezes all potion effects while you are standing still.", category = ModuleCategory.PLAYER, supportedVersions = [MinecraftVersion.MC_1_8])
class PotionSaver : Module()
{
	@EventTarget
	fun onPacket(e: PacketEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val packet = e.packet
		val isMovePacket = classProvider.isCPacketPlayer(packet) && !classProvider.isCPacketPlayerPosition(packet) && !classProvider.isCPacketPlayerPosLook(packet) && !classProvider.isCPacketPlayerPosLook(packet)

		if (!MovementUtils.isMoving && isMovePacket && !thePlayer.isUsingItem) e.cancelEvent()
	}
}
