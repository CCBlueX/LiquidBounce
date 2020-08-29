/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.play.server

interface ISPacketEntityVelocity {
    var motionX: Int
    var motionY: Int
    var motionZ: Int

    val entityID: Int
}