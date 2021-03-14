/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerBlockPlacement
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock

class CPacketPlayerBlockPlacementImpl<out T : CPacketPlayerTryUseItemOnBlock>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketPlayerBlockPlacement

 fun ICPacketPlayerBlockPlacement.unwrap(): CPacketPlayerTryUseItemOnBlock = (this as CPacketPlayerBlockPlacementImpl<*>).wrapped
 fun CPacketPlayerTryUseItemOnBlock.wrap(): ICPacketPlayerBlockPlacement = CPacketPlayerBlockPlacementImpl(this)
