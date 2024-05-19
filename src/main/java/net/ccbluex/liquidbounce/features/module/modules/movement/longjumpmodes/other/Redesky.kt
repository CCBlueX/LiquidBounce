/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode

object Redesky : LongJumpMode("Redesky") {
    override fun onUpdate() {
        player.jumpMovementFactor = 0.15f
        player.motionY += 0.05f
    }
}