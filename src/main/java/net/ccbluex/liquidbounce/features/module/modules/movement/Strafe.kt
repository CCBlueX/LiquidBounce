package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.extensions.toDegreesF
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import kotlin.math.cos
import kotlin.math.sin

object Strafe : Module("Strafe", Category.MOVEMENT, gameDetecting = false, hideModule = false) {

    private val strength by FloatValue("Strength", 0.5F, 0F..1F)
    private val noMoveStop by BoolValue("NoMoveStop", false)
    private val onGroundStrafe by BoolValue("OnGroundStrafe", false)
    private val allDirectionsJump by BoolValue("AllDirectionsJump", false)

    private var wasDown = false
    private var jump = false

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (jump) {
            event.cancelEvent()
        }
    }

    override fun onEnable() {
        wasDown = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.player.onGround && mc.options.jumpKey.isPressed && allDirectionsJump && isMoving && !(mc.player.isInWater || mc.player.isInLava || mc.player.isOnLadder || mc.player.isInWeb)) {
            if (mc.options.jumpKey.isPressed) {
                mc.gameSettings.keyBindJump.pressed = false
                wasDown = true
            }
            val yaw = mc.player.rotationYaw
            mc.player.rotationYaw = direction.toDegreesF()
            mc.player.tryJump()
            mc.player.rotationYaw = yaw
            jump = true
            if (wasDown) {
                mc.gameSettings.keyBindJump.pressed = true
                wasDown = false
            }
        } else {
            jump = false
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (!isMoving) {
            if (noMoveStop) {
                mc.player.velocityX = .0
                mc.player.velocityZ = .0
            }
            return
        }

        val shotSpeed = speed
        val speed = shotSpeed * strength
        val velocityX = mc.player.velocityX * (1 - strength)
        val velocityZ = mc.player.velocityZ * (1 - strength)

        if (!mc.player.onGround || onGroundStrafe) {
            val yaw = direction
            mc.player.velocityX = -sin(yaw) * speed + velocityX
            mc.player.velocityZ = cos(yaw) * speed + velocityZ
        }
    }
}
