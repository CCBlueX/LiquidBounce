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
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.state.BlockState

/**
 * Returns whether the block context should be treated as a climb state.
 *
 * This mirrors vanilla climbing behavior relevant for ladder entry checks:
 * 1. Standing in a ladder block.
 * 2. Standing in an open trapdoor that connects to a ladder below with matching facing.
 *
 * @see net.minecraft.world.entity.LivingEntity.onClimbable
 * @see net.minecraft.world.level.block.LadderBlock
 * @see net.minecraft.world.level.block.TrapDoorBlock
 */
fun isLadderClimbState(currentState: BlockState, belowState: BlockState?): Boolean {
    val isTrapDoor = currentState.block is TrapDoorBlock
    val trapDoorOpen = if (isTrapDoor) currentState.getValue(TrapDoorBlock.OPEN) else false
    val trapDoorFacing = if (isTrapDoor) currentState.getValue(TrapDoorBlock.FACING) else null

    val lowerIsLadder = belowState?.block is LadderBlock
    val lowerLadderFacing = if (lowerIsLadder) belowState?.getValue(LadderBlock.FACING) else null

    return isLadderClimbState(
        isLadderBlock = currentState.block is LadderBlock,
        isTrapDoorBlock = isTrapDoor,
        trapDoorOpen = trapDoorOpen,
        trapDoorFacing = trapDoorFacing,
        lowerIsLadderBlock = lowerIsLadder,
        lowerLadderFacing = lowerLadderFacing
    )
}

/**
 * Boolean-only variant used by unit tests to validate climb-state rules
 * without requiring Minecraft block-state bootstrap.
 */
internal fun isLadderClimbState(
    isLadderBlock: Boolean,
    isTrapDoorBlock: Boolean,
    trapDoorOpen: Boolean,
    trapDoorFacing: Direction?,
    lowerIsLadderBlock: Boolean,
    lowerLadderFacing: Direction?
): Boolean {
    if (isLadderBlock) {
        return true
    }

    if (!isTrapDoorBlock || !trapDoorOpen) {
        return false
    }

    return lowerIsLadderBlock &&
        trapDoorFacing != null &&
        lowerLadderFacing == trapDoorFacing
}
