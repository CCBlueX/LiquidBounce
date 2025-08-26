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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.minecraft.block.*
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShapes

/**
 * Anti hazards module
 *
 * Prevents you walking into blocks that might be malicious for you.
 */
object ModuleAvoidHazards : ClientModule("AvoidHazards", Category.MOVEMENT) {
    private val avoid by multiEnumChoice("Avoid", Avoid.entries)

    // Conflicts with AvoidHazards
    val cobWebs get() = Avoid.COBWEB in avoid

    @Suppress("MagicNumber")
    private val UNSAFE_BLOCK_CAP = Block.createCuboidShape(
        0.0,
        0.0,
        0.0,
        16.0,
        4.0,
        16.0
    )

    @Suppress("unused")
    val shapeHandler = handler<BlockShapeEvent> { event ->
        avoid.find { it.test(event.state.block, event.state.fluidState, event.pos) }?.let {
            event.shape = if (it.fullCube) VoxelShapes.fullCube() else UNSAFE_BLOCK_CAP
        }
    }

    private enum class Avoid(
        override val choiceName: String,
        val fullCube: Boolean = true,
        val test: (block: Block, fluidState: FluidState, pos: BlockPos) -> Boolean
    ) : NamedChoice {
        CACTI("Cacti", test = { block, _, _ ->
            block is CactusBlock
        }),
        BERRY_BUSH("BerryBush", test = { block, _, _ ->
            block is SweetBerryBushBlock
        }),
        FIRE("Fire", test = { block, _, _, ->
            block is FireBlock
        }),
        COBWEB("Cobwebs", test = { block, _, _, ->
            block is CobwebBlock
        }),
        PRESSURE_PLATES("PressurePlates", fullCube = false, test = { block, _, _ ->
            block is AbstractPressurePlateBlock
        }),
        MAGMA("MagmaBlocks", fullCube = false, test = { _, _, pos ->
            pos.down().getBlock() is MagmaBlock
        }),
        LAVA("Lava", test = { _, fluidState, _ ->
            fluidState.isOf(Fluids.LAVA) || fluidState.isOf(Fluids.FLOWING_LAVA)
        })
    }
}
