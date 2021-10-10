/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketAnimation
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.client.C0APacketAnimation

class CPacketAnimationImpl<out T : C0APacketAnimation>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketAnimation

fun ICPacketAnimation.unwrap(): C0APacketAnimation = (this as CPacketAnimationImpl<*>).wrapped
fun C0APacketAnimation.wrap(): ICPacketAnimation = CPacketAnimationImpl(this)
