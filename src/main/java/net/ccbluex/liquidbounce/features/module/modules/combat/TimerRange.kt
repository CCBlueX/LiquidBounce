/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import java.awt.Color
import kotlin.random.Random

object TimerRange : Module("TimerRange", ModuleCategory.COMBAT) {

    private var playerTicks = 0
    private var smartTick = 0
    private var cooldownTick = 0

    // Condition to confirm
    private var confirmTick = false
    private var confirmMove = false

    // Condition to prevent getting timer speed stuck
    private var confirmAttack = false

    // Condition to makesure timer isn't reset on lagback, when not attacking
    private var confirmLagBack = false

    // Condition to makesure timer isn't reset on knockback, when timer isn't changed
    private var confirmKnockback = false

    private val timerBoostMode by ListValue("TimerMode", arrayOf("Normal", "Smart", "SmartMove"), "Normal")

    private val ticksValue by IntegerValue("Ticks", 10, 1..20)
    private val timerBoostValue by FloatValue("TimerBoost", 1.5f, 0.01f..35f)
    private val timerChargedValue by FloatValue("TimerCharged", 0.45f, 0.05f..5f)

    // Normal Mode Settings
    private val rangeValue by FloatValue("Range", 3.5f, 1f..5f) { timerBoostMode == "Normal" }
    private val cooldownTickValue by IntegerValue("CooldownTick", 10, 1..50) { timerBoostMode == "Normal" }

    // Smart & SmartMove Mode Settings
    private val minRange by FloatValue("MinRange", 1f, 1f..5f) { timerBoostMode != "Normal" }
    private val maxRange by FloatValue("MaxRange", 5f, 1f..5f) { timerBoostMode != "Normal" }

