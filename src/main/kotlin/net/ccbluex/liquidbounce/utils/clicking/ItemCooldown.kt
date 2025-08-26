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
package net.ccbluex.liquidbounce.utils.clicking

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.kotlin.random

class ItemCooldown<T>(module: T) : ToggleableConfigurable(module, "ItemCooldown", true, aliases = arrayOf("Cooldown"))
    where T : EventListener {

    private val minimumCooldown by floatRange(
        "Minimum",
        1.0f..1.0f, 0.0f..2.0f
    )

    private var nextCooldown = minimumCooldown.random()

    fun isCooldownPassed(ticks: Int = 0) = !this.enabled || cooldownProgress(ticks) >= nextCooldown

    /**
     * Calculates the current cooldown progress.
     *
     * This can be out of percentage range [0, 1] to allow for higher minimum cooldowns.
     */
    fun cooldownProgress(baseTime: Int = 0) =
        (player.lastAttackedTicks + baseTime).toFloat() / player.attackCooldownProgressPerTick

    /**
     * Generates a new cooldown based on the range that was set by the user.
     */
    fun newCooldown() {
        nextCooldown = minimumCooldown.random()
    }

}
