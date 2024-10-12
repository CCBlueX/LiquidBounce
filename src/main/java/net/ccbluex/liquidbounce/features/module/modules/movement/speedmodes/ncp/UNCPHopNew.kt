/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.potion.Potion

/*
* Working on UNCP/NCP & Verus b3896/b3901
* Tested on: eu.loyisa.cn, anticheat-test.com
* Credit: @larryngton
* https://github.com/CCBlueX/LiquidBounce/pull/3798/files
*/
object UNCPHopNew : SpeedMode("UNCPHopNew") {

    private var airTick = 0

    private const val SPEED_VALUE = 0.199999999
    private const val BOOST_MULTIPLIER = 0.00718
    private const val DAMAGE_BOOST_SPEED = 0.5F

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        if (player.fallDistance > 2) {
            if (mc.timer.timerSpeed > 1f) mc.timer.timerSpeed = 1f
            return
        }

        if (!isMoving || player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return

        if (player.onGround) {
            if (Speed.lowHop) player.motionY = 0.4 else player.tryJump()
            airTick = 0
            return
        } else {
            if (player.hurtTime <= 1) {
                airTick++
                if (airTick == Speed.onTick) {
                    strafe()
                    player.motionY = -0.1523351824467155
                }
            }
        }

        if (Speed.onHurt && player.hurtTime in 2..4 && player.motionY >= 0) {
            player.motionY -= 0.1
        }

        if (player.onGround) {
            strafe(speed = MovementUtils.speed.coerceAtLeast(calculateSpeed(0.281).toFloat()))
        } else {
            if (Speed.airStrafe) {
                strafe(speed = MovementUtils.speed.coerceAtLeast(calculateSpeed(0.2).toFloat()), strength = 0.7)
            }
        }

        if (Speed.timerBoost) {
            if (player.hurtTime <= 1) {
                when (player.ticksExisted % 5) {
                    0 -> mc.timer.timerSpeed = 1.025F
                    2 -> mc.timer.timerSpeed = 1.08F
                    4 -> mc.timer.timerSpeed = 1F
                }
            } else if (player.hurtTime > 1) {
                mc.timer.timerSpeed = 1F
            }
        }

        if (Speed.shouldBoost) {
            player.motionX *= 1F + BOOST_MULTIPLIER
            player.motionZ *= 1F + BOOST_MULTIPLIER
        }

        if (Speed.damageBoost && player.hurtTime >= 1) {
            strafe(speed = MovementUtils.speed.coerceAtLeast(DAMAGE_BOOST_SPEED))
        }
    }

    private fun calculateSpeed(baseValue: Double): Double {
        val player = mc.thePlayer ?: return 0.0

        val speedAmplifier = player.getActivePotionEffect(Potion.moveSpeed)?.amplifier ?: 0
        return baseValue + SPEED_VALUE * speedAmplifier
    }

    override fun onDisable() {
        airTick = 0
    }
}