    private val minTickDelay: IntegerValue = object : IntegerValue("MinTickDelay", 50, 1..500) {
        override fun isSupported() = timerBoostMode != "Normal"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxTickDelay.get())
    }

    private val maxTickDelay: IntegerValue = object : IntegerValue("MaxTickDelay", 100, 1..500) {
        override fun isSupported() = timerBoostMode != "Normal"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minTickDelay.get())
    }

    private val lookThreshold by FloatValue("LookThreshold", 1.0f, 0.1f..1.5f) { timerBoostMode == "SmartMove" }

    // Mark Option
    private val markMode by ListValue("Mark", arrayOf("Off", "Box", "Platform"), "Off") { timerBoostMode == "SmartMove" }
    private val outline by BoolValue("Outline", false) { timerBoostMode == "SmartMove" && markMode == "Box" }

    // Optional
    private val resetOnlagBack by BoolValue("ResetOnLagback", false)
    private val resetOnKnockback by BoolValue("ResetOnKnockback", false)
    private val chatDebug by BoolValue("ChatDebug", true) { resetOnlagBack || resetOnKnockback }
    private val notificationDebug by BoolValue("NotificationDebug", false) { resetOnlagBack || resetOnKnockback }

    private fun timerReset() {
        mc.timer.timerSpeed = 1f
    }

    override fun onEnable() {
        timerReset()
    }

    override fun onDisable() {
        timerReset()
        smartTick = 0
        cooldownTick = 0
        playerTicks = 0
    }

    /**
     * Attack event
     */
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
        val randomTickDelay = Random.nextInt(minTickDelay.get(), maxTickDelay.get())
        val randomRange = Random.nextDouble(minRange.toDouble(), maxRange.toDouble())

        smartTick++
        cooldownTick++

        val shouldSlowed = when (timerBoostMode) {
            "Normal" -> cooldownTick >= cooldownTickValue && entityDistance <= rangeValue
            "Smart" -> smartTick >= randomTickDelay && entityDistance <= randomRange
            else -> false
        }

        if (shouldSlowed && confirmAttack) {
            confirmAttack = false
            playerTicks = ticksValue

            if (resetOnKnockback) {
                confirmKnockback = true
            }
            if (resetOnlagBack) {
                confirmLagBack = true
            }
            cooldownTick = 0
            smartTick = 0
        } else {
            timerReset()
        }
    }

    /**
     * Move event (SmartMove)
     */
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (timerBoostMode != "SmartMove") {
            return
        }

        val randomTickDelay = Random.nextInt(minTickDelay.get(), maxTickDelay.get())
        val randomRange = Random.nextDouble(minRange.toDouble(), maxRange.toDouble())

        if (isPlayerMoving()) {
            smartTick++

            if (smartTick >= randomTickDelay) {
                confirmTick = true
                smartTick = 0
            }
        } else {
            smartTick = 0
            confirmMove = false
        }

        val nearbyEntity = getNearestEntityInRange()

        if (nearbyEntity != null && isPlayerMoving()) {
            if (isLookingOnEntities(nearbyEntity, lookThreshold.toDouble())) {
                val entityDistance = mc.thePlayer.getDistanceToEntityBox(nearbyEntity)

                if (confirmTick && entityDistance <= randomRange) {
                    playerTicks = ticksValue
                    confirmTick = false
                    confirmMove = true

                    if (resetOnKnockback) {
                        confirmKnockback = true
                    }
                    if (resetOnlagBack) {
                        confirmLagBack = true
                    }
                }
            }
        }
    }

    /**
     * Update event
     */
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
     * Render event (Mark)
     */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (timerBoostMode.lowercase() == "smartmove") {
            getNearestEntityInRange()?.let { nearbyEntity ->
                val entityDistance = mc.thePlayer.getDistanceToEntityBox(nearbyEntity)
                if (entityDistance <= maxRange && isLookingOnEntities(nearbyEntity, lookThreshold.toDouble())) {
                    if (markMode == "Box") {
                        drawEntityBox(nearbyEntity, Color(37, 126, 255, 70), outline)
                    } else if (markMode != "Off") {
                        drawPlatform(nearbyEntity, Color(37, 126, 255, 70))
                    }
                } else if (entityDistance <= maxRange) {
                    if (markMode == "Box") {
                        drawEntityBox(nearbyEntity, Color(210, 60, 60, 70), outline)
                    } else if (markMode != "Off") {
                        drawPlatform(nearbyEntity, Color(210, 60, 60, 70))
                    }
                }
            }
        }
    }

    /**
     * Check if player is moving
     */
    private fun isPlayerMoving(): Boolean {
        return mc.thePlayer.moveForward != 0f || mc.thePlayer.moveStrafing != 0f
    }

    /**
     * Get all entities in the world.
     */
    private fun getAllEntities(): List<Entity> {
        return mc.theWorld.loadedEntityList
            .filter { EntityUtils.isSelected(it, true) }
            .toList()
    }

    /**
     * Find the nearest entity in range.
     */
    private fun getNearestEntityInRange(): Entity? {
        val player = mc.thePlayer

        val entitiesInRange = getAllEntities()
            .filter { player.getDistanceToEntityBox(it) <= rangeValue }

        return entitiesInRange.minByOrNull { player.getDistanceToEntityBox(it) }
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
     * Lagback Reset is Inspired from Nextgen TimerRange
     * Reset Timer on Lagback & Knockback.
     */
    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (isPlayerMoving() && !shouldResetTimer()
            && mc.timer.timerSpeed > 1.0 || mc.timer.timerSpeed < 1.0 ) {

            // Check for lagback
            if (resetOnlagBack && confirmLagBack) {
                if (packet is S08PacketPlayerPosLook) {
                    confirmLagBack = false
                    timerReset()
                    if (chatDebug) {
                        Chat.print("Lagback Received | Timer Reset")
                    }
                    if (notificationDebug) {
                        hud.addNotification(Notification("Lagback Received | Timer Reset", 1000F))
                    }
                }
            }

            // Check for knockback
            if (resetOnKnockback && confirmKnockback) {
                if (packet is S12PacketEntityVelocity && mc.thePlayer.entityId == packet.entityID
                    && packet.motionY > 0 && (packet.motionX.toDouble() != 0.0 || packet.motionZ.toDouble() != 0.0)) {
                    confirmKnockback = false
                    timerReset()
                    if (chatDebug) {
                        Chat.print("Knockback Received | Timer Reset")
                    }
                    if (notificationDebug) {
                        hud.addNotification(Notification("Knockback Received | Timer Reset", 1000F))
                    }
                }
            }
        }
    }

    /**
     * HUD Tag
     */
    override val tag
        get() = timerBoostMode
}