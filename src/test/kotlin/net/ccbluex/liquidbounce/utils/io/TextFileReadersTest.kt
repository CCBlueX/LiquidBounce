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
package net.ccbluex.liquidbounce.utils.io

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextFileReadersTest {

    private fun textFile(dir: Path, name: String, content: String): Path =
        dir.resolve(name).also { Files.writeString(it, content) }

    private fun emptyFile(dir: Path, name: String): Path =
        dir.resolve(name).also { Files.createFile(it) }

    @Test
    fun `decodes code points across a chunk boundary`(@TempDir dir: Path) {
        val prefix = "a".repeat(DEFAULT_BUFFER_SIZE - 1)
        val file = textFile(dir, "boundary.txt", "${prefix}中")

        CodePointReader(file, cyclic = false).use { reader ->
            repeat(prefix.length) { assertEquals('a'.code, reader.nextInt()) }
            assertTrue(reader.hasNext())
            assertEquals('中'.code, reader.nextInt())
            assertFalse(reader.hasNext())
        }
    }

    @Test
    fun `wraps around the file while cyclic`(@TempDir dir: Path) {
        val file = textFile(dir, "cyclic.txt", "ab")

        CodePointReader(file, cyclic = true).use { reader ->
            assertEquals(listOf('a', 'b', 'a', 'b', 'a'), List(5) { reader.nextInt().toChar() })
        }
    }

    @Test
    fun `ends at the end of the file while not cyclic`(@TempDir dir: Path) {
        val file = textFile(dir, "plain.txt", "hi")

        CodePointReader(file, cyclic = false).use { reader ->
            assertEquals('h'.code, reader.nextInt())
            assertEquals('i'.code, reader.nextInt())
            assertFalse(reader.hasNext())
        }
    }

    @Test
    fun `reads content appended to a cycling file`(@TempDir dir: Path) {
        val file = textFile(dir, "growing.txt", "a")

        CodePointReader(file, cyclic = true).use { reader ->
            assertEquals('a'.code, reader.nextInt())
            Files.writeString(file, "abc")
            assertEquals(listOf('b', 'c', 'a'), List(3) { reader.nextInt().toChar() })
        }
    }

    @Test
    fun `ignores a truncated trailing sequence`(@TempDir dir: Path) {
        val file = dir.resolve("truncated.txt")
        Files.write(file, byteArrayOf('a'.code.toByte(), 0xC3.toByte()))

        CodePointReader(file, cyclic = false).use { reader ->
            assertEquals('a'.code, reader.nextInt())
            assertFalse(reader.hasNext())
        }
    }

    @Test
    fun `does not loop forever on an empty file`(@TempDir dir: Path) {
        val file = emptyFile(dir, "empty.txt")

        CodePointReader(file, cyclic = true).use { reader ->
            assertFalse(reader.hasNext())
        }
    }

    @Test
    fun `releases the file handle on close`(@TempDir dir: Path) {
        val file = textFile(dir, "handle.txt", "x")

        CodePointReader(file, cyclic = false).use { it.nextInt() }

        Files.delete(file)
    }

    @Test
    fun `reads utf8 lines at their offsets`(@TempDir dir: Path) {
        val file = textFile(dir, "lines.txt", "第一行\r\nsecond\nlast\n")

        val starts = scanLineStarts(file)

        val lines = List(starts.size) { readUtf8LineAt(file, starts.getLong(it)) }
        assertEquals(listOf("第一行", "second", "last"), lines)
    }

    @Test
    fun `reports no line for an empty file`(@TempDir dir: Path) {
        val file = emptyFile(dir, "no-lines.txt")

        assertTrue(scanLineStarts(file).isEmpty)
    }

}
