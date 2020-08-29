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

@ModuleInfo(name = "LadderJump", description = "Boosts you up when touching a ladder.", category = ModuleCategory.MOVEMENT)
class LadderJump : Module() {

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.onGround) {
            if (thePlayer.isOnLadder) {
                thePlayer.motionY = 1.5
                jumped = true
            } else jumped = false
        } else if (!thePlayer.isOnLadder && jumped) thePlayer.motionY += 0.059
    }

    companion object {
        @JvmField
        var jumped = false
    }
}