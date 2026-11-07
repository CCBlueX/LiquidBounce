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
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class PathExtTest {

    @TempDir
    lateinit var folder: Path

    @Test
    fun `replaces an existing config without a separate delete`() {
        val target = folder.resolve("settings.json")
        val source = folder.resolve("settings.json.tmp")
        target.writeText("old settings")
        source.writeText("new settings")

        assertEquals(target, source.atomicMoveTo(target))

        assertEquals("new settings", target.readText())
        assertFalse(Files.exists(source))
    }

    @Test
    fun `missing temporary file does not remove the saved config`() {
        val target = folder.resolve("settings.json")
        target.writeText("saved settings")

        assertFailsWith<NoSuchFileException> {
            folder.resolve("settings.json.tmp").atomicMoveTo(target)
        }

        assertEquals("saved settings", target.readText())
    }

    @Test
    fun `first save moves to a new target`() {
        val target = folder.resolve("settings.json")
        val source = folder.resolve("settings.json.tmp")
        source.writeText("first settings")

        source.atomicMoveTo(target)

        assertEquals("first settings", target.readText())
        assertFalse(Files.exists(source))
    }

}
