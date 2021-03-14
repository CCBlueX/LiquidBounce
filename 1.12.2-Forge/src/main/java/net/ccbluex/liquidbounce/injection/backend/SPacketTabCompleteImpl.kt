/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketTabComplete
import net.minecraft.network.play.server.SPacketTabComplete

class SPacketTabCompleteImpl<out T : SPacketTabComplete>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketTabComplete {
    override val completions: Array<String>
        get() = wrapped.matches
}

 fun ISPacketTabComplete.unwrap(): SPacketTabComplete = (this as SPacketTabCompleteImpl<*>).wrapped
 fun SPacketTabComplete.wrap(): ISPacketTabComplete = SPacketTabCompleteImpl(this)
