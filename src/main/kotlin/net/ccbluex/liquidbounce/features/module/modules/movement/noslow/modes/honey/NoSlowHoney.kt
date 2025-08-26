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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.honey

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.BlockVelocityMultiplierEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.minecraft.block.HoneyBlock

internal object NoSlowHoney : ToggleableConfigurable(ModuleNoSlow, "HoneyBlock", true) {

    val multiplier by float("Multiplier", 1f, 0.4f..2f)

    @Suppress("unused")
    val blockVelocityHandler = handler<BlockVelocityMultiplierEvent> { event ->
        if (event.block is HoneyBlock) {
            event.multiplier = multiplier
        }
    }

}
