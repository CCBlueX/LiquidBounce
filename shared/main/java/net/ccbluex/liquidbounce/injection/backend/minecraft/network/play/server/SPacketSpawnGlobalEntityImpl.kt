/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketSpawnGlobalEntity
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity

class SPacketSpawnGlobalEntityImpl<out T : S2CPacketSpawnGlobalEntity>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketSpawnGlobalEntity
{
    override val type: Int
        get() = wrapped.func_149052_c()

    override val x: Double
        get() = wrapped.func_149051_d().toDouble()
    override val y: Double
        get() = wrapped.func_149050_e().toDouble()
    override val z: Double
        get() = wrapped.func_149049_f().toDouble()
}

fun ISPacketSpawnGlobalEntity.unwrap(): S2CPacketSpawnGlobalEntity = (this as SPacketSpawnGlobalEntityImpl<*>).wrapped
fun S2CPacketSpawnGlobalEntity.wrap(): ISPacketSpawnGlobalEntity = SPacketSpawnGlobalEntityImpl(this)
