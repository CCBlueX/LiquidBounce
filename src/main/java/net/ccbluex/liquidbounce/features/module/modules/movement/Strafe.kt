package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@ModuleInfo(name = "Strafe", description = "Allows you to freely move in mid air.", category = ModuleCategory.MOVEMENT)
class Strafe : Module() {

    private var strengthValue= FloatValue("Strength", 0.5F, 0F, 1F)
    private var noMoveStopValue = BoolValue("NoMoveStop", false)
    private var onGroundStrafeValue = BoolValue("OnGroundStrafe", false)
    private var allDirectionsJumpValue = BoolValue("AllDirectionsJump", false)

    private var wasDown: Boolean = false
    private var jump: Boolean = false

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
        if (mc.thePlayer!!.onGround && mc.gameSettings.keyBindJump.isKeyDown && allDirectionsJumpValue.get() && (mc.thePlayer!!.movementInput.moveForward != 0F || mc.thePlayer!!.movementInput.moveStrafe != 0F) && !(mc.thePlayer!!.isInWater || mc.thePlayer!!.isInLava || mc.thePlayer!!.isOnLadder || mc.thePlayer!!.isInWeb)) {
            if (mc.gameSettings.keyBindJump.isKeyDown) {
                mc.gameSettings.keyBindJump.pressed = false
                wasDown = true
            }
            val yaw = mc.thePlayer!!.rotationYaw
            mc.thePlayer!!.rotationYaw = getMoveYaw()
            mc.thePlayer!!.jump()
            mc.thePlayer!!.rotationYaw = yaw
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
        val shotSpeed = sqrt((mc.thePlayer!!.motionX * mc.thePlayer!!.motionX) + (mc.thePlayer!!.motionZ * mc.thePlayer!!.motionZ))
        val speed = (shotSpeed * strengthValue.get())
        val motionX = (mc.thePlayer!!.motionX * (1 - strengthValue.get()))
        val motionZ = (mc.thePlayer!!.motionZ * (1 - strengthValue.get()))
        if (!(mc.thePlayer!!.movementInput.moveForward != 0F || mc.thePlayer!!.movementInput.moveStrafe != 0F)) {
            if (noMoveStopValue.get()) {
                mc.thePlayer!!.motionX = 0.0
                mc.thePlayer!!.motionZ = 0.0
            }
            return
        }
        if (!mc.thePlayer!!.onGround || onGroundStrafeValue.get()) {
            val yaw = getMoveYaw()
            mc.thePlayer!!.motionX = (((-sin(Math.toRadians(yaw.toDouble())) * speed) + motionX))
            mc.thePlayer!!.motionZ = (((cos(Math.toRadians(yaw.toDouble())) * speed) + motionZ))
        }
    }


    private fun getMoveYaw(): Float {
        var moveYaw = mc.thePlayer!!.rotationYaw
        if (mc.thePlayer!!.moveForward != 0F && mc.thePlayer!!.moveStrafing == 0F) {
            moveYaw += if(mc.thePlayer!!.moveForward > 0) 0 else 180
        } else if (mc.thePlayer!!.moveForward != 0F && mc.thePlayer!!.moveStrafing != 0F) {
            if (mc.thePlayer!!.moveForward > 0) {
                moveYaw += if (mc.thePlayer!!.moveStrafing > 0) -45 else 45
            } else {
                moveYaw -= if (mc.thePlayer!!.moveStrafing > 0) -45 else 45
            }
            moveYaw += if(mc.thePlayer!!.moveForward > 0) 0 else 180
        } else if (mc.thePlayer!!.moveStrafing != 0F && mc.thePlayer!!.moveForward == 0F) {
            moveYaw += if(mc.thePlayer!!.moveStrafing > 0) -90 else 90
        }
        return moveYaw
    }
}
