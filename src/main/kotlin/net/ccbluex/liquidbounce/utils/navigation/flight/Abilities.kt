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

package net.ccbluex.liquidbounce.utils.navigation.flight

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyVanilla
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkSettings
import net.minecraft.world.entity.LivingEntity.FLYING_AIR_DRAG

/** Whether the keys build up speed that fades, as in creative flight, or set it right away, as Fly's Vanilla mode. */
enum class Control { MOMENTUM, DIRECT }

/** How the player flies, captured on the render thread for one search. Speeds are in blocks per tick. */
class Abilities(
    val control: Control,
    val horizontalSpeed: Double,
    val verticalSpeed: Double,
    val isFlying: Boolean,
) {

    val horizontalCost = 1.0 / horizontalSpeed
    val verticalCost = 1.0 / verticalSpeed

    companion object {
        const val VERTICAL_DRAG = 0.6

        private const val INPUT_SCALE = 0.98
        private const val VERTICAL_THRUST = 3.0

        fun capture(): Abilities? {
            if (FlyVanilla.running) {
                return Abilities(
                    Control.DIRECT,
                    FlyVanilla.BaseSpeed.horizontalSpeed.toDouble(),
                    FlyVanilla.BaseSpeed.verticalSpeed.toDouble(),
                    isFlying = true,
                )
            }

            val abilities = player.abilities
            if (!abilities.mayfly && !abilities.flying) {
                return null
            }

            // Sprinting doubles the flying speed, and the speed settles where drag takes what the keys add.
            val speed = abilities.flyingSpeed.toDouble()
            return Abilities(
                Control.MOMENTUM,
                horizontalSpeed = 2.0 * speed * INPUT_SCALE / (1.0 - FLYING_AIR_DRAG),
                verticalSpeed = speed * VERTICAL_THRUST / (1.0 - VERTICAL_DRAG),
                isFlying = abilities.flying,
            )
        }
    }

}

internal fun WalkSettings.captureFlight(): Abilities? = if (allowFly) Abilities.capture() else null
