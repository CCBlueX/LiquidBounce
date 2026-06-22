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

import net.minecraft.core.Direction
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LadderClimbStateTest {

    @Test
    fun `ladder block is climb state`() {
        assertTrue(
            isLadderClimbState(
                isLadderBlock = true,
                isTrapDoorBlock = false,
                trapDoorOpen = false,
                trapDoorFacing = null,
                lowerIsLadderBlock = false,
                lowerLadderFacing = null
            )
        )
    }

    @Test
    fun `open trapdoor with matching ladder below is climb state`() {
        assertTrue(
            isLadderClimbState(
                isLadderBlock = false,
                isTrapDoorBlock = true,
                trapDoorOpen = true,
                trapDoorFacing = Direction.NORTH,
                lowerIsLadderBlock = true,
                lowerLadderFacing = Direction.NORTH
            )
        )
    }

    @Test
    fun `open trapdoor with mismatching ladder below is not climb state`() {
        assertFalse(
            isLadderClimbState(
                isLadderBlock = false,
                isTrapDoorBlock = true,
                trapDoorOpen = true,
                trapDoorFacing = Direction.NORTH,
                lowerIsLadderBlock = true,
                lowerLadderFacing = Direction.SOUTH
            )
        )
    }

    @Test
    fun `closed trapdoor is not climb state`() {
        assertFalse(
            isLadderClimbState(
                isLadderBlock = false,
                isTrapDoorBlock = true,
                trapDoorOpen = false,
                trapDoorFacing = Direction.NORTH,
                lowerIsLadderBlock = true,
                lowerLadderFacing = Direction.NORTH
            )
        )
    }
}
