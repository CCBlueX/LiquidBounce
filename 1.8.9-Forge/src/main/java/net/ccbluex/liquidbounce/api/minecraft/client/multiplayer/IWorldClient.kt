/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.multiplayer

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld

interface IWorldClient : IWorld {
    val loadedEntityList: Collection<IEntity>

    fun sendQuittingDisconnectingPacket()
    fun sendBlockBreakProgress(entityId: Int, blockPos: WBlockPos, damage: Int)
    fun addEntityToWorld(entityId: Int, fakePlayer: IEntity)
    fun removeEntityFromWorld(entityId: Int)
}