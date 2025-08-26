/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.File
import java.io.InputStream

/**
 * Extracts an [ArchiveInputStream] to a specified [folder]
 */
private fun ArchiveInputStream<*>.extractTo(folder: File) = use { ais ->
    if (!folder.exists()) {
        folder.mkdir()
    }

    while (true) {
        // Lunar Client uses a stone age version of Apache Commons Compress that does not have the nextEntry method.
        @Suppress("DEPRECATION")
        val entry = when (ais) {
            is TarArchiveInputStream -> ais.nextTarEntry
            is ZipArchiveInputStream -> ais.nextZipEntry
            else -> ais.nextEntry
        }  ?: break

        if (entry.isDirectory) {
            continue
        }

        val newFile = File(folder, entry.name).apply {
            parentFile?.mkdirs()
        }

        // Ensure the entry is within the target directory to prevent zip slip
        if (!newFile.canonicalPath.startsWith(folder.canonicalPath)) {
            throw SecurityException("Entry is outside of the target directory: ${entry.name}")
        }

        newFile.outputStream().buffered().use { ais.copyTo(it) }
    }
}

/**
 * Extracts a ZIP archive from an [InputStream] to a specified [folder] and close it
 */
fun extractZip(zipStream: InputStream, folder: File) =
    ZipArchiveInputStream(zipStream.buffered()).extractTo(folder)

/**
 * Extracts a ZIP file to a specified [folder]
 */
fun extractZip(zipFile: File, folder: File) = extractZip(zipFile.inputStream(), folder)

/**
 * Creates a ZIP file from multiple files
 */
fun Collection<File>.createZipArchive(file: File) {
    ZipArchiveOutputStream(file.outputStream().buffered()).use { aos ->
        for (item in this) {
            if (!item.isFile) continue

            aos.putArchiveEntry(ZipArchiveEntry(item, item.name))
            item.inputStream().buffered().use { it.copyTo(aos) }
            aos.closeArchiveEntry()
        }

        aos.finish()
    }
}
