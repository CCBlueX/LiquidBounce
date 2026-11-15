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
package net.ccbluex.liquidbounce.deeplearn.model

import com.google.gson.JsonObject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelRegistryTest {
    @Test
    fun `slots only accept models for their task, input and outputs`() {
        val file = ModelFile("test", "default", "unit", InputSchema("test", 1, 4), NetworkSpec(listOf(8), 2),
            InputNormalization(FloatArray(4), FloatArray(4) { 1f }), JsonObject(), "", ByteArray(0))
        assertTrue(ModelSlot("test", "default", InputSchema("test", 1, 4), 2, emptyList()).accepts(file))
        assertFalse(ModelSlot("test", "other", InputSchema("test", 1, 4), 2, emptyList()).accepts(file))
        assertFalse(ModelSlot("test", "default", InputSchema("test", 2, 4), 2, emptyList()).accepts(file))
        assertFalse(ModelSlot("test", "default", InputSchema("test", 1, 4), 3, emptyList()).accepts(file))
    }
}
