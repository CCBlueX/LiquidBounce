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

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.util.math.MathHelper

internal object ElytraFlyModePitch40Infinite : ElytraFlyMode("Pitch40Infinite") {

    private val minSpeed by float("MinSpeed", 25f, 10f..70f)
    private val maxSpeed by float("MaxSpeed", 150f, 50f..170f)
    private val maxHeight by int("MaxHeight", 200, 50..360)
    private val pitchIncrement by float("PitchIncrement", 3f, 1f..10f)
    
    private var infinitePitch = 0f
    private var infiniteFlag = false
    
    // Speed conversion constant (m/s to km/h approximately)
    private const val SPEED_CONVERSION_FACTOR = 72f
    private const val MIN_PITCH = -40f
    private const val MAX_PITCH = 40f

    override fun enable() {
        resetState()
        
        // Warning if player is too low
        if (player.y < maxHeight) {
            notification(
                "ElytraFly",
                ModuleElytraFly.message("altitudeTooLow", maxHeight),
                NotificationEvent.Severity.INFO
            )
        }
    }

    override fun onTick() {
        if (!player.isGliding) {
            return
        }

        val currentSpeed = calculateCurrentSpeed()
        updateInfiniteFlag(currentSpeed)
        updatePitch()
    }
    
    /**
     * Resets the mode state to initial values
     */
    private fun resetState() {
        infinitePitch = 0f
        infiniteFlag = false
    }
    
    /**
     * Calculates the current player speed in km/h
     */
    private fun calculateCurrentSpeed(): Float {
        return (player.velocity.horizontalLength() * SPEED_CONVERSION_FACTOR).toFloat()
    }
    
    /**
     * Updates the infinite flag based on speed and height
     */
    private fun updateInfiniteFlag(speed: Float) {
        infiniteFlag = when {
            player.y >= maxHeight -> true
            speed < minSpeed && !infiniteFlag -> true
            speed > maxSpeed && infiniteFlag -> false
            else -> infiniteFlag
        }
    }
    
    /**
     * Updates the pitch value with smooth changes
     */
    private fun updatePitch() {
        val pitchDelta = if (infiniteFlag) pitchIncrement else -pitchIncrement
        infinitePitch = MathHelper.clamp(
            infinitePitch + pitchDelta,
            MIN_PITCH,
            MAX_PITCH
        )
        
        player.pitch = infinitePitch
    }
} 
