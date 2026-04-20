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

package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.test.assertVec3Equals
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlockBoxExtensionsTest {

    @Test
    fun `to returns maximum corner`() {
        val box = BoundingBox(10, 64, 20, 11, 65, 21)

        assertEquals(BlockPos(11, 65, 21), box.to)
    }

    @Test
    fun `centerOnSide keeps world offset for vertical faces`() {
        val box = BoundingBox(10, 64, 20, 11, 65, 21)

        assertVec3Equals(Vec3(11.0, 63.5, 21.0), box.centerOnSide(Direction.DOWN), 1e-9)
        assertVec3Equals(Vec3(11.0, 65.5, 21.0), box.centerOnSide(Direction.UP), 1e-9)
    }

    @Test
    fun `centerOnSide keeps world offset for horizontal faces`() {
        val box = BoundingBox(10, 64, 20, 11, 65, 21)

        assertVec3Equals(Vec3(11.5, 65.0, 21.0), box.centerOnSide(Direction.EAST), 1e-9)
        assertVec3Equals(Vec3(9.5, 65.0, 21.0), box.centerOnSide(Direction.WEST), 1e-9)
        assertVec3Equals(Vec3(11.0, 65.0, 21.5), box.centerOnSide(Direction.SOUTH), 1e-9)
        assertVec3Equals(Vec3(11.0, 65.0, 19.5), box.centerOnSide(Direction.NORTH), 1e-9)
    }
}
