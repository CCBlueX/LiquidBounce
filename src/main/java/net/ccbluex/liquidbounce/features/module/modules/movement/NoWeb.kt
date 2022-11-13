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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "NoWeb", description = "Prevents you from getting slowed down in webs.", category = ModuleCategory.MOVEMENT)
class NoWeb : Module() {

    private val modeValue = ListValue("Mode", arrayOf("None", "AAC", "LAAC", "Rewi"), "None")

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isInWeb)
            return

        when (modeValue.get().lowercase()) {
            "none" -> thePlayer.isInWeb = false
            "aac" -> {
                thePlayer.jumpMovementFactor = 0.59f

                if (!mc.gameSettings.keyBindSneak.isKeyDown)
                    thePlayer.motionY = 0.0
            }
            "laac" -> {
                thePlayer.jumpMovementFactor = if (thePlayer.movementInput.moveStrafe != 0f) 1.0f else 1.21f

                if (!mc.gameSettings.keyBindSneak.isKeyDown)
                    thePlayer.motionY = 0.0

                if (thePlayer.onGround)
                    thePlayer.jump()
            }
            "rewi" -> {
                thePlayer.jumpMovementFactor = 0.42f

                if (thePlayer.onGround)
                    thePlayer.jump()
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}
