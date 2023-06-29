/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.minecraft.util.math.MathHelper

/**
 * Sprint module
 *
 * Sprints automatically.
 */

object ModuleSprint : Module("Sprint", Category.MOVEMENT) {

    val allDirections by boolean("AllDirections", false)
    val ignoreBlindness by boolean("IgnoreBlindness", false)
    val ignoreHunger by boolean("IgnoreHunger", false)
    val stopOnGround by boolean("StopOnGround", true)
    val stopOnAir by boolean("StopOnAir", true)

    fun shouldSprintOmnidirectionally() = enabled && allDirections

    fun shouldIgnoreBlindness() = enabled && ignoreBlindness

    fun shouldIgnoreHunger() = enabled && ignoreHunger

    fun shouldPreventSprint(): Boolean {
        val player = mc.player ?: return false

        val deltaYaw = player.yaw - (RotationManager.currentRotation ?: return false).yaw
        val (forward, sideways) = Pair(player.input.movementForward, player.input.movementSideways)

        val hasForwardMovement =
            forward * MathHelper.cos(deltaYaw * 0.017453292f) + sideways * MathHelper.sin(deltaYaw * 0.017453292f) > 1.0E-5

        val preventSprint =
            (if (player.isOnGround) stopOnGround else stopOnAir) && !shouldSprintOmnidirectionally() && RotationManager.activeConfigurable?.fixVelocity == false && !hasForwardMovement

        return enabled && preventSprint
    }
}
