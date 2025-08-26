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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.sneaking

import net.ccbluex.liquidbounce.config.types.nesting.NoneChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerSneakMultiplier
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.utils.client.inGame
import kotlin.math.max

internal object NoSlowSneaking : ToggleableConfigurable(ModuleNoSlow, "Sneaking", true) {

    private val minMultiplier by float("MinMultiplier", 1f, 0.3f..1f)

    @Suppress("unused")
    private val modes = choices(this, "Mode") {
        arrayOf(
            NoneChoice(it),
            NoSlowSneakingSwitch(it),
            NoSlowSneakingAAC5(it),
        )
    }

    @Suppress("unused")
    val multiplierHandler = handler<PlayerSneakMultiplier> { event ->
        event.multiplier = max(event.multiplier, minMultiplier.toDouble())
    }

    override val running: Boolean
        get() = super.running && inGame && player.isSneaking
}
