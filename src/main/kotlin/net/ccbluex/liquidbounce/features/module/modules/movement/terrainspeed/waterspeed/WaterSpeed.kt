/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.waterspeed

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.ModuleTerrainSpeed
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.copy

internal object WaterSpeed : ToggleableValueGroup(ModuleTerrainSpeed, "WaterSpeed", true) {

    val autoSwim by boolean("AutoSwim", true)

    object BaseSpeed : ValueGroup("BaseSpeed") {
        val horizontalSpeed by float("Horizontal", 0.44f, 0.1f..10f)
        val verticalSpeed by float("Vertical", 0.44f, 0.1f..10f)
    }

    object SprintSpeed : ToggleableValueGroup(this, "SprintSpeed", true) {
        val horizontalSpeed by float("Horizontal", 1f, 0.1f..10f)
        val verticalSpeed by float("Vertical", 1f, 0.1f..10f)
    }

    init {
        tree(BaseSpeed)
        tree(SprintSpeed)
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent> { event ->
        if (autoSwim && player.isInWater && !mc.options.keyShift.isDown) {
            event.jump = true
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> {
        if (!player.isInWater) return@handler

        val useSprintSpeed = mc.options.keySprint.isDown && SprintSpeed.enabled
        val horizontalSpeed = if (useSprintSpeed) SprintSpeed.horizontalSpeed else BaseSpeed.horizontalSpeed
        val verticalSpeed = if (useSprintSpeed) SprintSpeed.verticalSpeed else BaseSpeed.verticalSpeed

        if (player.moving) {
            player.deltaMovement = player.deltaMovement.withStrafe(
                speed = horizontalSpeed.toDouble()
            )
        }

        player.deltaMovement = if (mc.options.keyJump.isDown) {
            player.deltaMovement.copy(y = verticalSpeed.toDouble())
        } else if (mc.options.keyShift.isDown) {
            player.deltaMovement.copy(y = -verticalSpeed.toDouble())
        } else {
            return@handler
        }
    }
}
