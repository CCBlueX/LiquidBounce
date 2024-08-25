/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.minecraft.util.EnumFacing

object AACv3 : LongJumpMode("AACv3") {
    override fun onUpdate() {
        if (mc.player.fallDistance > 0.5f && !LongJump.teleported) {
            val value = 3.0
            val horizontalFacing = mc.player.horizontalFacing
            var x = 0.0
            var z = 0.0

            when (horizontalFacing) {
                EnumFacing.NORTH -> z = -value
                EnumFacing.EAST -> x = value
                EnumFacing.SOUTH -> z = value
                EnumFacing.WEST -> x = -value
                else -> {}
            }

            mc.player.setPosition(mc.player.posX + x, mc.player.posY, mc.player.posZ + z)
            LongJump.teleported = true
        }
    }
}