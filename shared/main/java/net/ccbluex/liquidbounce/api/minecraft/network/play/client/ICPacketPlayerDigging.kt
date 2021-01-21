/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos

interface ICPacketPlayerDigging : IPacket
{
	val status: WAction
	val position: WBlockPos
	val facing: IEnumFacing

	enum class WAction
	{
		START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK, DROP_ALL_ITEMS, DROP_ITEM, RELEASE_USE_ITEM;
	}
}
