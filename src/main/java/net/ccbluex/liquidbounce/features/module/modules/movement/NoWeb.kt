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
class NoWeb : Module()
{
    private val modeValue = ListValue("Mode", arrayOf("None", "AAC3.1.5", "AAC3.3.6-WebWalk", "Rewinside", "AAC4", "OldMatrix"), "None")

    private var wasInWeb = false

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val thePlayer = mc.thePlayer ?: return
        val gameSettings = mc.gameSettings

        if (!thePlayer.isInWeb)
        {
            if (wasInWeb)
            {
                mc.timer.timerSpeed = 1.0F // Reset timer speed
                wasInWeb = false
            }

            return
        }

        wasInWeb = true

        when (modeValue.get().lowercase())
        {
            "none" -> thePlayer.isInWeb = false

            "aac3.1.5" ->
            {
                thePlayer.jumpMovementFactor = 0.59f

                if (!gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = 0.0
            }

            "aac3.3.6-webwalk" ->
            {
                // Usage: https://youtu.be/AhUljvLhFSA?t=43

                thePlayer.jumpMovementFactor = if (thePlayer.movementInput.moveStrafe != 0f) 1.0f else 1.21f

                if (!gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = 0.0

                if (thePlayer.onGround) thePlayer.jump()
            }

            "rewinside" ->
            {
                thePlayer.jumpMovementFactor = 0.42f

                if (thePlayer.onGround) thePlayer.jump()
            }

            "aac4" ->
            {
                mc.timer.timerSpeed = 0.99F
                thePlayer.jumpMovementFactor = 0.02958f
                thePlayer.motionY -= 0.00775

                if (thePlayer.onGround)
                {
                    thePlayer.jump()
                    thePlayer.motionY = 0.4050
                    mc.timer.timerSpeed = 1.35F
                }
            }

            "oldmatrix" ->
            {
                thePlayer.jumpMovementFactor = 0.124133333f
                thePlayer.motionY = -0.0125
                if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.1625
                if (thePlayer.onGround)
                {
                    thePlayer.jump()
                    thePlayer.motionY = 0.2425
                }
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}
