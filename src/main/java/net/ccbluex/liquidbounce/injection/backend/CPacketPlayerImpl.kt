/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer

class CPacketPlayerImpl<T : C03PacketPlayer>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketPlayer {
    override var x: Double
        get() = wrapped.x
        set(value) {
            wrapped.x = value
        }
    override var y: Double
        get() = wrapped.y
        set(value) {
            wrapped.y = value
        }
    override var z: Double
        get() = wrapped.z
        set(value) {
            wrapped.z = value
        }
    override var yaw: Float
        get() = wrapped.yaw
        set(value) {
            wrapped.yaw = value
        }
    override var pitch: Float
        get() = wrapped.pitch
        set(value) {
            wrapped.pitch = value
        }
    override var onGround: Boolean
        get() = wrapped.onGround
        set(value) {
            wrapped.onGround = value
        }
    override var rotating: Boolean
        get() = wrapped.rotating
        set(value) {
            wrapped.rotating = value
        }

}

inline fun ICPacketPlayer.unwrap(): C03PacketPlayer = (this as CPacketPlayerImpl<*>).wrapped
inline fun C03PacketPlayer.wrap(): ICPacketPlayer = CPacketPlayerImpl(this)