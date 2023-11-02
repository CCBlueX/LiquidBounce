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

object TickBase : Module("TickBase", ModuleCategory.COMBAT) {

    private var playerTicks = 0

    private val ticksValue by IntegerValue("Ticks", 10, 3..20)
    private val timerValue by FloatValue("Timer", 1f, 1f..50f)
    private val attackedValue by FloatValue("Attacked", 0.5f, 0.05f..1f)

    private val thePlayer = mc.thePlayer

    // Timer Reset
    private val resetTimer: Float
        get() {
            mc.timer.timerSpeed = 1f
            return 1f
        }

    @EventTarget
    fun onAttack(event: AttackEvent) {

        if (event.targetEntity is EntityLivingBase && playerTicks < 1) {

            playerTicks = ticksValue

        }
    }

    override fun onEnable() {
        resetTimer
    }

    override fun onDisable() {
        resetTimer
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (playerTicks > 0) {
            val tickProgress = 1.0 - (playerTicks.toDouble() / ticksValue.toDouble())

            val playerSpeed = when {
                tickProgress < 0.5 -> timerValue
                tickProgress < 0.75 -> attackedValue
                else -> 1f
            }

            setTimerSpeed(playerSpeed)
            playerTicks -= 1
        }
    }

    private fun setTimerSpeed(speed: Float) {
        mc.timer.timerSpeed = speed
    }
}