/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import kotlin.random.Random

object TimerRange : Module("TimerRange", ModuleCategory.COMBAT) {

    private var playerTicks = 0
    private var smartTick = 0
    private var cooldownTick = 0
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

    private val lookThreshold by FloatValue("LookThreshold", 0.5f, 0.1f..1f) { timerBoostMode == "SmartMove" }

    // Optional
    private val resetOnlagBack by BoolValue("ResetOnLagback", false)
    private val resetOnKnockback by BoolValue("ResetOnKnockback", false)
    private val chatDebug by BoolValue("ChatDebug", true) { resetOnlagBack || resetOnKnockback }

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
            if (isLookingTowardsEntities(nearbyEntity)) {
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
     * This check is useful to prevent player from changing speed while looking away
     * And prevent player from changing speed toward unintended entity/target while moving.
     */
    private fun isLookingTowardsEntities(entity: EntityLivingBase): Boolean {
        val lookVec = mc.thePlayer.lookVec.normalize()
        val playerPos = mc.thePlayer.positionVector.addVector(0.0, mc.thePlayer.eyeHeight.toDouble(), 0.0)
        val entityPos = entity.positionVector.addVector(0.0, entity.eyeHeight.toDouble(), 0.0)

        val directionToEntity = entityPos.subtract(playerPos).normalize()
        val dotProductThreshold = lookVec.dotProduct(directionToEntity)

        // Player needs to be facing the entity/target with chosen dotproduct, in this case default threshold of at least 0.5
        return dotProductThreshold > lookThreshold.toDouble()
    }

    /**
     * Check if player is moving
     */
    private fun isPlayerMoving(): Boolean {
        return mc.thePlayer.moveForward != 0f || mc.thePlayer.moveStrafing != 0f
    }

    /**
     * Get all living entities in the world.
     */
    private fun getAllLivingEntities(): List<EntityLivingBase> {
        return mc.theWorld.loadedEntityList.filterIsInstance<EntityLivingBase>()
            .filter { EntityUtils.isSelected(it, true) }
            .toList()
    }

    /**
     * Find the nearest living entity in range.
     */
    private fun getNearestEntityInRange(): EntityLivingBase? {
        val player = mc.thePlayer

        val entitiesInRange = getAllLivingEntities()
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