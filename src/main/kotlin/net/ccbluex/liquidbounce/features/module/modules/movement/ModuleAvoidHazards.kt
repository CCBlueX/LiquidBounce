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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.BasePressurePlateBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.CactusBlock
import net.minecraft.world.level.block.FireBlock
import net.minecraft.world.level.block.MagmaBlock
import net.minecraft.world.level.block.PowderSnowBlock
import net.minecraft.world.level.block.SweetBerryBushBlock
import net.minecraft.world.level.block.WebBlock
import net.minecraft.world.level.block.WitherRoseBlock
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.shapes.Shapes

/**
 * Anti hazards module
 *
 * Prevents you walking into blocks that might be malicious for you.
 */
object ModuleAvoidHazards : ClientModule("AvoidHazards", ModuleCategories.MOVEMENT) {
    private val avoid by multiEnumChoice("Avoid", Avoid.entries)

    // Conflicts with AvoidHazards
    val cobWebs get() = Avoid.COBWEB in avoid

    @Suppress("MagicNumber")
    private val UNSAFE_BLOCK_CAP = Block.box(
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
            event.shape = if (it.fullCube) Shapes.block() else UNSAFE_BLOCK_CAP
        }
    }

    private enum class Avoid(
        override val tag: String,
        val fullCube: Boolean = true,
        val test: (block: Block, fluidState: FluidState, pos: BlockPos) -> Boolean
    ) : Tagged {
        CACTI("Cacti", test = { block, _, _ ->
            block is CactusBlock
        }),
        BERRY_BUSH("BerryBush", test = { block, _, _ ->
            block is SweetBerryBushBlock
        }),
        FIRE("Fire", test = { block, _, _ ->
            block is FireBlock
        }),
        COBWEB("Cobwebs", test = { block, _, _ ->
            block is WebBlock
        }),
        PRESSURE_PLATES("PressurePlates", fullCube = false, test = { block, _, _ ->
            block is BasePressurePlateBlock
        }),
        MAGMA("MagmaBlocks", fullCube = false, test = { _, _, pos ->
            pos.below().getBlock() is MagmaBlock
        }),
        LAVA("Lava", test = { _, fluidState, _ ->
            fluidState.`is`(Fluids.LAVA) || fluidState.`is`(Fluids.FLOWING_LAVA)
        }),
        WITHER_ROSE("WitherRose", test = { block, _, _ ->
            block is WitherRoseBlock
        }),
        POWDER_SNOW("PowderSnow", test = { block, _, _ ->
            block is PowderSnowBlock
        })
    }
}
