/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import java.util.*

@ModuleInfo(name = "NoRotateSet", description = "Prevents the server from rotating your head.", category = ModuleCategory.MISC)
class NoRotateSet : Module()
{
	val confirmValue = BoolValue("Confirm", true)
	val confirmIllegalRotationValue = BoolValue("ConfirmIllegalRotation", false)
	val noZeroValue = BoolValue("NoZero", false)

	// Implementation moved to MixinNetHandlerPlayClient
	//	@EventTarget
	//	fun onPacket(event: PacketEvent)
	//	{
	//		val thePlayer = mc.thePlayer ?: return
	//
	//		if (classProvider.isSPacketPlayerPosLook(event.packet))
	//		{
	//			val packet = event.packet.asSPacketPosLook()
	//
	//			if (noZeroValue.get() && packet.yaw == 0F && packet.pitch == 0F) return
	//
	//			if (confirmIllegalRotationValue.get() || packet.pitch <= 90 && packet.pitch >= -90 && RotationUtils.serverRotation != null && packet.yaw != RotationUtils.serverRotation.yaw && packet.pitch != RotationUtils.serverRotation.pitch)
	//			{
	//
	//				if (confirmValue.get()) mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerLook(packet.yaw, packet.pitch, thePlayer.onGround))
	//			}
	//
	//			packet.yaw = thePlayer.rotationYaw
	//			packet.pitch = thePlayer.rotationPitch
	//		}
	//	}

	override val tag: String?
		get()
		{
			val tagBuilder = StringJoiner(", ")
			if (confirmValue.get()) tagBuilder.add("Confirm${if (confirmIllegalRotationValue.get()) "(Allow illegal)" else ""}")
			if (noZeroValue.get()) tagBuilder.add("NoZero")

			return "$tagBuilder"
		}
}
