/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category

object PerfectHorseJump : Module("PerfectHorseJump", Category.MOVEMENT, subjective = true, gameDetecting = false) {

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return

        thePlayer.horseJumpPowerCounter = 9
        thePlayer.horseJumpPower = 1f
    }
}
