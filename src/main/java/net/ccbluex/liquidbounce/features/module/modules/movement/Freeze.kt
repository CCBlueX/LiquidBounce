/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object Freeze : Module("Freeze", Category.MOVEMENT) {
    private var velocityX = 0.0
    private var velocityY = 0.0
    private var velocityZ = 0.0
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0

    override fun onEnable() {
        mc.player ?: return

        x = mc.player.x
        y = mc.player.y
        z = mc.player.z
        velocityX = mc.player.velocityX
        velocityY = mc.player.velocityY
        velocityZ = mc.player.velocityZ
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        mc.player.velocityX = 0.0
        mc.player.velocityY = 0.0
        mc.player.velocityZ = 0.0
        mc.player.updatePosition(x, y, z)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is PlayerMoveC2SPacket) {
            event.cancelEvent()
        }
        if (event.packet is PlayerPositionLookS2CPacket) {
            x = event.packet.x
            y = event.packet.y
            z = event.packet.z
            velocityX = 0.0
            velocityY = 0.0
            velocityZ = 0.0
        }
    }

    override fun onDisable() {
        mc.player.velocityX = velocityX
        mc.player.velocityY = velocityY
        mc.player.velocityZ = velocityZ
         mc.player.updatePosition(x, y, z)
    }
}
