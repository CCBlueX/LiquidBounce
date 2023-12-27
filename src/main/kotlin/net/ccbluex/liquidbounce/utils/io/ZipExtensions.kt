/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

fun extractZip(zipStream: InputStream, folder: File) {
    if (!folder.exists()) {
        folder.mkdir()
    }

    ZipInputStream(zipStream).use { zipInputStream ->
        var zipEntry = zipInputStream.nextEntry

        while (zipEntry != null) {
            if (zipEntry.isDirectory) {
                zipEntry = zipInputStream.nextEntry
                continue
            }

            val newFile = File(folder, zipEntry.name)
            File(newFile.parent).mkdirs()

            FileOutputStream(newFile).use {
                zipInputStream.copyTo(it)
            }
            zipEntry = zipInputStream.nextEntry
        }

        zipInputStream.closeEntry()
    }
}

fun extractZip(zipFile: File, folder: File) = extractZip(FileInputStream(zipFile), folder)
