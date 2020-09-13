/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntity
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.minecraft.network.play.server.S14PacketEntity

class SPacketEntityImpl<T : S14PacketEntity>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEntity {
    override val onGround: Boolean
        get() = wrapped.onGround

    override fun getEntity(world: IWorld): IEntity? = wrapped.getEntity(world.unwrap())?.wrap()
}

inline fun ISPacketEntity.unwrap(): S14PacketEntity = (this as SPacketEntityImpl<*>).wrapped
inline fun S14PacketEntity.wrap(): ISPacketEntity = SPacketEntityImpl(this)