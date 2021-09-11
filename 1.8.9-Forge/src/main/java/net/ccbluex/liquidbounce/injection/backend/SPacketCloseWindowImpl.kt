/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketCloseWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow

class SPacketCloseWindowImpl<out T : S2EPacketCloseWindow>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketCloseWindow
{
	override val windowId: Int
		get() = wrapped.windowId
}

fun ISPacketCloseWindow.unwrap(): S2EPacketCloseWindow = (this as SPacketCloseWindowImpl<*>).wrapped
fun S2EPacketCloseWindow.wrap(): ISPacketCloseWindow = SPacketCloseWindowImpl(this)
