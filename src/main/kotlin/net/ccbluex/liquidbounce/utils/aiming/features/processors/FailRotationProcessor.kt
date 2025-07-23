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
package net.ccbluex.liquidbounce.utils.aiming.features.processors

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.random.Random

/**
 * The fail focus acts as fail rate, it will purposely miss the target on a certain rate.
 */
class FailRotationProcessor(owner: EventListener? = null)
    : ToggleableConfigurable(owner, "Fail", false), RotationProcessor {

    private val failRate by int("Rate", 3, 1..100, "%")
    val failFactor by float("Factor", 0.04f, 0.01f..0.99f)

    private val strengthHorizontal by floatRange("StrengthHorizontal", 5f..10f, 1f..90f,
        "°")
    private val strengthVertical by floatRange("StrengthVertical", 0f..2f, 0f..90f,
        "°")

    /**
     * The duration it takes to transition from the fail factor to the normal factor.
     */
    private var transitionInDuration by intRange("TransitionInDuration", 1..4, 0..20,
        "ticks")

    private var ticksElapsed = 0
    private var currentTransitionInDuration = transitionInDuration.random()
    private var shiftRotation = Rotation(0f, 0f)

    val isInFailState: Boolean
        get() = running && ticksElapsed < currentTransitionInDuration

    @Suppress("unused")
    private val gameTick = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        // Fail rate
        val chance = (0f..100f).random()
        if (failRate > chance) {
            currentTransitionInDuration = transitionInDuration.random()

            val yawShift = if (Random.nextBoolean()) {
                strengthHorizontal.random()
            } else {
                -strengthHorizontal.random()
            }

            val pitchShift = if (Random.nextBoolean()) {
                strengthVertical.random()
            } else {
                -strengthVertical.random()
            }

            shiftRotation = Rotation(yawShift, pitchShift)
            ticksElapsed = 0

            ModuleDebug.debugParameter(this, "Chance", chance)
            ModuleDebug.debugParameter(this, "Duration", currentTransitionInDuration)
            ModuleDebug.debugParameter(this, "Shift", shiftRotation)
        } else {
            ticksElapsed++
        }

        ModuleDebug.debugParameter(this, "Elapsed", ticksElapsed)
    }

    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation {
        return if (this.running && isInFailState) {
            val prevRotation = RotationManager.previousRotation ?: return targetRotation
            val serverRotation = RotationManager.serverRotation

            val deltaYaw = (prevRotation.yaw - serverRotation.yaw) * failFactor
            val deltaPitch = (prevRotation.pitch - serverRotation.pitch) * failFactor

            ModuleDebug.debugParameter(this, "DeltaYaw", deltaYaw)
            ModuleDebug.debugParameter(this, "DeltaPitch", deltaPitch)

            return Rotation(
                targetRotation.yaw + deltaYaw + shiftRotation.yaw,
                targetRotation.pitch + deltaPitch + shiftRotation.pitch
            )
        } else {
            targetRotation
        }
    }

}
