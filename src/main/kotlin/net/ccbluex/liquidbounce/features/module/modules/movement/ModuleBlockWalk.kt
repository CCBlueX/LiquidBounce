/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.Blocks
import net.minecraft.util.shape.VoxelShapes

/**
 * BlockWalk module
 *
 * Allows you to walk on non-fullblock blocks.
 */

object ModuleBlockWalk : Module("BlockWalk", Category.MOVEMENT) {

    private val blocks by blocks("Blocks", hashSetOf(Blocks.COBWEB, Blocks.SNOW))

    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (event.state.block in blocks) {
            event.shape = VoxelShapes.fullCube()
        }
    }
}
