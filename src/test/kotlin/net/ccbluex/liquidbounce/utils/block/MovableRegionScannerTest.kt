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

package net.ccbluex.liquidbounce.utils.block

import net.minecraft.util.math.BlockBox
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MovableRegionScannerTest {

    private lateinit var scanner: MovableRegionScanner

    @BeforeEach
    fun setup() {
        scanner = MovableRegionScanner()
    }

    @Test
    fun `same region returns empty`() {
        val region = BlockBox(0, 0, 0, 2, 2, 2)

        scanner.moveTo(region) // set first region
        val result = scanner.moveTo(region)

        assertTrue(result.isEmpty(), "Moving to same region should yield no new boxes")
    }

    @Test
    fun `new region inside old returns empty`() {
        val oldRegion = BlockBox(0, 0, 0, 4, 4, 4)
        val innerRegion = BlockBox(1, 1, 1, 3, 3, 3)

        scanner.moveTo(oldRegion)
        val result = scanner.moveTo(innerRegion)

        assertTrue(result.isEmpty(), "New region inside old region should yield no new boxes")
    }

    @Test
    fun `non overlapping returns whole new region`() {
        val region1 = BlockBox(0, 0, 0, 2, 2, 2)
        val region2 = BlockBox(10, 10, 10, 12, 12, 12)

        scanner.moveTo(region1)
        val result = scanner.moveTo(region2)

        assertEquals(1, result.size)
        assertEquals(region2, result.first())
    }

    @Test
    fun `expansion on positive X`() {
        val region1 = BlockBox(0, 0, 0, 2, 2, 2)
        val region2 = BlockBox(0, 0, 0, 4, 2, 2)

        scanner.moveTo(region1)
        val result = scanner.moveTo(region2)

        assertEquals(1, result.size)
        val newBox = result.first()
        assertEquals(BlockBox(3, 0, 0, 4, 2, 2), newBox)
    }

    @Test
    fun `expansion on negative Z`() {
        val region1 = BlockBox(0, 0, 0, 2, 2, 2)
        val region2 = BlockBox(0, 0, -2, 2, 2, 2)

        scanner.moveTo(region1)
        val result = scanner.moveTo(region2)

        assertEquals(1, result.size)
        val newBox = result.first()
        assertEquals(BlockBox(0, 0, -2, 2, 2, -1), newBox)
    }

    @Test
    fun `expansion in multiple directions`() {
        val region1 = BlockBox(0, 0, 0, 2, 2, 2)
        val region2 = BlockBox(-1, -1, -1, 3, 3, 3)

        scanner.moveTo(region1)
        val result = scanner.moveTo(region2)

        // Expect expansion in -X, +X, -Y, +Y, -Z, +Z
        assertEquals(6, result.size)
    }
}
