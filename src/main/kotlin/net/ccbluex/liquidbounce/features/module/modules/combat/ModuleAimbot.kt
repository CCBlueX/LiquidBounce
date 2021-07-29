/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.MouseRotationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.applyRotation
import net.ccbluex.liquidbounce.utils.combat.PriorityEnum
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.eyesPos

/**
 * Aimbot module
 *
 * Automatically faces selected entities around you.
 */
object ModuleAimbot : Module("Aimbot", Category.COMBAT) {

    private val range by float("Range", 4.2f, 1f..8f)
    private val fov by float("FOV", 26f, 0.1f..180f)

    private val targetTracker = tree(TargetTracker(PriorityEnum.DIRECTION))

    private var currentRotation: Rotation? = null

    val repeatable = repeatable {
        val eyes = player.eyesPos

        for (target in targetTracker.enemies()) {
            if (target.boxedDistanceTo(player) > range) {
                continue
            }

            val box = target.boundingBox

            if (fov >= RotationManager.rotationDifference(RotationManager.makeRotation(box.center, eyes), Rotation(player.yaw, player.pitch))) {
                val (rotation, _) = RotationManager.raytraceBox(eyes, box, range = range.toDouble(), wallsRange = 0.0) ?: continue

                currentRotation = rotation
                return@repeatable
            }
        }

        currentRotation = null
    }

    val mouseRotationHandler = handler<MouseRotationEvent> {
        currentRotation?.let {
            player.applyRotation(RotationManager.limitAngleChange(Rotation(player.yaw, player.pitch), it, 0.12f))
        }
    }

}
