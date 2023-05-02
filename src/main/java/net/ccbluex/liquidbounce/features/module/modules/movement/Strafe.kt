package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.extensions.toDegreesF
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import kotlin.math.cos
import kotlin.math.sin

object Strafe : Module("Strafe", ModuleCategory.MOVEMENT) {

    private var strength by FloatValue("Strength", 0.5F, 0F..1F)
    private var noMoveStop by BoolValue("NoMoveStop", false)
    private var onGroundStrafe by BoolValue("OnGroundStrafe", false)
    private var allDirectionsJump by BoolValue("AllDirectionsJump", false)

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
        if (mc.thePlayer.onGround && mc.gameSettings.keyBindJump.isKeyDown && allDirectionsJump && isMoving && !(mc.thePlayer.isInWater || mc.thePlayer.isInLava || mc.thePlayer.isOnLadder || mc.thePlayer.isInWeb)) {
            if (mc.gameSettings.keyBindJump.isKeyDown) {
                mc.gameSettings.keyBindJump.pressed = false
                wasDown = true
            }
            val yaw = mc.thePlayer.rotationYaw
            mc.thePlayer.rotationYaw = direction.toDegreesF()
            mc.thePlayer.jump()
            mc.thePlayer.rotationYaw = yaw
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
                mc.thePlayer.motionX = .0
                mc.thePlayer.motionZ = .0
            }
            return
        }

        val shotSpeed = speed
        val speed = shotSpeed * strength
        val motionX = mc.thePlayer.motionX * (1 - strength)
        val motionZ = mc.thePlayer.motionZ * (1 - strength)

        if (!mc.thePlayer.onGround || onGroundStrafe) {
            val yaw = direction
            mc.thePlayer.motionX = -sin(yaw) * speed + motionX
            mc.thePlayer.motionZ = cos(yaw) * speed + motionZ
        }
    }
}
