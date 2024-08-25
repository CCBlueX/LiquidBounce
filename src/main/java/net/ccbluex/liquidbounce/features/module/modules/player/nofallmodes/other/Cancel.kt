/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object Cancel : NoFallMode("Cancel") {

    private var isFalling = false

    /**
     * NoFall Cancel
     * NOTE: The recommended distance for falling is < 15.
     */

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (event.isCancelled)
            return

        if (packet is PlayerPositionLookS2CPacket && isFalling) {
            sendPacket(PositionOnly(packet.x, packet.y, packet.z, true))
            isFalling = false
        }

        if (packet is PlayerMoveC2SPacket) {
            if (mc.player.fallDistance > 3F) {
                isFalling = true
                event.cancelEvent()
            }
        }
    }

    override fun onDisable() {
        mc.player.fallDistance = 0F
        isFalling = false
    }
}