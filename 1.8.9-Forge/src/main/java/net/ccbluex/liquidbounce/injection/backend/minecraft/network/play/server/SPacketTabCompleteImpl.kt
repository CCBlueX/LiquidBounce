/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketTabComplete
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.server.S3APacketTabComplete

class SPacketTabCompleteImpl<out T : S3APacketTabComplete>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketTabComplete
{
	override val completions: Array<String>
		get() = wrapped.func_149630_c()
}

fun ISPacketTabComplete.unwrap(): S3APacketTabComplete = (this as SPacketTabCompleteImpl<*>).wrapped
fun S3APacketTabComplete.wrap(): ISPacketTabComplete = SPacketTabCompleteImpl(this)
