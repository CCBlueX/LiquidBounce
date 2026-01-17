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

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.ModuleTerrainSpeed
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.copy

internal object WaterSpeed : ToggleableConfigurable(ModuleTerrainSpeed, "WaterSpeed", true) {

    val autoSwim by boolean("AutoSwim", true)
    val horizontalSpeed by float("HorizontalSpeed", 0.1f, 0.01f..10f)
    object SwimmingBoost : ToggleableConfigurable(this@WaterSpeed, "SwimmingBoost", true) {
        val sprintBoost by float("Boost", 0.30f, 0.01f..10f)
    }
    val verticalSpeed by float("VerticalSpeed", 0.25f, 0.01f..2f)

    init {
        tree(SwimmingBoost)
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

        if (player.moving) {
            val speed = if (player.isSprinting && SwimmingBoost.enabled) {
                horizontalSpeed * (1.0 + SwimmingBoost.sprintBoost)
            } else {
                horizontalSpeed
            }

            player.deltaMovement = player.deltaMovement.withStrafe(
                speed = speed.toDouble()
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
