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

package net.ccbluex.liquidbounce.utils.navigation.walk

import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.ccbluex.liquidbounce.utils.navigation.terrain.hasRoom
import net.minecraft.world.entity.Avatar.SWIMMING_BB_HEIGHT
import net.minecraft.world.entity.Entity.TOTAL_AIR_SUPPLY

/** Where a swimmer fits in water: upright, or submerged in the swimming pose, which needs sprinting. */
internal class Water(private val terrain: Terrain, private val canSprint: Boolean) {

    fun fits(x: Int, y: Int, z: Int) = terrain.isWater(x, y, z) &&
        (!isLow(x, y, z) || canSprint && isSubmerged(x, y, z) && terrain.hasRoom(x, y, z, SWIMMING_HEIGHT))

    fun isLow(x: Int, y: Int, z: Int) = !terrain.hasRoom(x, y, z)

    fun isSubmerged(x: Int, y: Int, z: Int) = terrain.isWater(x, y + 1, z) || !terrain.isPassable(x, y + 1, z)

    companion object {
        private const val SWIMMING_HEIGHT = SWIMMING_BB_HEIGHT.toDouble()
        const val MAX_AIR = TOTAL_AIR_SUPPLY
        private const val AIR_MARGIN = 2

        fun movesUnderwater(air: Int) = (air / AIR_MARGIN / Costs.SWIM).toInt()
    }

}
