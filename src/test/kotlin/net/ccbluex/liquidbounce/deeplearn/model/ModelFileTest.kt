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
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelFileTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `files round trip every float bit for bit`() {
        val original = model(mean = floatArrayOf(-0f, Float.NaN, Float.POSITIVE_INFINITY, 1.1f),
            scale = floatArrayOf(Float.NEGATIVE_INFINITY, 0.2f, Float.MIN_VALUE, Float.MAX_VALUE))
        val path = directory.resolve("model.${ModelFile.EXTENSION}")
        original.write(path)
        val restored = ModelFile.read(path)
        assertEquals(original.task, restored.task)
        assertEquals(original.variant, restored.variant)
        assertEquals(original.name, restored.name)
        assertEquals(original.provenance, restored.provenance)
        assertEquals(original.network.hidden, restored.network.hidden)
        assertEquals(original.network.outputs, restored.network.outputs)
        assertEquals(original.input.size, restored.input.size)
        assertContentEquals(original.normalization.mean.bits(), restored.normalization.mean.bits())
        assertContentEquals(original.normalization.scale.bits(), restored.normalization.scale.bits())
        assertEquals(original.normalization.limit, restored.normalization.limit)
        assertEquals(original.metadata, restored.metadata)
        assertContentEquals(original.parameters, restored.parameters)
        Files.list(directory).use { entries -> assertTrue(entries.noneMatch { it.toString().endsWith(".tmp") }) }
    }

    @Test
    fun `the same model always gives the same bytes`() {
        val path = directory.resolve("model.${ModelFile.EXTENSION}")
        model().write(path)
        assertContentEquals(model().bytes(), Files.readAllBytes(path))
        assertContentEquals(Files.readAllBytes(path), ModelFile.read(path).bytes())
    }

    @Test
    fun `corrupted and incomplete files are rejected`() {
        val bytes = model().bytes()
        val corrupted = bytes.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }
        assertFails { ModelFile.read(corrupted.inputStream()) }
        assertFails { ModelFile.read(zip("model.json" to description().toString().toByteArray())) }
        val future = description().apply { addProperty("format", ModelFile.FORMAT + 1) }
        assertFails {
            ModelFile.read(zip("model.json" to future.toString().toByteArray(), "parameters.bin" to ByteArray(8)))
        }
    }

    @Test
    fun `slots only accept models for their task, input and outputs`() {
        val slot = ModelSlot("test", "default", InputSchema("test", 1, SIZE), 2)
        assertTrue(slot.accepts(model()))
        assertFalse(ModelSlot("test", "other", InputSchema("test", 1, SIZE), 2).accepts(model()))
        assertFalse(ModelSlot("test", "default", InputSchema("test", 2, SIZE), 2).accepts(model()))
        assertFalse(ModelSlot("test", "default", InputSchema("test", 1, SIZE), 3).accepts(model()))
    }

    private fun model(
        mean: FloatArray = FloatArray(SIZE) { it * 0.5f },
        scale: FloatArray = FloatArray(SIZE) { 1f + it },
    ) = ModelFile(
        "test", "default", "unit", InputSchema("test", 1, SIZE), NetworkSpec(listOf(8, 8), 2),
        InputNormalization(mean, scale), JsonObject().apply { addProperty("cap", 1.5f) }, "seed=1",
        Random(1).nextBytes(64),
    )

    private fun description(): JsonObject = ZipInputStream(model().bytes().inputStream()).use { zip ->
        generateSequence { zip.nextEntry }.first { it.name == "model.json" }
        JsonParser.parseString(zip.readAllBytes().decodeToString()).asJsonObject
    }

    private fun zip(vararg entries: Pair<String, ByteArray>) = ByteArrayOutputStream().also { bytes ->
        ZipOutputStream(bytes).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
    }.toByteArray().inputStream()

    private fun FloatArray.bits() = map { it.toRawBits() }

    private companion object {
        const val SIZE = 4
    }
}
