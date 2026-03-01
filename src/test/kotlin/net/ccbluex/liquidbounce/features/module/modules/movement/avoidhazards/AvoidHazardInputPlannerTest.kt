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
package net.ccbluex.liquidbounce.features.module.modules.movement.avoidhazards

import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvoidHazardInputPlannerTest {

    @Test
    fun `original input stays when already safe`() {
        val result = AvoidHazardInputPlanner.chooseSafeInput(DirectionalInput.FORWARDS) {
            it == DirectionalInput.FORWARDS
        }

        assertEquals(DirectionalInput.FORWARDS, result)
    }

    @Test
    fun `unsafe input uses nearest safe candidate`() {
        val safeCandidates = setOf(DirectionalInput.LEFT, DirectionalInput.FORWARDS_LEFT)

        val result = AvoidHazardInputPlanner.chooseSafeInput(DirectionalInput.FORWARDS) {
            it in safeCandidates
        }

        assertEquals(DirectionalInput.FORWARDS_LEFT, result)
    }

    @Test
    fun `returns none when no candidate is safe`() {
        val result = AvoidHazardInputPlanner.chooseSafeInput(DirectionalInput.FORWARDS) { false }

        assertEquals(DirectionalInput.NONE, result)
    }

    @Test
    fun `returns none when original has no movement`() {
        val result = AvoidHazardInputPlanner.chooseSafeInput(DirectionalInput.NONE) {
            throw AssertionError("Safety predicate should not be called for no input")
        }

        assertEquals(DirectionalInput.NONE, result)
    }
}

