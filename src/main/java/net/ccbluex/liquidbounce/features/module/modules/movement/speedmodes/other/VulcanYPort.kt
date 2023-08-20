package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode.strafe
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving

class VulcanYPort : SpeedMode("VulcanYPort") {

    private var wasTimer = false
    private var ticks = 0

    override fun onUpdate() {
        ticks++
        if (wasTimer) {
            mc.timer.timerSpeed = 1.00f
            wasTimer = false
        }
        mc.thePlayer.jumpMovementFactor = 0.0245f
        if (!mc.thePlayer.onGround && ticks > 3 && mc.thePlayer.motionY > 0) {
            mc.thePlayer.motionY = -0.27
        }

        mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        if (getSpeed() < 0.215f && !mc.thePlayer.onGround) {
            strafe(0.215f)
        }
        if (mc.thePlayer.onGround && isMoving()) {
            ticks = 0
            mc.gameSettings.keyBindJump.pressed = false
            mc.thePlayer.jump()
            if (!mc.thePlayer.isAirBorne) {
                return
            }
            mc.timer.timerSpeed = 1.2f
            wasTimer = true
            if (getSpeed() < 0.48f) {
                strafe(0.48f)
            } else {
                strafe((getSpeed() * 0.985).toFloat())
            }
        } else if (!isMoving()) {
            mc.timer.timerSpeed = 1.00f
        }
    }

    override fun onMotion() {}

    override fun onMotion(event: MotionEvent) {}

    override fun onDisable() {
        val scaffoldModule = Client.moduleManager.getModule(Scaffold::class.java)

        if (!mc.thePlayer.isSneaking && !scaffoldModule!!.state)
            strafe(0.2f)
    }

    override fun onMove(event: MoveEvent) {
    }
}

class VulcanYPort : SpeedMode("VulcanYPort") {

    private var wasTimer = false
    private var ticks = 0

    override fun onUpdate() {
        ticks++
        if (wasTimer) {
            mc.timer.timerSpeed = 1.00f
            wasTimer = false
        }
        mc.thePlayer.jumpMovementFactor = 0.0245f
        if (!mc.thePlayer.onGround && ticks > 3 && mc.thePlayer.motionY > 0) {
            mc.thePlayer.motionY = -0.27
        }

        mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        if (getSpeed() < 0.215f && !mc.thePlayer.onGround) {
            strafe(0.215f)
        }
        if (mc.thePlayer.onGround && isMoving()) {
            ticks = 0
            mc.gameSettings.keyBindJump.pressed = false
            mc.thePlayer.jump()
            if (!mc.thePlayer.isAirBorne) {
                return
            }
            mc.timer.timerSpeed = 1.2f
            wasTimer = true
            if (getSpeed() < 0.48f) {
                strafe(0.48f)
            } else {
                strafe((getSpeed() * 0.985).toFloat())
            }
        } else if (!isMoving()) {
            mc.timer.timerSpeed = 1.00f
        }
    }

    override fun onMotion() {}

    override fun onMotion(event: MotionEvent) {}

    override fun onDisable() {
        val scaffoldModule = Client.moduleManager.getModule(Scaffold::class.java)

        if (!mc.thePlayer.isSneaking && !scaffoldModule!!.state)
            strafe(0.2f)
    }

    override fun onMove(event: MoveEvent) {
    }
}
