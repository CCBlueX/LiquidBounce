/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SlabBlock
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class NoFallMlgPlacementTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrapMinecraft() {
            MinecraftBootstrap.ensureInitialized()
        }
    }

    @Test
    fun `water bucket only accepts a standalone source`() {
        assertTrue(canPlaceExposedWater(Blocks.AIR.defaultBlockState()))
        assertFalse(canPlaceExposedWater(waterloggedSlab()))
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
        assertTrue(shouldForceSneakForExposedWater(Items.WATER_BUCKET, Blocks.STONE_SLAB.defaultBlockState()))
        assertFalse(shouldForceSneakForExposedWater(Items.WATER_BUCKET, Blocks.STONE.defaultBlockState()))
    }

    private fun waterloggedSlab() = Blocks.STONE_SLAB.defaultBlockState()
        .setValue(SlabBlock.WATERLOGGED, true)
}
