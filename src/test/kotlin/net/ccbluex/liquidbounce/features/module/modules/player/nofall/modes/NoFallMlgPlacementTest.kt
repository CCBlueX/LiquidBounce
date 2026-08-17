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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.ccbluex.liquidbounce.utils.block.liquid.canPlaceStandaloneFluid
import net.ccbluex.liquidbounce.utils.block.liquid.requiresSneakForAdjacentFluidPlacement
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.material.Fluids
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoFallMlgPlacementTest {

    companion object {
        init {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    @Test
    fun `water bucket only accepts a standalone source`() {
        assertTrue(Blocks.AIR.defaultBlockState().canPlaceStandaloneFluid(Fluids.WATER))
        assertFalse(waterloggedSlab().canPlaceStandaloneFluid(Fluids.WATER))
        assertFalse(
            wasMlgPlacementApplied(
                MlgPlacementActionType.MLG,
                Items.WATER_BUCKET,
                Blocks.AIR.defaultBlockState(),
                waterloggedSlab(),
            ),
        )
    }

    @Test
    fun `placement success requires the expected state transition`() {
        assertTrue(
            wasMlgPlacementApplied(
                MlgPlacementActionType.SCAFFOLDING,
                Items.SCAFFOLDING,
                Blocks.AIR.defaultBlockState(),
                Blocks.SCAFFOLDING.defaultBlockState(),
            ),
        )
        assertFalse(
            wasMlgPlacementApplied(
                MlgPlacementActionType.SCAFFOLDING,
                Items.SCAFFOLDING,
                Blocks.SCAFFOLDING.defaultBlockState(),
                Blocks.SCAFFOLDING.defaultBlockState(),
            ),
        )
        assertTrue(
            wasMlgPlacementApplied(
                MlgPlacementActionType.MLG,
                Items.WATER_BUCKET,
                Blocks.AIR.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
            ),
        )
    }

    @Test
    fun `water bucket requires sneak for liquid containers`() {
        assertTrue(
            Blocks.STONE_SLAB.defaultBlockState()
                .requiresSneakForAdjacentFluidPlacement(Fluids.WATER),
        )
        assertFalse(
            Blocks.STONE.defaultBlockState()
                .requiresSneakForAdjacentFluidPlacement(Fluids.WATER),
        )
    }

    private fun waterloggedSlab() = Blocks.STONE_SLAB.defaultBlockState()
        .setValue(SlabBlock.WATERLOGGED, true)
}
