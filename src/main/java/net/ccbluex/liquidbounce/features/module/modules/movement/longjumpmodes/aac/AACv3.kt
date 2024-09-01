/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.minecraft.util.math.Direction

object AACv3 : LongJumpMode("AACv3") {
    override fun onUpdate() {
        if (mc.player.fallDistance > 0.5f && !LongJump.teleported) {
            val value = 3.0
            val horizontalFacing = mc.player.horizontalFacing
            var x = 0.0
            var z = 0.0

            when (horizontalFacing) {
                Direction.NORTH -> z = -value
                Direction.EAST -> x = value
                Direction.SOUTH -> z = value
                Direction.WEST -> x = -value
                else -> {}
            }

            mc.player.updatePosition(mc.player.x + x, mc.player.z, mc.player.z + z)
            LongJump.teleported = true
        }
    }
}