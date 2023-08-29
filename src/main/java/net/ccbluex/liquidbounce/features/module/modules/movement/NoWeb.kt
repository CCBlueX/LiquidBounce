/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.ListValue

object NoWeb : Module("NoWeb", ModuleCategory.MOVEMENT) {

    private val mode by ListValue("Mode", arrayOf("None", "AAC", "LAAC", "Rewi", "MineBlaze"), "None")

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isInWeb)
            return

        when (mode.lowercase()) {
            "none" -> thePlayer.isInWeb = false
            "aac" -> {
                thePlayer.jumpMovementFactor = 0.59f

                if (!mc.gameSettings.keyBindSneak.isKeyDown)
                    thePlayer.motionY = 0.0
            }
            "laac" -> {
                thePlayer.jumpMovementFactor = if (thePlayer.movementInput.moveStrafe != 0f) 1f else 1.21f

                if (!mc.gameSettings.keyBindSneak.isKeyDown)
                    thePlayer.motionY = 0.0

                if (thePlayer.onGround)
                    thePlayer.jump()
            }
             "mineblaze" -> {
                if (mc.thePlayer.movementInput.moveStrafe == 0.0F && mc.gameSettings.keyBindForward.isKeyDown && mc.thePlayer.isCollidedVertically) {
                    mc.thePlayer.jumpMovementFactor = 0.74F
                } else {
                    mc.thePlayer.jumpMovementFactor = 0.2F
                    mc.thePlayer.onGround = true
                }
            }
            "rewi" -> {
                thePlayer.jumpMovementFactor = 0.42f

                if (thePlayer.onGround)
                    thePlayer.jump()
            }
        }
    }

    override val tag
        get() = mode
}
