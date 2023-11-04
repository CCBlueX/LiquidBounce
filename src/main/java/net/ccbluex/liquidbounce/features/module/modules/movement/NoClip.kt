/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.value.FloatValue
import kotlin.math.cos
import kotlin.math.sin

object NoClip : Module("NoClip", ModuleCategory.MOVEMENT) {
val speed by FloatValue("Speed", 0.5f, 0f..10f)

    override fun onDisable() {
        mc.thePlayer?.noClip = false
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        val thePlayer = mc.thePlayer ?: return

		if (isMoving) {
			val dir = direction;
			
			val xDir = -sin(dir) * speed
			val zDir = cos(dir) * speed
			
			event.x = xDir
			event.z = zDir
			thePlayer.motionX = xDir * 0.9
			thePlayer.motionZ = zDir * 0.9
		}
		else
		{
			event.x = 0.0
			event.z = 0.0
			thePlayer.motionX = 0.0
			thePlayer.motionZ = 0.0
		}

        thePlayer.noClip = true
        thePlayer.onGround = false

        thePlayer.capabilities.isFlying = false

        var ySpeed = 0.0

        if (mc.gameSettings.keyBindJump.isKeyDown)
            ySpeed += speed

        if (mc.gameSettings.keyBindSneak.isKeyDown)
            ySpeed -= speed

        thePlayer.motionY = ySpeed
        event.y = ySpeed
    }
}
