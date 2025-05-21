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
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ElytraFly boost mode
 *
 * Allows to fly with elytra without fireworks by simulating natural flight mechanics
 */
internal object ElytraFlyModeBoost : ElytraFlyMode("Boost") {

    /**
     * Helper extension function to replace Y coordinate in Vec3d
     */
    private fun Vec3d.withY(y: Double): Vec3d = Vec3d(this.x, y, this.z)

    /**
     * Base boost speed 
     */
    private val boostSpeed by float("Speed", 0.9f, 0.5f..2.0f)
    
    /**
     * How much acceleration is applied for smoother flight
     */
    private val acceleration by float("Acceleration", 0.01f, 0.005f..0.05f)
    
    /**
     * Enable auto-boost when looking up
     */
    private val autoBoost by boolean("AutoBoost", true)
    
    /**
     * Enable diving mechanics (looking down to gain speed, up to gain height)
     * This simulates vanilla elytra behavior but is more efficient
     */
    private val diveMechanics by boolean("DiveMechanics", true)
    
    /**
     * Smart ground behavior (automatically slows down near ground)
     */
    private val smartGround by boolean("SmartGroundBehavior", true)
    
    /**
     * How far from ground smart behavior activates
     */
    private val groundDistance by float("GroundDistance", 3.0f, 1.5f..7.0f)
    
    /**
     * Vertical flight control multiplier
     */
    private val verticalControl by float("VerticalControl", 0.8f, 0.2f..1.0f)
    
    /**
     * Current acceleration value (used for smooth acceleration)
     */
    private var currentAcceleration = 0.0f
    
    /**
     * Current speed gained from diving
     */
    private var currentDiveSpeed = 0.0f
    
    /**
     * Advanced Settings
     */
    
    /**
     * How much acceleration you gain when diving (looking down)
     */
    private val diveAcceleration by float("DiveAcceleration", 0.05f, 0.01f..0.1f)
    
    /**
     * How much diving speed affects flight
     */
    private val diveEfficiency by float("DiveEfficiency", 0.8f, 0.4f..1.5f)

    override fun enable() {
        currentAcceleration = 0.0f
        currentDiveSpeed = 0.0f
    }

    override fun disable() {
        currentAcceleration = 0.0f
        currentDiveSpeed = 0.0f
    }
    
    /**
     * Check if player is near ground
     */
    private fun isNearGround(): Boolean = 
        smartGround && 
        world.getBlockCollisions(
            player, 
            player.boundingBox.offset(0.0, -groundDistance.toDouble(), 0.0)
        ).iterator().hasNext()
    
    override fun onTick() {
        if (!player.isGliding) {
            currentAcceleration = 0.0f
            currentDiveSpeed = 0.0f
            return
        }

        // Check for ground proximity
        val isNearGround = isNearGround()

        // Handle diving mechanics
        val divePullUpBoost = handleDiving()
        
        // Check if boost should be applied
        val shouldBoost = mc.options.jumpKey.isPressed || 
            (autoBoost && player.pitch < -10f && !isNearGround) ||
            divePullUpBoost > 0
            
        // Apply acceleration logic
        handleAcceleration(shouldBoost)
        
        // Apply horizontal speed
        if (player.moving) {
            player.velocity = player.velocity.withStrafe(
                speed = calculateEffectiveSpeed(isNearGround)
            )
        }
    }
    
    /**
     * Handle diving mechanics and return current pull-up boost value
     */
    private fun handleDiving(): Float {
        if (!diveMechanics) {
            // Decay existing dive speed if diving is disabled
            if (currentDiveSpeed > 0) {
                currentDiveSpeed = max(0f, currentDiveSpeed - 0.01f)
            }
            return 0f
        }
        
        // Process diving (when looking down)
        if (player.pitch > 15f) {
            // Calculate dive factor based on pitch angle (0-1)
            val diveFactor = min(player.pitch / 90f, 1f)
            
            // Increase dive speed based on current angle
            currentDiveSpeed = min(
                currentDiveSpeed + diveAcceleration * diveFactor,
                1.2f  // Maximum dive speed multiplier
            )
            
            return 0f  // No upward boost while diving
        } else {
            // Gradually decrease dive speed when not diving
            val oldDiveSpeed = currentDiveSpeed
            currentDiveSpeed = max(0f, currentDiveSpeed - 0.01f)
            
            // Apply dive speed conversion to upward momentum when player pulls up
            if (player.pitch < 0 && oldDiveSpeed > 0) {
                val pullUpFactor = (-player.pitch / 90f) * diveEfficiency
                return oldDiveSpeed * pullUpFactor
            }
            
            return 0f
        }
    }
    
