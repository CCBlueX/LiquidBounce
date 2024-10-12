/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MovementInputEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.SimulatedPlayer

object Parkour : Module("Parkour", Category.MOVEMENT, subjective = true, gameDetecting = false, hideModule = false) {

    @EventTarget
    fun onMovementInput(event: MovementInputEvent) {
        val thePlayer = mc.thePlayer ?: return

        val simPlayer = SimulatedPlayer.fromClientPlayer(event.originalInput)

        simPlayer.tick()

        if (isMoving && thePlayer.onGround && !thePlayer.isSneaking && !mc.gameSettings.keyBindSneak.isKeyDown && !simPlayer.onGround) {
            event.originalInput.jump = true
        }

    }
}
