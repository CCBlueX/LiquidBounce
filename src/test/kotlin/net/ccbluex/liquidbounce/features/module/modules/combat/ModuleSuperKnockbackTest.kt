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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModuleSuperKnockbackTest {

    /**
     * A target looking along +z, and the player at [degrees] off that direction around the y axis.
     */
    private fun isLookingTowardsPlayerAt(degrees: Double): Boolean {
        val radians = Math.toRadians(degrees)
        val targetToPlayer = Vec3(sin(radians) * 3.0, 0.5, cos(radians) * 3.0)

        return isLookingTowards(Vec3(0.0, 0.0, 1.0), targetToPlayer)
    }

    @Test
    fun `a target looking at the player faces them`() {
        assertTrue(isLookingTowardsPlayerAt(0.0))
        assertTrue(isLookingTowardsPlayerAt(45.0))
        assertTrue(isLookingTowardsPlayerAt(-80.0))
    }

    @Test
    fun `a target looking away from the player does not face them`() {
        assertFalse(isLookingTowardsPlayerAt(180.0))
        assertFalse(isLookingTowardsPlayerAt(135.0))
        assertFalse(isLookingTowardsPlayerAt(-100.0))
    }

}
