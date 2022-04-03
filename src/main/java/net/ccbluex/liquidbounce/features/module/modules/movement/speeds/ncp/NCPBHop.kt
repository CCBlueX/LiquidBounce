/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.potion.Potion
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class NCPBHop : SpeedMode("NCPBHop") {
    private var level = 1
    private var moveSpeed = 0.2873
    private var lastDist = 0.0
    private var timerDelay = 0
    override fun onEnable() {
        mc.timer.timerSpeed = 1f
        level = if (mc.theWorld!!.getCollidingBoundingBoxes(mc.thePlayer!!, mc.thePlayer!!.entityBoundingBox.offset(0.0, mc.thePlayer!!.motionY, 0.0)).size > 0 || mc.thePlayer!!.isCollidedVertically) 1 else 4
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        moveSpeed = baseMoveSpeed
        level = 0
    }

    override fun onMotion() {
        val xDist = mc.thePlayer!!.posX - mc.thePlayer!!.prevPosX
        val zDist = mc.thePlayer!!.posZ - mc.thePlayer!!.prevPosZ
        lastDist = Math.sqrt(xDist * xDist + zDist * zDist)
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {
        ++timerDelay
        timerDelay %= 5
        if (timerDelay != 0) {
            mc.timer.timerSpeed = 1f
        } else {
            if (MovementUtils.isMoving) mc.timer.timerSpeed = 32767f
            if (MovementUtils.isMoving) {
                mc.timer.timerSpeed = 1.3f
                mc.thePlayer!!.motionX *= 1.0199999809265137
                mc.thePlayer!!.motionZ *= 1.0199999809265137
            }
        }
        if (mc.thePlayer!!.onGround && MovementUtils.isMoving) level = 2
        if (round(mc.thePlayer!!.posY - mc.thePlayer!!.posY.toInt().toDouble()) == round(0.138)) {
            val thePlayer = mc.thePlayer!!

            thePlayer.motionY -= 0.08
            event.y = event.y - 0.09316090325960147
            thePlayer.posY -= 0.09316090325960147
        }
        if (level == 1 && (mc.thePlayer!!.moveForward != 0.0f || mc.thePlayer!!.moveStrafing != 0.0f)) {
            level = 2
            moveSpeed = 1.35 * baseMoveSpeed - 0.01
        } else if (level == 2) {
            level = 3
            mc.thePlayer!!.motionY = 0.399399995803833
            event.y = 0.399399995803833
            moveSpeed *= 2.149
        } else if (level == 3) {
            level = 4
            val difference = 0.66 * (lastDist - baseMoveSpeed)
            moveSpeed = lastDist - difference
        } else {
            if (mc.theWorld!!.getCollidingBoundingBoxes(mc.thePlayer!!, mc.thePlayer!!.entityBoundingBox.offset(0.0, mc.thePlayer!!.motionY, 0.0)).isNotEmpty() || mc.thePlayer!!.isCollidedVertically) level = 1
            moveSpeed = lastDist - lastDist / 159.0
        }
        moveSpeed = max(moveSpeed, baseMoveSpeed)
        val movementInput = mc.thePlayer!!.movementInput
        var forward: Float = movementInput.moveForward
        var strafe: Float = movementInput.moveStrafe
        var yaw = mc.thePlayer!!.rotationYaw
        if (forward == 0.0f && strafe == 0.0f) {
            event.x = 0.0
            event.z = 0.0
        } else if (forward != 0.0f) {
            if (strafe >= 1.0f) {
                yaw += (if (forward > 0.0f) -45 else 45).toFloat()
                strafe = 0.0f
            } else if (strafe <= -1.0f) {
                yaw += (if (forward > 0.0f) 45 else -45).toFloat()
                strafe = 0.0f
            }
            if (forward > 0.0f) {
                forward = 1.0f
            } else if (forward < 0.0f) {
                forward = -1.0f
            }
        }
        val mx2 = cos(Math.toRadians(yaw + 90.0f.toDouble()))
        val mz2 = sin(Math.toRadians(yaw + 90.0f.toDouble()))
        event.x = forward.toDouble() * moveSpeed * mx2 + strafe.toDouble() * moveSpeed * mz2
        event.z = forward.toDouble() * moveSpeed * mz2 - strafe.toDouble() * moveSpeed * mx2
        mc.thePlayer!!.stepHeight = 0.6f
        if (forward == 0.0f && strafe == 0.0f) {
            event.x = 0.0
            event.z = 0.0
        }
    }

    private val baseMoveSpeed: Double
        get() {
            var baseSpeed = 0.2873
            if (mc.thePlayer!!.isPotionActive(Potion.moveSpeed)) baseSpeed *= 1.0 + 0.2 * (mc.thePlayer!!.getActivePotionEffect(
                Potion.moveSpeed))!!.amplifier + 1
            return baseSpeed
        }

    private fun round(value: Double): Double {
        var bigDecimal = BigDecimal(value)
        bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP)
        return bigDecimal.toDouble()
    }
}