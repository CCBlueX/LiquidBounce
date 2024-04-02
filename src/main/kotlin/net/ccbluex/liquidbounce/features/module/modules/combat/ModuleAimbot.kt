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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer

/**
 * Aimbot module
 *
 * Automatically faces selected entities around you.
 */
object ModuleAimbot : Module("Aimbot", Category.COMBAT) {

    private val range by float("Range", 4.2f, 1f..8f)

    private object OnClick : ToggleableConfigurable(this, "OnClick", false) {
        val delayUntilStop by int("DelayUntilStop", 3, 0..10, "ticks")
    }

    init {
        tree(OnClick)
    }

    private val targetTracker = tree(TargetTracker(PriorityEnum.DIRECTION))
    private val targetRenderer = tree(WorldTargetRenderer(this))
    private val pointTracker = tree(PointTracker())
    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    private val clickTimer = Chronometer()

    val tickHandler = handler<SimulatedTickEvent> { _ ->
        targetTracker.cleanup()

        if (mc.options.attackKey.isPressed) {
            clickTimer.reset()
        }

        if (OnClick.enabled && (clickTimer.hasElapsed(OnClick.delayUntilStop * 50L)
                || !mc.options.attackKey.isPressed && ModuleAutoClicker.enabled)) {
            return@handler
        }

        findNextTargetRotation()?.also { vecRotation ->
            RotationManager.aimAt(
                vecRotation,
                true,
                rotationsConfigurable,
                Priority.IMPORTANT_FOR_USAGE_1,
                this@ModuleAimbot
            )
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        val partialTicks = event.partialTicks
        val target = targetTracker.lockedOnTarget ?: return@handler

        renderEnvironmentForWorld(matrixStack) {
            targetRenderer.render(this, target, partialTicks)
        }
    }

    private fun findNextTargetRotation(): VecRotation? {
        for (target in targetTracker.enemies()) {
            if (target.boxedDistanceTo(player) > range) {
                continue
            }

            val (fromPoint, toPoint, box, cutOffBox) = pointTracker.gatherPoint(target,
                PointTracker.AimSituation.FOR_NOW)

            val rotationPreference = LeastDifferencePreference(player.rotation, toPoint)

            val spot = raytraceBox(
                fromPoint,
                cutOffBox,
                range = range.toDouble(),
                wallsRange = 0.0,
                rotationPreference = rotationPreference
            ) ?: raytraceBox(
                fromPoint, box, range = range.toDouble(),
                wallsRange = 0.0,
                rotationPreference = rotationPreference
            ) ?: continue

            if (RotationManager.rotationDifference(player.rotation, spot.rotation)
                <= rotationsConfigurable.resetThreshold) {
                break
            }

            targetTracker.lock(target)
            return spot
        }

        return null
    }

}
