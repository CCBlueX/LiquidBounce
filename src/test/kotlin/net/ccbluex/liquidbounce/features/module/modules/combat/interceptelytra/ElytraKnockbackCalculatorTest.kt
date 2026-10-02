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
package net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra

import net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra.InterceptElytraSolver.KNOCKBACK_MULTIPLIER
import net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra.InterceptElytraSolver.predictKnockback
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElytraKnockbackCalculatorTest {

    private val center = Vec3(0.0, 0.0, 0.0)
    private val eye = Vec3(0.0, 1.6, 0.0)

    @Test
    fun `full power near center with full exposure`() {
        // Just above the center: the eye can never sit exactly on it (degenerate case returns zero).
        val knockback = predictKnockback(center, Vec3(0.0, 0.001, 0.0), exposure = 1.0)

        assertEquals(KNOCKBACK_MULTIPLIER, knockback.length(), 1e-3)
        assertEquals(Vec3(0.0, 1.0, 0.0).x, knockback.normalize().x, 1e-9)
        assertEquals(Vec3(0.0, 1.0, 0.0).y, knockback.normalize().y, 1e-9)
        assertEquals(Vec3(0.0, 1.0, 0.0).z, knockback.normalize().z, 1e-9)
    }

    @Test
    fun `zero beyond normalized distance one`() {
        assertEquals(Vec3.ZERO, predictKnockback(center, Vec3(0.0, 3.0, 0.0), exposure = 1.0))
    }

    @Test
    fun `zero exposure yields zero knockback`() {
        assertEquals(Vec3.ZERO, predictKnockback(center, eye, exposure = 0.0))
    }

    @Test
    fun `knockback resistance halves the power`() {
        val knockback = predictKnockback(center, Vec3(0.0, 0.001, 0.0), exposure = 1.0, knockbackResistance = 0.5)

        assertEquals(KNOCKBACK_MULTIPLIER * 0.5, knockback.length(), 1e-3)
    }

    @Test
    fun `linear falloff with distance`() {
        // distance 1.2 blocks -> normalized 0.5 -> power (1 - 0.5) * 1.22
        val knockback = predictKnockback(center, Vec3(1.2, 0.0, 0.0), exposure = 1.0)

        assertEquals(KNOCKBACK_MULTIPLIER * 0.5, knockback.length(), 1e-9)
    }

    @Test
    fun `degenerate center equals eye returns zero`() {
        assertEquals(Vec3.ZERO, predictKnockback(eye, eye, exposure = 1.0))
    }

    @Test
    fun `creative flying multiplier zeroes knockback`() {
        assertEquals(Vec3.ZERO, predictKnockback(center, eye, exposure = 1.0, knockbackMultiplier = 0.0))
    }

    @Test
    fun `knockback points radially away from the center`() {
        val knockback = predictKnockback(Vec3(10.0, 0.0, 0.0), Vec3(10.0, 1.0, 0.0), exposure = 1.0)

        assertTrue(knockback.x == 0.0 && knockback.y > 0.0 && knockback.z == 0.0)
    }
}
