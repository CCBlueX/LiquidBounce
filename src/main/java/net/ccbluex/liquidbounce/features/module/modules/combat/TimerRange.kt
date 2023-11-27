/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.value.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import kotlin.random.Random

object TimerRange : Module("TimerRange", ModuleCategory.COMBAT) {

    private var playerTicks = 0
    private var smartCounter = 0

    // Condition to prevent getting timer speed stuck
    private var confirmAttack = false

    // Condition to makesure timer isn't reset on lagback, when not attacking
    private var confirmLagBack = false

    private val timerBoostMode by ListValue(
        "TimerMode", arrayOf("Normal", "Smart"),
        "Normal"
    )

    private val ticksValue by IntegerValue("Ticks", 10, 1..20)
    private val timerBoostValue by FloatValue("TimerBoost", 1.5f, 0.01f..35f)
    private val timerChargedValue by FloatValue("TimerCharged", 0.45f, 0.05f..5f)

    private val rangeValue by FloatValue("Range", 3.5f, 1f..5f) { timerBoostMode != "Smart" }

    private val minRange by FloatValue("MinRange", 1f, 1f..5f) { timerBoostMode == "Smart" }
    private val maxRange by FloatValue("MaxRange", 5f, 1f..5f) { timerBoostMode == "Smart" }

    private val minTickDelay by IntegerValue("MinTickDelay", 5, 1..100) { timerBoostMode == "Smart" }
    private val maxTickDelay by IntegerValue("MaxTickDelay", 100, 1..100) { timerBoostMode == "Smart" }

    private val resetlagBack by BoolValue("ResetOnLagback", false)

    private fun timerReset() {
        mc.timer.timerSpeed = 1f
    }

    override fun onEnable() {
        timerReset()
    }

    override fun onDisable() {
        timerReset()
        smartCounter = 0
        playerTicks = 0
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity !is EntityLivingBase || shouldResetTimer()) {
            timerReset()
            return
        } else {
            confirmAttack = true
        }

        val targetEntity = event.targetEntity
        val entityDistance = mc.thePlayer.getDistanceToEntityBox(targetEntity)
        val randomCounter = Random.nextInt(minTickDelay, maxTickDelay)
        val randomRange = Random.nextDouble(minRange.toDouble(), maxRange.toDouble())

        smartCounter++

        val shouldSlowed = when (timerBoostMode) {
            "Normal" -> entityDistance <= rangeValue
            "Smart" -> smartCounter >= randomCounter && entityDistance <= randomRange
            else -> false
        }

        if (shouldSlowed && confirmAttack) {
            confirmAttack = false
            playerTicks = ticksValue
            if (resetlagBack) { confirmLagBack = true }
            smartCounter = 0
        } else {
            timerReset()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Randomize the timer & charged delay a bit, to bypass some AntiCheat
        val timerboost = Random.nextDouble(0.5, 0.56)
        val charged = Random.nextDouble(0.75, 0.91)

        if (playerTicks <= 0) {
            timerReset()
            return
        }

        val tickProgress = playerTicks.toDouble() / ticksValue.toDouble()
        val playerSpeed = when {
            tickProgress < timerboost -> timerBoostValue
            tickProgress < charged -> timerChargedValue
            else -> 1f
        }

        val speedAdjustment = if (playerSpeed >= 0) playerSpeed else 1f + ticksValue - playerTicks
        val adjustedTimerSpeed = maxOf(speedAdjustment, 0f)

        mc.timer.timerSpeed = adjustedTimerSpeed

        playerTicks--
    }

    /**
     * Separate condition to make it cleaner
     */
    private fun shouldResetTimer(): Boolean {
        return (playerTicks >= 1
                || mc.thePlayer.isSpectator || mc.thePlayer.isDead
                || mc.thePlayer.isInWater || mc.thePlayer.isInLava
                || mc.thePlayer.isInWeb || mc.thePlayer.isOnLadder
                || mc.thePlayer.isRiding)
    }

    /**
     * Inspired from Nextgen TimerRange
     * Reset Timer on Lagback.
     */
    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is S08PacketPlayerPosLook
            && resetlagBack && confirmLagBack && !shouldResetTimer()) {
            confirmLagBack = false
            timerReset()
            Chat.print("Lagback Detected | Timer Reset")
        }
    }

    /**
     * HUD Tag
     */
    override val tag
        get() = timerBoostMode
}