/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.EntityLivingBase
import kotlin.math.cos
import kotlin.math.sin


object TimerRange : Module("TimerRange", ModuleCategory.COMBAT) {

    private var playerTicks = 0
    private var tickBalance = 0f

    private val ticksValue by IntegerValue("Ticks", 10, 1..20)
    private val timerValue by FloatValue("Timer", 1f, 1f..40f)
    private val chargedValue by FloatValue("Charged", 0.5f, 0.05f..1f)

    private val thePlayer = mc.thePlayer

    // Timer Reset
    private fun resetTimer() {
        mc.timer.timerSpeed = 1f
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {

        if (event.targetEntity is EntityLivingBase && playerTicks < 1) {

            playerTicks = ticksValue

        }
    }

    override fun onEnable() {
        resetTimer()
    }

    override fun onDisable() {
        resetTimer()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (playerTicks > 0) {
            val tickProgress = 1.0 - (playerTicks.toDouble() / ticksValue.toDouble())

            val playerSpeed = when {
                tickProgress < 0.5 -> timerValue
                tickProgress < 0.75 -> chargedValue
                else -> 1f
            }

            if (playerSpeed >= 0) {
                mc.timer.timerSpeed = playerSpeed + tickBalance
                tickBalance = 0f
            } else {
                tickBalance += playerSpeed
                mc.timer.timerSpeed = 0f
            }

            playerTicks--

            if (!thePlayer.isEntityAlive) {
                return
            }

            val onGround = thePlayer.onGround
            val isTimerValue = playerSpeed == timerValue
            val moveSpeed =
                if (isTimerValue && onGround) 0.5 else if (isTimerValue) 0.2 else if (playerSpeed == chargedValue) 0.1 else 0.15
            val strafeSpeed = if (isTimerValue) 0.3 else 0.1

            val yawRadians = Math.toRadians(thePlayer.rotationYaw.toDouble())
            val forwardMotion = -sin(yawRadians) * moveSpeed
            val sidewaysMotion = cos(yawRadians) * moveSpeed

            if (thePlayer.movementInput.moveStrafe != 0f) {
                val strafeYaw = yawRadians + if (thePlayer.movementInput.moveStrafe < 0) Math.PI / 4 else -Math.PI / 4
                val strafeForwardMotion = cos(strafeYaw) * strafeSpeed
                val strafeSidewaysMotion = sin(strafeYaw) * strafeSpeed

                thePlayer.motionX = forwardMotion + strafeForwardMotion
                thePlayer.motionZ = sidewaysMotion + strafeSidewaysMotion
            } else {
                thePlayer.motionX = forwardMotion
                thePlayer.motionZ = sidewaysMotion
            }
        }
    }
}