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

	val x: Double // FIXME: Remove corrections (divide by 32), move it to 1.8.9 backend
	val y: Double
	val z: Double
}
