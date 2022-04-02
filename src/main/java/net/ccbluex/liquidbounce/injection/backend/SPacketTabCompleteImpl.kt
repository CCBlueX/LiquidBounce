/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketTabComplete
import net.minecraft.network.play.server.S3APacketTabComplete

class SPacketTabCompleteImpl<T : S3APacketTabComplete>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketTabComplete {
    override val completions: Array<String>
        get() = wrapped.func_149630_c()
}

inline fun ISPacketTabComplete.unwrap(): S3APacketTabComplete = (this as SPacketTabCompleteImpl<*>).wrapped
inline fun S3APacketTabComplete.wrap(): ISPacketTabComplete = SPacketTabCompleteImpl(this)