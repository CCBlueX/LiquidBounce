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
package net.ccbluex.liquidbounce.deeplearn.models

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull

class LegacyModelFileTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `the bundled models read without the engine`() {
        for (name in listOf("19kc8kp", "21kc11kp")) {
            val bytes = assertNotNull(javaClass.getResourceAsStream("/resources/liquidbounce/models/$name.params"))
                .use { it.readAllBytes() }
            val file = LegacyModelFile.read(bytes.inputStream(), name)
            assertEquals(6, file.input.size)
            assertEquals(LegacyModelFile.NETWORK, file.network)
            assertContentEquals(bytes.copyOfRange(HEADER, bytes.size), file.parameters)
        }
    }

    @Test
    fun `a model folder loads the parameters saved last`() {
        for (name in listOf("tf-0000.params", "tf-0002.params", "tf-0010.params", "notes.txt")) {
            Files.createFile(directory.resolve(name))
        }
        assertEquals(directory.resolve("tf-0010.params"), LegacyModelFile.latest(directory))
    }

    @Test
    fun `other files are rejected`() {
        assertFails { LegacyModelFile.read(ByteArray(HEADER).inputStream(), "zeros") }
    }

    private companion object {
        // DJL's header of a model with one input of shape (32, 6) and no properties
        const val HEADER = 64
    }
}
