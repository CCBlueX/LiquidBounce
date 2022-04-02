/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket

@Suppress("INAPPLICABLE_JVM_NAME")
interface ICPacketPlayer : IPacket {
    var x: Double
    var y: Double
    var z: Double

    var yaw: Float
    var pitch: Float
    var onGround: Boolean

    @get:JvmName("isRotating")
    var rotating: Boolean
}