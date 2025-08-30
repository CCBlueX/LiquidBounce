/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes

import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * ElytraFly boost mode
 */
@Suppress("MagicNumber")
internal object ElytraFlyModeBoost : ElytraFlyMode("Boost") {

    // ===================== Configuration Settings =====================
    
    // General flight settings
    private val boostSpeed by float("Speed", 0.9f, 0.5f..2.0f)
    private val acceleration by float("Acceleration", 0.01f, 0.005f..0.05f)
    private val autoBoost by boolean("AutoBoost", true)
    private val verticalControl by float("VerticalControl", 0.8f, 0.2f..1.0f)
    
    // Ground proximity settings
    private val smartGround by boolean("SmartGroundBehavior", true)
    private val groundDistance by float("GroundDistance", 3.0f, 1.5f..7.0f)
    
    // Dive mechanics settings
    private val diveMechanics by boolean("DiveMechanics", true)
    private val diveAcceleration by float("DiveAcceleration", 0.05f, 0.01f..0.1f)
    private val diveEfficiency by float("DiveEfficiency", 0.8f, 0.4f..1.5f)
    
    // ===================== Runtime State =====================
    
    private var currentAcceleration = 0f
    private var currentDiveSpeed = 0f
    private var cachedGroundCheck = false
    private var groundCheckCooldown = 0
    
    // ===================== Physics Constants =====================
    
    private const val MAX_DIVE_SPEED_MULTIPLIER = 1.2f
    private const val DIVE_SPEED_REDUCTION = 0.01f
    private const val GRAVITY_COMPENSATION = 0.008
    private const val NATURAL_LIFT_FACTOR = 0.005
    private const val DECELERATION_FACTOR = 0.98f
    private const val MIN_ACCELERATION_THRESHOLD = 0.01f
    private const val PITCH_REDUCTION_FACTOR = 0.3
    private const val GROUND_PENALTY_FACTOR = 0.8
    private const val SPEED_EFFECT_MULTIPLIER = 0.1
    private const val NEAR_GROUND_HORIZONTAL_BOOST = 1.3
    private const val NEAR_GROUND_VERTICAL_REDUCTION = 0.3
    private const val DIVE_THRESHOLD_ANGLE = 15f
    private const val NEGATIVE_PITCH_THRESHOLD = -10f
    private const val MAX_PITCH_ANGLE = 90f
    private const val DIVE_BOOST_MULTIPLIER = 2.0

    // ===================== Module Lifecycle =====================

    override fun enable() = resetState()
    override fun disable() = resetState()
    
    private fun resetState() {
        currentAcceleration = 0f
        currentDiveSpeed = 0f
        cachedGroundCheck = false
        groundCheckCooldown = 0
    }
    
    // ===================== Ground Detection =====================
    
    private fun isNearGround(): Boolean {
        if (!smartGround) return false
        
        if (--groundCheckCooldown <= 0) {
            cachedGroundCheck = world.getBlockCollisions(
                player, 
                player.boundingBox.offset(0.0, -groundDistance.toDouble(), 0.0)
            ).iterator().hasNext()
            groundCheckCooldown = 3
        }
        
        return cachedGroundCheck
    }
    
    // ===================== Main Flight Logic =====================
    
    override fun onTick() {
        if (!player.isGliding) {
            resetState()
            return
        }

        val isNearGround = isNearGround()
        val divePullUpBoost = handleDiveMechanics()
        
        val shouldBoost = mc.options.jumpKey.isPressed || 
            (autoBoost && player.pitch < NEGATIVE_PITCH_THRESHOLD && !isNearGround) ||
            divePullUpBoost > 0
            
        handleAcceleration(shouldBoost)
        
        if (player.moving) {
            player.velocity = player.velocity.withStrafe(
                speed = calculateEffectiveSpeed(isNearGround)
            )
        }
    }
    
    // ===================== Flight Physics =====================

    private fun handleDiveMechanics(): Float {
        if (!diveMechanics) {
            currentDiveSpeed = max(0f, currentDiveSpeed - DIVE_SPEED_REDUCTION)
            return 0f
        }
        
        val oldDiveSpeed = currentDiveSpeed
        
        if (player.pitch > DIVE_THRESHOLD_ANGLE) {
            val diveFactor = min(player.pitch / MAX_PITCH_ANGLE, 1f)
            currentDiveSpeed = min(
                currentDiveSpeed + diveAcceleration * diveFactor,
                MAX_DIVE_SPEED_MULTIPLIER
            )
            return 0f
        }
        
        currentDiveSpeed = max(0f, currentDiveSpeed - DIVE_SPEED_REDUCTION)
        
        return if (player.pitch < 0 && oldDiveSpeed > 0) {
            oldDiveSpeed * (-player.pitch / MAX_PITCH_ANGLE) * diveEfficiency
        } else {
            0f
        }
    }
    
