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

object LadderJump : Module("LadderJump", ModuleCategory.MOVEMENT) {

    var jumped = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.onGround) {
            if (mc.thePlayer.isOnLadder) {
                mc.thePlayer.motionY = 1.5
                jumped = true
            } else jumped = false
        } else if (!mc.thePlayer.isOnLadder && jumped) {
            mc.thePlayer.motionY += 0.059
        }
    }

}