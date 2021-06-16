/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import java.util.*

interface ISPacketPlayerSpawn : IPacket
{
	val entityID: Int
	val uuid: UUID

	val x: Int
	val y: Int
	val z: Int
}
