/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction

class CPacketEntityActionImpl<T : C0BPacketEntityAction>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketEntityAction

inline fun ICPacketEntityAction.unwrap(): C0BPacketEntityAction = (this as CPacketEntityActionImpl<*>).wrapped
inline fun C0BPacketEntityAction.wrap(): ICPacketEntityAction = CPacketEntityActionImpl(this)