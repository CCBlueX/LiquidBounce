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

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.InputStream
import java.nio.channels.Channels
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipExtensionsTest {

    /** Builds a zip in memory; a `null` content marks a directory entry (name must end with `/`). */
    private fun zip(vararg entries: Pair<String, String?>): InputStream {
        val buffer = SeekableInMemoryByteChannel()
        ZipArchiveOutputStream(buffer).use { aos ->
            for ((name, content) in entries) {
                aos.putArchiveEntry(ZipArchiveEntry(name))
                if (content != null) {
                    aos.write(content.toByteArray())
                }
                aos.closeArchiveEntry()
            }
        }
        return Channels.newInputStream(buffer)
    }

    private inline fun withTempDir(block: (Path) -> Unit) {
        val dir = createTempDirectory("zip-extract-test")
        try {
            block(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    // ----- happy path -----

    @Test
    fun `extracts nested files and creates directories`() {
        withTempDir { dir ->
            extractZip(zip("a.txt" to "hello", "sub/b.txt" to "world"), dir)
            assertEquals("hello", dir.resolve("a.txt").readText())
            assertEquals("world", dir.resolve("sub/b.txt").readText())
        }
    }

    @Test
    fun `empty zip extracts nothing`() {
        withTempDir { dir ->
            extractZip(zip(), dir)
            assertTrue(!dir.resolve("anything").exists())
        }
    }

    @Test
    fun `normalizes dot segments inside root`() {
        withTempDir { dir ->
            extractZip(zip("a/./b.txt" to "x"), dir)
            assertEquals("x", dir.resolve("a/b.txt").readText())
        }
    }

    @Test
    fun `collapses parent segments inside root`() {
        withTempDir { dir ->
            extractZip(zip("a/../b.txt" to "x"), dir)
            assertEquals("x", dir.resolve("b.txt").readText())
        }
    }

    @Test
    fun `creates empty directory entries`() {
        withTempDir { dir ->
            extractZip(zip("empty/" to null), dir)
            assertTrue(dir.resolve("empty").isDirectory())
        }
    }

    // ----- zip-slip rejection (cross-platform) -----

    @Test
    fun `rejects parent traversal`() {
        withTempDir { dir ->
            assertFailsWith<SecurityException> {
                extractZip(zip("../evil.txt" to "x"), dir)
            }
            assertTrue(!dir.resolve("evil.txt").exists())
        }
    }

    @Test
    fun `rejects deep parent traversal`() {
        withTempDir { dir ->
            assertFailsWith<SecurityException> {
                extractZip(zip("a/../../evil.txt" to "x"), dir)
            }
        }
    }

    @Test
    fun `rejects absolute path entry`() {
        withTempDir { dir ->
            assertFailsWith<SecurityException> {
                extractZip(zip("/etc/evil.txt" to "x"), dir)
            }
        }
    }

    @Test
    fun `rejects dot entry treated as file`() {
        withTempDir { dir ->
            assertFailsWith<SecurityException> {
                extractZip(zip("." to "x"), dir)
            }
        }
    }

    // ----- windows-specific entry forms (on unix these are plain file names) -----

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun `rejects root-relative backslash entry on windows`() {
        withTempDir { dir ->
            assertFailsWith<SecurityException> {
                extractZip(zip("\\evil.txt" to "x"), dir)
            }
        }
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun `rejects drive-letter absolute entry on windows`() {
        withTempDir { dir ->
            assertFailsWith<SecurityException> {
                extractZip(zip("C:\\evil.txt" to "x"), dir)
            }
        }
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun `rejects backslash parent traversal on windows`() {
        withTempDir { dir ->
            assertFailsWith<SecurityException> {
                extractZip(zip("..\\..\\evil.txt" to "x"), dir)
            }
        }
    }

    // ----- strict CREATE_NEW semantics -----

    @Test
    fun `duplicate entries throw FileAlreadyExistsException`() {
        withTempDir { dir ->
            assertFailsWith<FileAlreadyExistsException> {
                extractZip(zip("a.txt" to "1", "a.txt" to "2"), dir)
            }
        }
    }

    @Test
    fun `re-extracting over existing files throws`() {
        withTempDir { dir ->
            extractZip(zip("a.txt" to "1"), dir)
            assertFailsWith<FileAlreadyExistsException> {
                extractZip(zip("a.txt" to "2"), dir)
            }
        }
    }

    @EnabledOnOs(OS.WINDOWS)
    @Test
    fun `rejects escape through pre-existing junction`() {
        withTempDir { dir ->
            val real = dir.resolve("real").createDirectories()
            val folder = dir.resolve("folder").createDirectories()
            val proc = ProcessBuilder("cmd", "/c", "mklink", "/J", folder.resolve("link").toString(), real.toString())
                .redirectErrorStream(true)
                .start()
            proc.waitFor()
            assumeTrue(folder.resolve("link").exists(), "mklink /J failed")

            assertFailsWith<SecurityException> {
                extractZip(zip("link/evil.txt" to "x"), folder)
            }
            assertTrue(!real.resolve("evil.txt").exists(), "entry must not escape through the junction")
        }
    }

    @Test
    fun `rejects escape through pre-existing symlink`() {
        withTempDir { dir ->
            val real = dir.resolve("real").createDirectories()
            val folder = dir.resolve("folder").createDirectories()
            val link = folder.resolve("link")
            try {
                Files.createSymbolicLink(link, real)
            } catch (e: Exception) {
                assumeTrue(false, "cannot create symlink: ${e.message}")
                return@withTempDir
            }

            assertFailsWith<SecurityException> {
                extractZip(zip("link/evil.txt" to "x"), folder)
            }
            assertTrue(!real.resolve("evil.txt").exists(), "entry must not escape through the symlink")
        }
    }
}
