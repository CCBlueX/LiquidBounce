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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold

object ScaffoldAccelerationFeature : ToggleableConfigurable(ModuleScaffold, "Acceleration", false) {
    private val speedMultiplier by float("SpeedMultiplier", 0.6f, 0.1f..3f)
    private val onlyOnGround by boolean("OnlyOnGround", false)

    @Suppress("unused")
    val stateUpdateHandler = tickHandler {
        if (onlyOnGround && !player.isOnGround) {
            return@tickHandler
        }

        player.velocity.x *= speedMultiplier
        player.velocity.z *= speedMultiplier
    }
}
