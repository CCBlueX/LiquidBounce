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

package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import net.minecraft.world.item.BucketItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ItemCategorizationTest {

    companion object {
        init { MinecraftBootstrap.ensureInitialized() }
    }

    @Test
    fun `a fluid bucket holds the fluid it is categorized by`() {
        assertFalse(Items.MILK_BUCKET is BucketItem)
        assertEquals(Fluids.WATER, (Items.WATER_BUCKET as BucketItem).content)
        assertEquals(Fluids.LAVA, (Items.LAVA_BUCKET as BucketItem).content)
    }

}
