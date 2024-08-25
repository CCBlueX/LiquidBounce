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

object LadderJump : Module("LadderJump", Category.MOVEMENT) {

    var jumped = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.player.onGround) {
            if (mc.player.isOnLadder) {
                mc.player.velocityY = 1.5
                jumped = true
            } else jumped = false
        } else if (!mc.player.isOnLadder && jumped) {
            mc.player.velocityY += 0.059
        }
    }

}