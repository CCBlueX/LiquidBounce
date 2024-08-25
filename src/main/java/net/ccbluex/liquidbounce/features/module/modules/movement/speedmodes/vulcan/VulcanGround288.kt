/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.potion.Potion

object VulcanGround288 : SpeedMode("VulcanGround2.8.8") {
    override fun onUpdate() {
        val player = mc.player ?: return
        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) return

        if (isMoving && collidesBottom()) {
            val speedEffect = player.getActivePotionEffect(Potion.moveSpeed)
            val isAffectedBySpeed = speedEffect != null && speedEffect.amplifier > 0
            val isMovingSideways = player.moveStrafing != 0f

            val strafe = when {
                isAffectedBySpeed -> 0.59f
                isMovingSideways -> 0.41f
                else -> 0.42f
            }

            strafe(strafe)
            player.velocityY = 0.005
        }
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket && collidesBottom()) {
            packet.y += 0.005
        }
    }

    private fun collidesBottom(): Boolean {
        val world = mc.world ?: return false
        val player = mc.player ?: return false

        return world.getCollidingBoundingBoxes(player, player.entityBoundingBox.offset(0.0, -0.005, 0.0)).isNotEmpty()
    }

    override fun onJump(event: JumpEvent) {
        event.cancelEvent()
    }
}
