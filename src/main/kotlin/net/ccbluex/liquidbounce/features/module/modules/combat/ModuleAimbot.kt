/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.Priority

/**
 * Aimbot module
 *
 * Automatically faces selected entities around you.
 */
object ModuleAimbot : Module("Aimbot", Category.COMBAT) {

    private val range by float("Range", 4.2f, 1f..8f)

    private val targetTracker = tree(TargetTracker(PriorityEnum.DIRECTION))
    private val pointTracker = tree(PointTracker())
    private val rotationsConfigurable = tree(RotationsConfigurable(10f..30f))

    private var targetRotation: Rotation? = null

    override fun disable() {
        targetRotation = null
    }

    val tickHandler = handler<SimulatedTickEvent> { _ ->
        targetRotation = findNextTargetRotation()
        targetRotation?.let {
            RotationManager.aimAt(
                it,
                true,
                rotationsConfigurable,
                Priority.IMPORTANT_FOR_USAGE_1,
                this@ModuleAimbot
            )
        }
    }

    private fun findNextTargetRotation(): Rotation? {
        for (target in targetTracker.enemies()) {
            if (target.boxedDistanceTo(player) > range) {
                continue
            }

            if (targetTracker.fov >= RotationManager.rotationDifference(target)) {
                val (fromPoint, toPoint, box, cutOffBox) = pointTracker.gatherPoint(target,
                    PointTracker.AimSituation.FOR_NOW)
                val rotationPreference = LeastDifferencePreference(RotationManager.serverRotation, toPoint)

                val spot = raytraceBox(
                    fromPoint,
                    cutOffBox,
                    range = range.toDouble(),
                    wallsRange = 0.0,
                    rotationPreference = rotationPreference
                ) ?: raytraceBox(
                    fromPoint, box, range = range.toDouble(), wallsRange = 0.0, rotationPreference = rotationPreference
                ) ?: continue

                if (RotationManager.rotationDifference(
                        player.rotation, spot.rotation
                    ) <= rotationsConfigurable.resetThreshold
                ) {
                    break
                }

                return spot.rotation
            }
        }

        return null
    }
}
