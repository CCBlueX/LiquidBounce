/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld

interface ICPacketUseEntity : IPacket
{
	val action: WAction

	fun getEntityFromWorld(theWorld: IWorld): IEntity?

	enum class WAction
	{
		INTERACT,
		ATTACK,
		INTERACT_AT
	}
}
