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

object NoClip : Module("NoClip", ModuleCategory.MOVEMENT) {

    override fun onDisable() {
        mc.thePlayer?.noClip = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        thePlayer.noClip = true
        thePlayer.fallDistance = 0f
        thePlayer.onGround = false

        thePlayer.capabilities.isFlying = false
        thePlayer.motionX = 0.0
        thePlayer.motionY = 0.0
        thePlayer.motionZ = 0.0

        val speed = 0.32f

        thePlayer.jumpMovementFactor = speed

        if (mc.gameSettings.keyBindJump.isKeyDown)
            thePlayer.motionY += speed.toDouble()

        if (mc.gameSettings.keyBindSneak.isKeyDown)
            thePlayer.motionY -= speed.toDouble()
    }
}
