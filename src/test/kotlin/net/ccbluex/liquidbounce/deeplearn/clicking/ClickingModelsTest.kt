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
package net.ccbluex.liquidbounce.deeplearn.clicking

import net.ccbluex.liquidbounce.deeplearn.model.ModelFile
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClickingModelsTest {
    @Test
    fun `every style has a bundled model that fits its slot`() {
        for (style in ClickingStyle.entries) {
            val slot = ClickingModels.slot(style)
            val resource = javaClass.getResourceAsStream(slot.resource(style.model))
            val file = assertNotNull(resource, style.id).use(ModelFile::read)
            assertTrue(slot.accepts(file), style.id)
            assertEquals(ClickingOutputs.NETWORK.hidden, file.network.hidden)
            assertTrue(ClickingModels.meanInterval(file) in 50f..200f, "${ClickingModels.meanInterval(file)}")
        }
    }

    @Test
    fun `a drawn gap falls into its bin`() {
        val random = Random(1)
        repeat(ClickingOutputs.BINS) { bin ->
            val output = FloatArray(ClickingOutputs.BINS) { if (it == bin) 50f else 0f }
            assertEquals(bin, ClickingOutputs.bin(ClickingOutputs.sample(output, random)))
        }
    }
}
