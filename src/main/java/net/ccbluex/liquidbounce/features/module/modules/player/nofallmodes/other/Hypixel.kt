/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly

/*
* Working on Watchdog
* Tested on: mc.hypixel.net
* Credit: @localpthebest / Hypixel
*/
object Hypixel : NoFallMode("Hypixel") {

    private var jump = false

    override fun onPacket(event: PacketEvent) {
        val player = mc.player ?: return
        val packet = event.packet

        if (packet is PositionOnly) {
            if (player.fallDistance >= 3.3) {
                jump = true
            }

            if (jump && player.onGround) {
                packet.onGround = false

                if (!mc.options.jumpKey.isPressed) {
                    player.updatePosition(packet.positionX, packet.positionY + 0.09, packet.positionZ)
                }

                jump = false
            }
        }
    }
}