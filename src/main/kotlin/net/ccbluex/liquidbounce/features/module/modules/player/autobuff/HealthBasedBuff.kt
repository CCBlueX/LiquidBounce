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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff

abstract class HealthBasedBuff(name: String) : Buff(name) {

    private val healthPercent by int("Health", 40, 1..100, "%HP")
    private val considerAbsorption by boolean("ConsiderAbsorption", true)

    val health
        get() = player.maxHealth * healthPercent / 100

    override val passesRequirements: Boolean
        get() = super.passesRequirements && passesHealthRequirements

    internal val passesHealthRequirements: Boolean
        get() {
            val fullHealth = player.health + if (considerAbsorption) player.absorptionAmount else 0f
            return fullHealth <= health
        }

}
