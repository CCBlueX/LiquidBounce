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

import it.unimi.dsi.fastutil.io.FastBufferedInputStream
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.readAttributes

@Suppress("ThrowsCount")
private fun Path.createDirectoryNoFollow(relative: Path) {
    fun Path.readAttrsNoFollow(): BasicFileAttributes? =
        try {
            readAttributes(LinkOption.NOFOLLOW_LINKS)
        } catch (_: java.nio.file.NoSuchFileException) {
            null
        }

    var current = this
    for (part in relative) {
        current = current.resolve(part)
        val attrs = current.readAttrsNoFollow()
        when {
            attrs == null -> current.createDirectory()
            attrs.isSymbolicLink -> throw SecurityException("Symlink in extraction path: $current")
            attrs.isDirectory -> {}
            else -> throw java.nio.file.FileAlreadyExistsException(current.toString())
        }

        val after = current.readAttrsNoFollow()
            ?: throw SecurityException("Directory vanished: $current")
        if (!after.isDirectory) {
            throw SecurityException("Path component is not a real directory: $current")
        }
        // toRealPath() resolves any junction/symlink in the chain;
        // deviating from the lexical path means a link was followed
        if (current.toRealPath() != current) {
            throw SecurityException("Symlink in extraction path: $current")
        }
    }
}

/**
 * Extracts an [ArchiveInputStream] to a specified [folder] and closes it.
 */
@Suppress("CognitiveComplexMethod")
private fun ArchiveInputStream<*>.extractTo(folder: Path) = use { ais ->
    val destDir = folder.createDirectories().toRealPath()

    for (entry in ais) {
        if (entry is ZipArchiveEntry && entry.isUnixSymlink) {
            throw SecurityException("Refusing symlink entry: ${entry.name}")
        }

        val relative = destDir.fileSystem.getPath(entry.name)
        if (relative.isAbsolute) {
            throw SecurityException("Absolute entry path: ${entry.name}")
        }

        val target = destDir.resolve(entry.name).normalize()
        if (!target.startsWith(destDir) || target == destDir && !entry.isDirectory) {
            throw SecurityException("Entry is outside of the target directory: ${entry.name}")
        }

        if (entry.isDirectory) {
            destDir.createDirectoryNoFollow(destDir.relativize(target))
            continue
        }

        if (!ais.canReadEntryData(entry)) {
            continue
        }

        destDir.createDirectoryNoFollow(destDir.relativize(target.parent ?: destDir))
        FastBufferedOutputStream(
            target.outputStream(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)
        ).use { ais.transferTo(it) }
    }
}

/**
 * Extracts a ZIP archive from an [InputStream] to a specified [folder] and close it
 */
fun extractZip(zipStream: InputStream, folder: File) = extractZip(zipStream, folder.toPath())

/**
 * Extracts a ZIP archive from an [InputStream] to a specified [folder] and close it
 */
fun extractZip(zipStream: InputStream, folder: Path) =
    ZipArchiveInputStream(zipStream).extractTo(folder)

/**
 * Extracts a ZIP file to a specified [folder]
 */
fun extractZip(zipFile: File, folder: File) = extractZip(zipFile, folder.toPath())

/**
 * Extracts a ZIP file to a specified [folder]
 */
fun extractZip(zipFile: File, folder: Path) = extractZip(FastBufferedInputStream(zipFile.inputStream()), folder)

/**
 * Creates a ZIP file from multiple files (flatten)
 */
fun Collection<File>.createZipArchive(file: File) {
    ZipArchiveOutputStream(file).use { aos ->
        for (item in this) {
            if (!item.isFile || !item.canRead()) continue

            aos.putArchiveEntry(ZipArchiveEntry(item, item.name))
            aos.write(item)
            aos.closeArchiveEntry()
        }
    }
}
