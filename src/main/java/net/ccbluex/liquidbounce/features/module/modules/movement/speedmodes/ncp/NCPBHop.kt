/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.potion.Potion
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

object NCPBHop : SpeedMode("NCPBHop") {
    private var level = 1
    private var moveSpeed = 0.2873
    private var lastDist = 0.0
    private var timerDelay = 0
    override fun onEnable() {
        mc.ticker.timerSpeed = 1f
        level = if (mc.world.getCollidingBoundingBoxes(mc.player, mc.player.entityBoundingBox.offset(0.0, mc.player.velocityY, 0.0)).size > 0 || mc.player.isCollidedVertically) 1 else 4
    }

    override fun onDisable() {
        mc.ticker.timerSpeed = 1f
        moveSpeed = baseMoveSpeed
        level = 0
    }

    override fun onMotion() {
        val xDist = mc.player.x - mc.player.prevPosX
        val zDist = mc.player.z - mc.player.prevZ
        lastDist = sqrt(xDist * xDist + zDist * zDist)
    }


    //TODO: Recode this mess
    override fun onMove(event: MoveEvent) {
        ++timerDelay
        timerDelay %= 5
        if (timerDelay != 0) {
            mc.ticker.timerSpeed = 1f
        } else {
            if (isMoving) mc.ticker.timerSpeed = 32767f // What?
            if (isMoving) {
                mc.ticker.timerSpeed = 1.3f
                mc.player.velocityX *= 1.0199999809265137
                mc.player.velocityZ *= 1.0199999809265137
            }
        }
        if (mc.player.onGround && isMoving) level = 2
        if (round(mc.player.z - mc.player.z.toInt().toDouble()) == round(0.138)) {
            val thePlayer = mc.player

            thePlayer.velocityY -= 0.08
            event.y -= 0.09316090325960147
            theplayer.z -= 0.09316090325960147
        }
        if (level == 1 && isMoving) {
            level = 2
            moveSpeed = 1.35 * baseMoveSpeed - 0.01
        } else if (level == 2) {
            level = 3
            mc.player.velocityY = 0.399399995803833
            event.y = 0.399399995803833
            moveSpeed *= 2.149
        } else if (level == 3) {
            level = 4
            val difference = 0.66 * (lastDist - baseMoveSpeed)
            moveSpeed = lastDist - difference
        } else {
            if (mc.world.getCollidingBoundingBoxes(mc.player, mc.player.entityBoundingBox.offset(0.0, mc.player.velocityY, 0.0)).isNotEmpty() || mc.player.isCollidedVertically) level = 1
            moveSpeed = lastDist - lastDist / 159.0
        }
        moveSpeed = max(moveSpeed, baseMoveSpeed)
        val movementInput = mc.player.movementInput
        var forward = movementInput.moveForward
        var strafe = movementInput.moveStrafe
        var yaw = mc.player.yaw
        if (forward == 0f && strafe == 0f) {
            event.zeroXZ()
        } else if (forward != 0f) {
            if (strafe >= 1f) {
                yaw += (if (forward > 0f) -45 else 45).toFloat()
                strafe = 0f
            } else if (strafe <= -1f) {
                yaw += (if (forward > 0f) 45 else -45).toFloat()
                strafe = 0f
            }
            if (forward > 0f) {
                forward = 1f
            } else if (forward < 0f) {
                forward = -1f
            }
        }
        val mx2 = cos((yaw + 90.0).toRadians())
        val mz2 = sin((yaw + 90.0).toRadians())
        event.x = forward.toDouble() * moveSpeed * mx2 + strafe.toDouble() * moveSpeed * mz2
        event.z = forward.toDouble() * moveSpeed * mz2 - strafe.toDouble() * moveSpeed * mx2
        mc.player.stepHeight = 0.6f

        if (!isMoving) event.zeroXZ()
    }

    private val baseMoveSpeed: Double
        get() {
            var baseSpeed = 0.2873
            if (mc.player.isPotionActive(Potion.moveSpeed)) baseSpeed *= 1.0 + 0.2 * (mc.player.getActivePotionEffect(
                Potion.moveSpeed)).amplifier + 1
            return baseSpeed
        }

    private fun round(value: Double): Double {
        var bigDecimal = BigDecimal(value)
        bigDecimal = bigDecimal.setScale(3, RoundingMode.HALF_UP)
        return bigDecimal.toDouble()
    }
}