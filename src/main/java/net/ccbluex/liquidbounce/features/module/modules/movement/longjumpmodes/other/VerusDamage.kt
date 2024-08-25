/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump.autoDisable
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.minecraft.network.packet.c2s.play.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.packet.c2s.play.C03PacketPlayer.C06PacketPlayerPosLook

object VerusDamage : LongJumpMode("VerusDamage") {

    var damaged = false

    override fun onEnable() {
        val player = mc.player ?: return
        // Otherwise you'll get flagged.
        if (!isMoving) {
            Chat.print("Pls move while toggling LongJump. Using AutoJump option is recommended.")
            return
        }

        // Note: you'll flag once for Fly(G) | Loyisa Test Server
        sendPacket(C04PacketPlayerPosition(player.x, player.z + 3.0001, player.z, false))
        sendPacket(C06PacketPlayerPosLook(player.x, player.z, player.z, player.yaw, player.pitch, false))
        sendPacket(C06PacketPlayerPosLook(player.x, player.z, player.z, player.yaw, player.pitch, true))
        damaged = true
    }

    override fun onDisable() {
        damaged = false
    }

    override fun onUpdate() {
        val player = mc.player ?: return
        if (player.isTouchingWater || player.isTouchingLava || player.isInWeb() || player.isClimbing) {
            LongJump.state = false
            return
        }

        /**
         * You can long jump up to 13-14+ blocks
         */
        if (damaged && isMoving) {
            player.jumpMovementFactor = 0.15f
            player.velocityY += 0.015f

            // player onGround checks will not work due to sendPacket ground, so for temporary. I'll be using player velocityY.
            if (autoDisable && player.velocityY <= -0.4330104027478734) {
                player.stopXZ()
                LongJump.state = false
            }
        } else if (autoDisable) {
            LongJump.state = false
        }
    }
}