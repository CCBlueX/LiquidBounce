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
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.minecraft.block.*
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShapes

/**
 * Anti hazards module
 *
 * Prevents you walking into blocks that might be malicious for you.
 */
object ModuleAvoidHazards : Module("AvoidHazards", Category.MOVEMENT) {

    val cacti by boolean("Cacti", true)
    val berryBush by boolean("BerryBush", true)
    val pressurePlates by boolean("PressurePlates", true)
    val fire by boolean("Fire", true)
    val magmaBlocks by boolean("MagmaBlocks", true)
    val cobWebs by boolean("Cobwebs", true)

    val shapeHandler = handler<BlockShapeEvent> { event ->
        if (cacti && event.state.block is CactusBlock) {
            event.shape = VoxelShapes.fullCube()
        } else if (berryBush && event.state.block is SweetBerryBushBlock) {
            event.shape = VoxelShapes.fullCube()
        } else if (fire && event.state.block is FireBlock) {
            event.shape = VoxelShapes.fullCube()
        } else if (cobWebs && event.state.block is CobwebBlock) {
            event.shape = VoxelShapes.fullCube()
        } else if (pressurePlates && event.state.block is AbstractPressurePlateBlock) {
            event.shape = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0)
        } else if (magmaBlocks && event.pos.down().getBlock() is MagmaBlock && !event.state.isSideSolid(
                world,
                event.pos,
                Direction.UP,
                SideShapeType.CENTER
            )
        ) {
            event.shape = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 4.0, 16.0)
        }
    }

}
