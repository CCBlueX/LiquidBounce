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

import net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra.InterceptElytraSolver.WIND_CHARGE_SPEED
import net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra.InterceptElytraSolver.estimateFlightTicks
import net.ccbluex.liquidbounce.features.module.modules.combat.interceptelytra.InterceptElytraSolver.predictGliderLinear
import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals

class GliderPredictionTest {

    private val base = Vec3(10.0, 20.0, 30.0)
    private val velocity = Vec3(0.5, -0.1, 0.25)

    @Test
    fun `zero ticks returns base position`() {
        assertEquals(base, predictGliderLinear(base, velocity, ticks = 0.0))
    }

    @Test
    fun `linear extrapolation scales with ticks and multiplier`() {
        val ticks = 4.0
        val multiplier = 2.0

        val predicted = predictGliderLinear(base, velocity, ticks, multiplier)
        val expected = base.add(velocity.scale(ticks * multiplier))

        assertEquals(expected.x, predicted.x, 1e-9)
        assertEquals(expected.y, predicted.y, 1e-9)
        assertEquals(expected.z, predicted.z, 1e-9)
    }

    @Test
    fun `estimateFlightTicks equals distance over speed`() {
        assertEquals(3.0 / WIND_CHARGE_SPEED, estimateFlightTicks(3.0), 1e-9)
        assertEquals(0.0, estimateFlightTicks(0.0), 1e-9)
    }
}
