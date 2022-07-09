/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerBlockPlacement
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.injection.backend.minecraft.item.wrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement

class CPacketPlayerBlockPlacementImpl<out T : C08PacketPlayerBlockPlacement>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketPlayerBlockPlacement
{
    override val position: WBlockPos
        get() = wrapped.position.wrap()

    override val stack: IItemStack?
        get() = wrapped.stack?.wrap()
}

fun ICPacketPlayerBlockPlacement.unwrap(): C08PacketPlayerBlockPlacement = (this as CPacketPlayerBlockPlacementImpl<*>).wrapped
fun C08PacketPlayerBlockPlacement.wrap(): ICPacketPlayerBlockPlacement = CPacketPlayerBlockPlacementImpl(this)