    /**
     * Update acceleration based on input and state
     */
    private fun handleAcceleration(shouldBoost: Boolean) {
        val maxAcceleration = boostSpeed
        
        if (shouldBoost && currentAcceleration < maxAcceleration) {
            // Gradual acceleration
            val accelerationFactor = 1f - currentAcceleration / maxAcceleration
            currentAcceleration += acceleration * accelerationFactor
            if (currentAcceleration > maxAcceleration) {
                currentAcceleration = maxAcceleration
            }
        } else if (!shouldBoost && currentAcceleration > 0) {
            // Gradual deceleration
            currentAcceleration *= 0.98f - acceleration
            if (currentAcceleration < 0.01f) {
                currentAcceleration = 0f
            }
        }
    }
    
    /**
     * Calculate effective flight speed with all modifiers
     */
    private fun calculateEffectiveSpeed(isNearGround: Boolean): Double {
        val baseSpeed = ModuleElytraFly.Speed.horizontal.toDouble()
        
        // Apply various speed modifiers
        val modifiers = mutableListOf<Double>()
        
        // Pitch adjustment (reduces horizontal speed when looking up)
        if (player.pitch < 0) {
            val reduction = abs(player.pitch / 90.0) * 0.3
            modifiers.add(1.0 - reduction)
        }
        
        // Apply dive speed boost
        if (currentDiveSpeed > 0) {
            modifiers.add(1.0 + currentDiveSpeed)
        }
        
        // Ground proximity reduction
        if (isNearGround) {
            modifiers.add(0.8)  // 20% speed reduction near ground
        }
        
        // Speed effect bonus
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            val amplifier = player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0
            modifiers.add(1.0 + (amplifier + 1) * 0.1)
        }
        
        // Apply all modifiers
        return modifiers.fold(baseSpeed) { acc, modifier -> acc * modifier }
    }
    
    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        if (ModuleElytraFly.shouldNotOperate() || !player.isGliding) {
            return@handler
        }
        
        // Get the pull-up boost from diving
        val divePullUpBoost = if (player.pitch < 0 && currentDiveSpeed > 0) {
            (-player.pitch / 90f) * diveEfficiency * currentDiveSpeed 
        } else {
            0f
        }
        
        // Apply boost when active
        if (currentAcceleration > 0 || currentDiveSpeed > 0) {
            val lookVec = player.getRotationVector()
            
            // Calculate boost factor
            val boostFactor = currentAcceleration.toDouble() + 
                (if (player.pitch > 0) currentDiveSpeed.toDouble() else divePullUpBoost.toDouble() * 0.2)
            
            // Adjust look vector near ground for more horizontal flight
            val adjustedLookVec = if (isNearGround()) {
                Vec3d(
                    lookVec.x * 1.3,
                    lookVec.y * 0.3,
                    lookVec.z * 1.3
                ).normalize()
            } else {
                lookVec
            }
            
            // Apply boost in the look direction
            val boostVec = Vec3d(
                adjustedLookVec.x * boostFactor,
                adjustedLookVec.y * boostFactor,
                adjustedLookVec.z * boostFactor
            )
            
            event.movement = event.movement.add(boostVec)
        }
        
        // Apply vertical control
        val horizontalSpeed = Math.sqrt(
            event.movement.x * event.movement.x + event.movement.z * event.movement.z
        )
        
        // Calculate lift based on horizontal speed
        val naturalLift = horizontalSpeed * 0.005
        
        // Apply additional vertical boost from diving pull-up maneuver
        val pullUpBoost = divePullUpBoost.toDouble() * 0.1
        
        // Apply vertical movement based on inputs
        event.movement = when {
            mc.options.jumpKey.isPressed -> {
                val upSpeed = (ModuleElytraFly.Speed.vertical.toDouble() * verticalControl) + pullUpBoost
                event.movement.withY(event.movement.y + upSpeed)
            }
            mc.options.sneakKey.isPressed -> {
                val downSpeed = ModuleElytraFly.Speed.vertical.toDouble() * verticalControl
                event.movement.withY(event.movement.y - downSpeed)
            }
            else -> {
                // Natural fall with lift
                event.movement.withY(event.movement.y - 0.008 + naturalLift + pullUpBoost)
            }
        }
    }
} 
