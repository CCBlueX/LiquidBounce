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

import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.world.entity.Avatar.SWIMMING_BB_HEIGHT
import net.minecraft.world.entity.Entity.TOTAL_AIR_SUPPLY

/**
 * Where a swimmer fits in water and where it has air to breathe.
 */
internal class WalkWater(private val terrain: NavigationTerrain, private val canSwim: Boolean) {

    /**
     * Whether a swimmer fits in the water block [x] [y] [z]: upright, or in the swimming pose with the head under
     * water when [canSwim], which needs sprinting.
     */
    fun fits(x: Int, y: Int, z: Int) = terrain.isWater(x, y, z) &&
        (fitsUpright(x, y, z) || canSwim && isSubmerged(x, y, z) && fitsSwimming(x, y, z))

    /**
     * Whether a swimmer that [fits] in [x] [y] [z] has to be in the swimming pose there.
     */
    fun isLow(x: Int, y: Int, z: Int) = !fitsUpright(x, y, z)

    /**
     * Whether a swimmer in [x] [y] [z] has no air to breathe: the head is under water or under a ceiling.
     */
    fun isSubmerged(x: Int, y: Int, z: Int) = terrain.isWater(x, y + 1, z) || !terrain.isPassable(x, y + 1, z)

    private fun fitsUpright(x: Int, y: Int, z: Int) =
        terrain.isClear(terrain.box(x + 0.5, y.toDouble(), z + 0.5))

    private fun fitsSwimming(x: Int, y: Int, z: Int) =
        terrain.isClear(terrain.box(x + 0.5, y.toDouble(), z + 0.5, SWIMMING_HEIGHT))

    companion object {
        /**
         * Height of a player in the swimming pose.
         */
        const val SWIMMING_HEIGHT = SWIMMING_BB_HEIGHT.toDouble()

        /**
         * Ticks of air a player has when breathing freely.
         */
        const val MAX_AIR = TOTAL_AIR_SUPPLY

        /**
         * Moves in a row with the head under water that [air] ticks of air last for: half of them at the speed of
         * swimming without sprinting, so the walk keeps a margin.
         */
        fun dive(air: Int) = (air / 2 / WalkCosts.SWIM).toInt()
    }

}
