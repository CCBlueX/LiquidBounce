/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketCloseWindow
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.client.C0DPacketCloseWindow

class CPacketCloseWindowImpl<out T : C0DPacketCloseWindow>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketCloseWindow
{
    override val windowId: Int
        get() = wrapped.windowId
}

fun ICPacketCloseWindow.unwrap(): C0DPacketCloseWindow = (this as CPacketCloseWindowImpl<*>).wrapped
fun C0DPacketCloseWindow.wrap(): ICPacketCloseWindow = CPacketCloseWindowImpl(this)
