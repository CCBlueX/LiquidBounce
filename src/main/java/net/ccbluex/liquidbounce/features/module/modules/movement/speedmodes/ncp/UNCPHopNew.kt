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
    private val groundMin = 0.281 + SPEED_VALUE * (mc.thePlayer?.getActivePotionEffect(Potion.moveSpeed)?.amplifier ?: 0)
    private val airMin = 0.2 + SPEED_VALUE * (mc.thePlayer?.getActivePotionEffect(Potion.moveSpeed)?.amplifier ?: 0)

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (!isMoving || player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return

        if (player.onGround) {
            if (Speed.lowHop) player.motionY = 0.4 else player.tryJump()
            airTick = 0
            return
        } else {
            airTick++
            if (airTick == Speed.onTick) {
                strafe()
                player.motionY = -0.1523351824467155
            }
        }

        if (Speed.onHurt && player.hurtTime >= 5 && player.motionY >= 0) {
            player.motionY -= 0.1
        }

        if (player.onGround) {
            strafe(speed = MovementUtils.speed.coerceAtLeast(groundMin.toFloat()))
        } else {
            if (Speed.airStrafe) {
                strafe(speed = MovementUtils.speed.coerceAtLeast(airMin.toFloat()), strength = 0.7)
            }
        }

        if (Speed.timerBoost && player.hurtTime <= 1) {
            if (player.onGround) {
                mc.timer.timerSpeed = 1.5F
            } else {
                mc.timer.timerSpeed = 1.08F
            }

            if (player.motionY <= 0) {
                mc.timer.timerSpeed = 1.1F
            }
        } else {
            if (Speed.timerBoost) {
                mc.timer.timerSpeed = 1.08F
            }
        }

        if (Speed.shouldBoost) {
            player.motionX *= 1F + 0.00718
            player.motionZ *= 1F + 0.00718
        }

        if (player.hurtTime >= 1 && Speed.damageBoost) {
            strafe(speed = MovementUtils.speed.coerceAtLeast(0.5f))
        }
    }

    override fun onDisable() {
        airTick = 0
    }
}
