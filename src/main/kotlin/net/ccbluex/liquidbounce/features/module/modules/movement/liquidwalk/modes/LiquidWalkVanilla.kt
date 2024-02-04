/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.ModuleLiquidWalk
import net.ccbluex.liquidbounce.utils.block.isBlockAtPosition
import net.ccbluex.liquidbounce.utils.entity.box
import net.minecraft.block.FluidBlock
import net.minecraft.fluid.Fluids
import net.minecraft.util.shape.VoxelShapes

internal object LiquidWalkVanilla : Choice("Vanilla") {

    private val dipIfBurning by boolean("dipIfBurning", true)

    // Add a new flag to control the execution of the function
    private var isPaused = false

    override val parent: ChoiceConfigurable
        get() = ModuleLiquidWalk.modes

    val shapeHandler = handler<BlockShapeEvent> { event ->
        // Check if the function is paused
        if (!isPaused) {
            if (event.state.fluidState.isOf(Fluids.WATER)
                && !isBlockAtPosition(player.box) { it is FluidBlock } && !player.input.sneaking) {
                event.shape = VoxelShapes.fullCube()
            }
        }

        if (dipIfBurning && player.isOnFire) {
            // Instead of changing the player's velocity, pause the function
            isPaused = true
        } else {
            // If the player is not on fire, resume the function
            isPaused = false
        }
    }
}
