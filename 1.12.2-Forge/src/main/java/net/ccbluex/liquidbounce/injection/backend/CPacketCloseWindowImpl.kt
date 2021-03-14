/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketCloseWindow
import net.minecraft.network.play.client.CPacketCloseWindow

class CPacketCloseWindowImpl<out T : CPacketCloseWindow>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketCloseWindow

 fun ICPacketCloseWindow.unwrap(): CPacketCloseWindow = (this as CPacketCloseWindowImpl<*>).wrapped
 fun CPacketCloseWindow.wrap(): ICPacketCloseWindow = CPacketCloseWindowImpl(this)
