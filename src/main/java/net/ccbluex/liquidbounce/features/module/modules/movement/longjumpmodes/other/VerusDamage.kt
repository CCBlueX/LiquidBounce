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
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook

object VerusDamage : LongJumpMode("VerusDamage") {

    var damaged = false

    override fun onEnable() {
        val player = mc.thePlayer ?: return
        // Otherwise you'll get flagged.
        if (!isMoving) {
            Chat.print("Pls move while toggling LongJump. Using AutoJump option is recommended.")
            return
        }

        // Note: you'll flag once for Fly(G) | Loyisa Test Server
        sendPacket(C04PacketPlayerPosition(player.posX, player.posY + 3.0001, player.posZ, false))
        sendPacket(C06PacketPlayerPosLook(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch, false))
        sendPacket(C06PacketPlayerPosLook(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch, true))
        damaged = true
    }

    override fun onDisable() {
        damaged = false
    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) {
            LongJump.state = false
            return
        }

        /**
         * You can long jump up to 13-14+ blocks
         */
        if (damaged && isMoving) {
            player.jumpMovementFactor = 0.15f
            player.motionY += 0.015f

            // player onGround checks will not work due to sendPacket ground, so for temporary. I'll be using player motionY.
            if (autoDisable && player.motionY <= -0.4330104027478734) {
                player.stopXZ()
                LongJump.state = false
            }
        } else if (autoDisable) {
            LongJump.state = false
        }
    }
}