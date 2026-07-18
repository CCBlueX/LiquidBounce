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
package net.ccbluex.liquidbounce.utils.aiming.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RotationTest {

    @Test
    fun testDirectionAngleUsesViewDirections() {
        assertEquals(
            20f,
            Rotation(0f, 80f).directionAngleTo(Rotation(180f, 80f)),
            1e-2f
        )
        assertEquals(
            0f,
            Rotation(0f, 90f).directionAngleTo(Rotation(180f, 90f)),
            1e-2f
        )
    }

    @Test
    fun testRotationDeltaLengthPreservesControlAxes() {
        assertEquals(
            180f,
            Rotation(0f, 90f).rotationDeltaLengthTo(Rotation(180f, 90f)),
            1e-3f
        )
        assertEquals(
            2f,
            Rotation(179f, 0f).rotationDeltaLengthTo(Rotation(-179f, 0f)),
            1e-3f
        )
    }

    @Test
    fun testDirectionAndRotationClosenessAreDistinct() {
        val first = Rotation(0f, 90f)
        val second = Rotation(180f, 90f)

        assertTrue(first.isDirectionCloseTo(second))
        assertFalse(first.isRotationDeltaCloseTo(second))
    }

}
