/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.value.FloatValue
import kotlin.math.cos
import kotlin.math.sin

object NoClip : Module("NoClip", Category.MOVEMENT, hideModule = false) {
val speed by FloatValue("Speed", 0.5f, 0f..10f)

    override fun onDisable() {
        mc.thePlayer?.noClip = false
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

	    strafe(speed, stopWhenNoInput = true, event)

        player.noClip = true
        player.onGround = false

        player.capabilities.isFlying = false

        var ySpeed = 0.0

        if (mc.gameSettings.keyBindJump.isKeyDown)
            ySpeed += speed

        if (mc.gameSettings.keyBindSneak.isKeyDown)
            ySpeed -= speed

        player.motionY = ySpeed
        event.y = ySpeed
    }
}
