package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import java.lang.Math.sqrt

@ModuleInfo(name = "Strafe", description = "Allows you to freely move in mid air.", category = ModuleCategory.MOVEMENT)
class Strafe : Module() {

    var jump: Boolean = false
    var wasDown: Boolean = false

    val strafeStrengthValue = FloatValue("StrafeStrength", 0.5F, 0F, 1F)
    val noMoveStopValue = BoolValue("NoMoveStop", false)
    val onGroundStrafeValue = BoolValue("OnGroundStrafe", false)
    val allDirectionsJumpsValue = BoolValue("AllDirectionsJumps", false)

    override fun onEnable() {
        wasDown = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer!!.onGround && mc.gameSettings.keyBindJump.isKeyDown && allDirectionsJumpsValue.get() &&
            MovementUtils.isMoving && !(mc.thePlayer!!.isInWater || mc.thePlayer!!.isInLava || mc.thePlayer!!.isOnLadder || mc.thePlayer!!.isInWeb)) {
            if (mc.gameSettings.keyBindJump.isKeyDown) {
                mc.gameSettings.keyBindJump.pressed = false
                this.wasDown = true
            }
            var yaw = mc.thePlayer!!.rotationYaw
            mc.thePlayer!!.rotationYaw = getMoveYaw()
            mc.thePlayer!!.jump()
            mc.thePlayer!!.rotationYaw = yaw
            if (this.wasDown) {
                mc.gameSettings.keyBindJump.pressed = true
                this.wasDown = false
            }
        } else {
            this.jump = false
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        val shotSpeed =
            sqrt((mc.thePlayer!!.motionX * mc.thePlayer!!.motionX) + (mc.thePlayer!!.motionZ * mc.thePlayer!!.motionZ))
        val speed = (shotSpeed * strafeStrengthValue.get())
        val motionX = (mc.thePlayer!!.motionX * (1 - strafeStrengthValue.get()))
        val motionZ = (mc.thePlayer!!.motionZ * (1 - strafeStrengthValue.get()))
        if (!MovementUtils.isMoving) {
            if (noMoveStopValue.get()) {
                mc.thePlayer!!.motionX = 0.0
                mc.thePlayer!!.motionZ = 0.0
            }
            return
        }
        if (!mc.thePlayer!!.onGround || onGroundStrafeValue.get()) {
            var yaw = getMoveYaw()
            mc.thePlayer!!.motionX = (((-Math.sin(Math.toRadians(yaw.toDouble())) * speed) + motionX))
            mc.thePlayer!!.motionZ = (((Math.cos(Math.toRadians(yaw.toDouble())) * speed) + motionZ))
        }
    }

    private fun getMoveYaw(): Float {
        var moveYaw = mc.thePlayer!!.rotationYaw
        if (mc.thePlayer!!.moveForward != 0f && mc.thePlayer!!.moveStrafing == 0f) {
            moveYaw = if (mc.thePlayer!!.moveForward > 0) 0F else 180.toFloat()
        } else if (mc.thePlayer!!.moveForward != 0f && mc.thePlayer!!.moveStrafing != 0f) {
            moveYaw = if (mc.thePlayer!!.moveForward > 0) 0F else 180.toFloat()
        } else if (mc.thePlayer!!.moveStrafing != 0f && mc.thePlayer!!.moveForward == 0f) {
            moveYaw = if (mc.thePlayer!!.moveStrafing > 0) (-90).toFloat() else 90.toFloat()
        }
        return moveYaw
    }
}