/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketTabComplete
import net.minecraft.network.play.server.SPacketTabComplete

class SPacketTabCompleteImpl<T : SPacketTabComplete>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketTabComplete {
    override val completions: Array<String>
        get() = wrapped.matches
}

inline fun ISPacketTabComplete.unwrap(): SPacketTabComplete = (this as SPacketTabCompleteImpl<*>).wrapped
inline fun SPacketTabComplete.wrap(): ISPacketTabComplete = SPacketTabCompleteImpl(this)