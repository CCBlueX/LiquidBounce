/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.network.play.client.C0BPacketEntityAction

object Matrix2 : SpeedMode("Matrix2") {
    
    override fun onUpdate() {
        if (mc.thePlayer.isInWater || mc.thePlayer.isInLava || mc.thePlayer.isInWeb || mc.thePlayer.isOnLadder) return
        if (isMoving) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.tryJump()
                mc.timer.timerSpeed = 0.525f
                strafe()

                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
            } else {
                mc.timer.timerSpeed = 1.075f

                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
            }

            if (mc.thePlayer.fallDistance <= 0.8 && mc.thePlayer.moveStrafing == 0f) {
                mc.thePlayer.speedInAir = 0.02035f
            } else {
                mc.thePlayer.speedInAir = 0.02f
            }
        } else {
            mc.timer.timerSpeed = 1f
        }
    }
    
    override fun onDisable() {
        mc.thePlayer.speedInAir = 0.02f
        mc.timer.timerSpeed = 1f
    }
}
