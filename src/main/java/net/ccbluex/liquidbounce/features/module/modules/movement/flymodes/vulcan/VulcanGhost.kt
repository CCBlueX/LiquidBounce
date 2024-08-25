/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.minecraft.block.BlockLadder
import net.minecraft.block.material.Material
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.Box

object VulcanGhost : FlyMode("VulcanGhost") {

    override fun onEnable() {
        Chat.print("Ensure that you sneak on landing.")
        Chat.print("After landing, go backward (Air) and go forward to landing location, then sneak again.")
        Chat.print("And then you can turn off fly.")
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is PlayerPositionLookS2CPacket) {
            event.cancelEvent()
        }
    }

    override fun onBB(event: BlockBBEvent) {
        if (!mc.options.jumpKey.isPressed && mc.options.sneakKey.isPressed) return
        if (!event.block.material.blocksMovement() && event.block.material != Material.carpet && event.block.material != Material.vine && event.block.material != Material.snow && event.block !is BlockLadder) {
            event.boundingBox = Box(
                -2.0,
                -1.0,
                -2.0,
                2.0,
                1.0,
                2.0
            ).offset(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble()
            )
        }
    }
}
