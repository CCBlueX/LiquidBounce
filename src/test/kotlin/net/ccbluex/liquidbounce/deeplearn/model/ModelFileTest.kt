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
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.test.zip
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ModelFileTest {
    @Test
    fun `every field is read, non-finite floats from strings`() {
        val file = ModelFile.read(zip(ModelFile.DESCRIPTION to DESCRIPTION, ModelFile.PARAMETERS to PARAMETERS))
        assertEquals("test", file.task)
        assertEquals("default", file.variant)
        assertEquals("unit", file.name)
        assertEquals("seed=1", file.provenance)
        assertEquals(2, file.input.size)
        assertEquals(listOf(8, 8), file.network.hidden)
        assertEquals(NetworkSpec.Activation.TANH, file.network.activation)
        assertEquals(2, file.network.outputs)
        assertContentEquals(floatArrayOf(0.5f, -1f), file.normalization.mean)
        assertContentEquals(floatArrayOf(1f, 2f), file.normalization.scale)
        assertEquals(Float.POSITIVE_INFINITY, file.normalization.limit)
        assertEquals(1.5f, file.metadata["cap"].asFloat)
        assertContentEquals(PARAMETERS.toByteArray(), file.parameters)
    }

    @Test
    fun `corrupted and incomplete files are rejected`() {
        assertFails { ModelFile.read(zip(ModelFile.DESCRIPTION to DESCRIPTION)) }
        assertFails { ModelFile.read(zip(ModelFile.PARAMETERS to PARAMETERS)) }
        for (change in listOf<JsonObject.() -> Unit>(
            { addProperty("format", ModelFile.FORMAT + 1) },
            { getAsJsonObject("network").addProperty("activation", "sigmoid") },
            { getAsJsonObject("normalization").add("scale", JsonParser.parseString("[1.0, 0.0]")) },
            { getAsJsonObject("input").addProperty("size", 3) },
        )) {
            val broken = JsonParser.parseString(DESCRIPTION).asJsonObject.apply(change)
            assertFails {
                ModelFile.read(zip(ModelFile.DESCRIPTION to broken.toString(), ModelFile.PARAMETERS to PARAMETERS))
            }
        }
    }

    private companion object {
        const val PARAMETERS = "weights"
        const val DESCRIPTION = """
            {
              "format": 1, "task": "test", "variant": "default", "name": "unit",
              "input": { "schema": "test", "version": 1, "size": 2 },
              "network": { "type": "mlp", "hidden": [8, 8], "activation": "tanh", "outputs": 2 },
              "provenance": "seed=1", "metadata": { "cap": 1.5 },
              "normalization": { "limit": "Infinity", "mean": [0.5, -1.0], "scale": [1.0, 2.0] }
            }
        """
    }
}
