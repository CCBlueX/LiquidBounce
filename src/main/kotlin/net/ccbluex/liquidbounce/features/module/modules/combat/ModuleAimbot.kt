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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MouseRotationEvent
import net.ccbluex.liquidbounce.event.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.rotation
import kotlin.math.round

/**
 * Aimbot module
 *
 * Automatically faces selected entities around you.
 */
object ModuleAimbot : Module("Aimbot", Category.COMBAT) {

    private val range by float("Range", 4.2f, 1f..8f)

    private val targetTracker = tree(TargetTracker(PriorityEnum.DIRECTION))

    private var targetRotation: Rotation? = null

    override fun disable() {
        targetRotation = null
    }

    val tickHandler = handler<PlayerNetworkMovementTickEvent> {
        if (it.state != EventState.PRE) {
            return@handler
        }

        for (target in targetTracker.enemies()) {
            if (target.boxedDistanceTo(player) > range) {
                continue
            }

            if (targetTracker.fov >= RotationManager.rotationDifference(target)) {
                val spot = RotationManager.raytraceBox(player.eyes, target.box, range.toDouble(), 0.0) ?: break

                targetRotation = spot.rotation
                return@handler
            }
        }

        targetRotation = null
    }

    val mouseRotationHandler = handler<MouseRotationEvent> { event ->
        targetRotation?.let {
            val f = mc.options.mouseSensitivity.value * 0.6F.toDouble() + 0.2F.toDouble()
            val gcd = f * f * f * 8.0

            val rotation = RotationManager.limitAngleChange(player.rotation, it, 13.37f)

            val (yaw, pitch) = Rotation(rotation.yaw - player.yaw, rotation.pitch - player.pitch)

            event.cursorDeltaX += round(yaw / gcd) * gcd
            event.cursorDeltaY += round(pitch / gcd) * gcd
        }
    }

}
