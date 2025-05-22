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

/**
 * ElytraFly boost mode
 *
 * Allows to fly with elytra without fireworks by simulating natural flight mechanics
 */
@Suppress("MagicNumber")
internal object ElytraFlyModeBoost : ElytraFlyMode("Boost") {

    private val boostSpeed by float("Speed", 0.9f, 0.5f..2.0f)
    private val acceleration by float("Acceleration", 0.01f, 0.005f..0.05f)
    private val autoBoost by boolean("AutoBoost", true)
    private val diveMechanics by boolean("DiveMechanics", true)
    private val smartGround by boolean("SmartGroundBehavior", true)
    private val groundDistance by float("GroundDistance", 3.0f, 1.5f..7.0f)
    private val verticalControl by float("VerticalControl", 0.8f, 0.2f..1.0f)
    
    private val diveAcceleration by float("DiveAcceleration", 0.05f, 0.01f..0.1f)
    private val diveEfficiency by float("DiveEfficiency", 0.8f, 0.4f..1.5f)
    
    private var currentAcceleration = 0f
    private var currentDiveSpeed = 0f
    
    private const val MAX_DIVE_SPEED_MULTIPLIER = 1.2f
    private const val DIVE_SPEED_REDUCTION = 0.01f

    override fun enable() {
        resetState()
    }

    override fun disable() {
        resetState()
    }
    
    private fun resetState() {
        currentAcceleration = 0f
        currentDiveSpeed = 0f
    }
    
    private fun isNearGround(): Boolean = 
        smartGround && world.getBlockCollisions(
            player, 
            player.boundingBox.offset(0.0, -groundDistance.toDouble(), 0.0)
        ).iterator().hasNext()
    
    override fun onTick() {
        if (!player.isGliding) {
            resetState()
            return
        }

        val isNearGround = isNearGround()
        val divePullUpBoost = handleDiving()
        
        val shouldBoost = mc.options.jumpKey.isPressed || 
            (autoBoost && player.pitch < -10f && !isNearGround) ||
            divePullUpBoost > 0
            
        handleAcceleration(shouldBoost)
        
        if (player.moving) {
            player.velocity = player.velocity.withStrafe(
                speed = calculateEffectiveSpeed(isNearGround)
            )
        }
    }
    
    private fun handleDiving(): Float {
        if (!diveMechanics) {
            currentDiveSpeed = max(0f, currentDiveSpeed - DIVE_SPEED_REDUCTION)
            return 0f
        }
        
        val oldDiveSpeed = currentDiveSpeed
        
        if (player.pitch > 15f) {
            val diveFactor = min(player.pitch / 90f, 1f)
            currentDiveSpeed = min(
                currentDiveSpeed + diveAcceleration * diveFactor,
                MAX_DIVE_SPEED_MULTIPLIER
            )
            return 0f
        } else {
            currentDiveSpeed = max(0f, currentDiveSpeed - DIVE_SPEED_REDUCTION)
            
            if (player.pitch < 0 && oldDiveSpeed > 0) {
                return oldDiveSpeed * (-player.pitch / 90f) * diveEfficiency
            }
        }
        
        return 0f
    }
    
    private fun handleAcceleration(shouldBoost: Boolean) {
        if (shouldBoost && currentAcceleration < boostSpeed) {
            val accelerationFactor = 1f - currentAcceleration / boostSpeed
            currentAcceleration += acceleration * accelerationFactor
            currentAcceleration = min(currentAcceleration, boostSpeed)
        } else if (!shouldBoost && currentAcceleration > 0) {
            currentAcceleration *= 0.98f - acceleration
            if (currentAcceleration < 0.01f) {
                currentAcceleration = 0f
            }
        }
    }
    
    private fun calculateEffectiveSpeed(isNearGround: Boolean): Double {
        val baseSpeed = ModuleElytraFly.Speed.horizontal.toDouble()
        var finalModifier = 1.0
        
        // Apply pitch-based reduction
        if (player.pitch < 0) {
            finalModifier *= 1.0 - (abs(player.pitch / 90.0) * 0.3)
        }
        
        // Apply dive speed boost
        if (currentDiveSpeed > 0) {
            finalModifier *= 1.0 + currentDiveSpeed
        }
        
        // Apply ground proximity penalty
        if (isNearGround) {
            finalModifier *= 0.8
        }
        
        // Apply speed potion effect
        player.getStatusEffect(StatusEffects.SPEED)?.let { effect ->
            finalModifier *= 1.0 + (effect.amplifier + 1) * 0.1
        }
        
        return baseSpeed * finalModifier
    }
    
    private fun calculateDivePullUpBoost(): Double {
        return if (player.pitch < 0 && currentDiveSpeed > 0) {
            (-player.pitch / 90f) * diveEfficiency * currentDiveSpeed * 0.1
        } else {
            0.0
        }
    }
    
    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        if (ModuleElytraFly.shouldNotOperate() || !player.isGliding) {
            return@handler
        }
        
        val divePullUpBoost = calculateDivePullUpBoost()
        val nearGround = isNearGround()
        
        if (currentAcceleration > 0 || currentDiveSpeed > 0) {
            val lookVec = player.getRotationVector()
            
            val boostFactor = currentAcceleration.toDouble() + 
                (if (player.pitch > 0) currentDiveSpeed.toDouble() else divePullUpBoost * 2.0)
            
            val adjustedLookVec = if (nearGround) {
                Vec3d(lookVec.x * 1.3, lookVec.y * 0.3, lookVec.z * 1.3).normalize()
            } else {
                lookVec
            }
            
            val boostVec = adjustedLookVec.multiply(boostFactor)
            event.movement = event.movement.add(boostVec)
        }
        
        val horizontalSpeed = Math.sqrt(
            event.movement.x * event.movement.x + event.movement.z * event.movement.z
        )
        
        val naturalLift = horizontalSpeed * 0.005
        
        event.movement = when {
            mc.options.jumpKey.isPressed && mc.options.sneakKey.isPressed -> {
                event.movement.copy(y = event.movement.y - 0.008 + naturalLift + divePullUpBoost)
            }
            mc.options.jumpKey.isPressed -> {
                val upSpeed = (ModuleElytraFly.Speed.vertical.toDouble() * verticalControl) + divePullUpBoost
                event.movement.copy(y = event.movement.y + upSpeed)
            }
            mc.options.sneakKey.isPressed -> {
                val downSpeed = ModuleElytraFly.Speed.vertical.toDouble() * verticalControl
                event.movement.copy(y = event.movement.y - downSpeed)
            }
            else -> {
                event.movement.copy(y = event.movement.y - 0.008 + naturalLift + divePullUpBoost)
            }
        }
    }
} 
