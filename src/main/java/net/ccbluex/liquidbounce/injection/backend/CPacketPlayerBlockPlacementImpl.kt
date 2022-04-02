/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerBlockPlacement
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement

class CPacketPlayerBlockPlacementImpl<T : C08PacketPlayerBlockPlacement>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketPlayerBlockPlacement

inline fun ICPacketPlayerBlockPlacement.unwrap(): C08PacketPlayerBlockPlacement = (this as CPacketPlayerBlockPlacementImpl<*>).wrapped
inline fun C08PacketPlayerBlockPlacement.wrap(): ICPacketPlayerBlockPlacement = CPacketPlayerBlockPlacementImpl(this)