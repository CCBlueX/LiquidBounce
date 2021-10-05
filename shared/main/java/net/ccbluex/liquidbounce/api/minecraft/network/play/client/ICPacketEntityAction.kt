/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket

interface ICPacketEntityAction : IPacket
{
	val action: WAction

	enum class WAction
	{
		START_SNEAKING,
		STOP_SNEAKING,
		STOP_SLEEPING,
		START_SPRINTING,
		STOP_SPRINTING,
		RIDING_JUMP,
		OPEN_INVENTORY
	}
}
