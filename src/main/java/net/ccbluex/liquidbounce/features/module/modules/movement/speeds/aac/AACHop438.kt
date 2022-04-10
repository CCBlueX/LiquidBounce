/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.block.BlockCarpet
import kotlin.math.cos
import kotlin.math.sin

class AACHop438 : SpeedMode("AACHop4.3.8") {
    override fun onMotion() {}
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1.0f;

        if (!MovementUtils.isMoving || thePlayer.isInWater || thePlayer.isInLava ||
                thePlayer.isOnLadder || thePlayer.isRiding) return

        if (thePlayer.onGround)
            thePlayer.jump();
        else {
            if (thePlayer.fallDistance <= 0.1)
                mc.timer.timerSpeed = 1.5f;
            else if (thePlayer.fallDistance < 1.3)
                mc.timer.timerSpeed = 0.7f;
            else
                mc.timer.timerSpeed = 1f;
        }
    }

    override fun onMove(event: MoveEvent) {}
    override fun onDisable() {}
}