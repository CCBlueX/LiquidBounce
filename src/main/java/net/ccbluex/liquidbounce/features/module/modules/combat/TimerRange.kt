/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.EntityLivingBase

object TimerRange : Module("TimerRange", ModuleCategory.COMBAT) {

    private var playerTicks = 0

    private val rangeValue by FloatValue("Range", 3.5f, 1f..5f)
    private val ticksValue by IntegerValue("Ticks", 10, 1..20)
    private val timerValue by FloatValue("Timer", 1f, 0.01f..40f)
    private val chargedValue by FloatValue("Charged", 0.5f, 0.05f..1f)

    private fun timerReset() {
        mc.timer.timerSpeed = 1f
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase && playerTicks < 1) {
            val targetEntity = event.targetEntity
            val entityDistance = mc.thePlayer.getDistanceToEntityBox(targetEntity)

            if (entityDistance <= rangeValue) {
                playerTicks = ticksValue

                val prevpos = mc.thePlayer.prevPosY
                var currentpos = mc.thePlayer.posY

                currentpos = prevpos

                mc.timer.timerSpeed = 0f
            } else {
                timerReset()
            }
        }
    }

    override fun onEnable() {
        timerReset()
    }

    override fun onDisable() {
        timerReset()
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
                mc.timer.timerSpeed = playerSpeed
            } else {
                val timerTicks = ticksValue - playerTicks
                if (timerTicks > 0) {
                    mc.timer.timerSpeed = (1f + timerTicks)
                } else {
                    mc.timer.timerSpeed = 0f
                }
            }

            playerTicks--
        }
    }
}