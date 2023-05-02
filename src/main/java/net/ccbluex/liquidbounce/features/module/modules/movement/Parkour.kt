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
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving

object Parkour : Module("Parkour", ModuleCategory.MOVEMENT) {

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (isMoving && thePlayer.onGround && !thePlayer.isSneaking && !mc.gameSettings.keyBindSneak.isKeyDown && !mc.gameSettings.keyBindJump.isKeyDown &&
                mc.theWorld.getCollidingBoundingBoxes(thePlayer, thePlayer.entityBoundingBox
                        .offset(0.0, -0.5, 0.0).expand(-0.001, 0.0, -0.001)).isEmpty())
            thePlayer.jump()
    }
}