    private fun handleAcceleration(shouldBoost: Boolean) {
        when {
            shouldBoost && currentAcceleration < boostSpeed -> {
                currentAcceleration = min(
                    currentAcceleration + acceleration * (1f - currentAcceleration / boostSpeed),
                    boostSpeed
                )
            }
            !shouldBoost && currentAcceleration > 0 -> {
                currentAcceleration = (currentAcceleration * (DECELERATION_FACTOR - acceleration))
                    .takeIf { it >= MIN_ACCELERATION_THRESHOLD } ?: 0f
            }
        }
    }
    
    private fun calculateEffectiveSpeed(isNearGround: Boolean): Double {
        val baseSpeed = ModuleElytraFly.Speed.horizontal.toDouble()
        
        val pitchReduction = if (player.pitch < 0) {
            abs(player.pitch / MAX_PITCH_ANGLE) * PITCH_REDUCTION_FACTOR
        } else {
            0.0
        }
        
        val speedBonus = currentDiveSpeed.toDouble() + 
            (player.getStatusEffect(StatusEffects.SPEED)?.let { (it.amplifier + 1) * SPEED_EFFECT_MULTIPLIER } ?: 0.0)
        
        val groundMultiplier = if (isNearGround) {
            GROUND_PENALTY_FACTOR
        } else {
            1.0
        }
        
        return baseSpeed * (1.0 - pitchReduction + speedBonus) * groundMultiplier
    }
    
    // ===================== Movement Event Handler =====================
    
    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        if (ModuleElytraFly.shouldNotOperate() || !player.isGliding) return@handler
        
        val nearGround = isNearGround()
        val divePullUpBoost = if (player.pitch < 0 && currentDiveSpeed > 0) {
            (-player.pitch / MAX_PITCH_ANGLE) * diveEfficiency * currentDiveSpeed * SPEED_EFFECT_MULTIPLIER
        } else {
            0.0
        }
        
        if (currentAcceleration > 0 || currentDiveSpeed > 0) {
            event.movement = event.movement.add(calculateBoostVector(nearGround, divePullUpBoost))
        }
        
        event.movement = event.movement.copy(y = calculateVerticalMovement(event.movement.y, divePullUpBoost, event))
    }
    
    // ===================== Helper Methods =====================
    
    private fun calculateBoostVector(nearGround: Boolean, divePullUpBoost: Double): Vec3d {
        val lookVec = player.getRotationVector()
        val boostFactor = currentAcceleration.toDouble() + 
            if (player.pitch > 0) {
                currentDiveSpeed.toDouble()
            } else {
                divePullUpBoost * DIVE_BOOST_MULTIPLIER
            }
        
        return if (nearGround) {
            Vec3d(
                lookVec.x * NEAR_GROUND_HORIZONTAL_BOOST, 
                lookVec.y * NEAR_GROUND_VERTICAL_REDUCTION, 
                lookVec.z * NEAR_GROUND_HORIZONTAL_BOOST
            ).normalize().multiply(boostFactor)
        } else {
            lookVec.multiply(boostFactor)
        }
    }
    
    private fun calculateVerticalMovement(currentY: Double, divePullUpBoost: Double, event: PlayerMoveEvent): Double {
        val horizontalSpeed = kotlin.math.sqrt(
            event.movement.x * event.movement.x + event.movement.z * event.movement.z
        )
        val naturalLift = horizontalSpeed * NATURAL_LIFT_FACTOR
        val verticalSpeed = ModuleElytraFly.Speed.vertical.toDouble() * verticalControl
        
        val baseY = currentY - GRAVITY_COMPENSATION + naturalLift + divePullUpBoost
        
        return when {
            mc.options.jumpKey.isPressed && !mc.options.sneakKey.isPressed -> currentY + verticalSpeed + divePullUpBoost
            mc.options.sneakKey.isPressed && !mc.options.jumpKey.isPressed -> currentY - verticalSpeed
            else -> baseY
        }
    }
} 
