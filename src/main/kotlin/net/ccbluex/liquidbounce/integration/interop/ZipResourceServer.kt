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
package net.ccbluex.liquidbounce.integration.interop

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import it.unimi.dsi.fastutil.bytes.ByteArrays
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipResourceServer(zipInputStream: InputStream) {

    private class Entry(val name: String, val data: ByteArray, val isDirectory: Boolean)

    private val entries = buildMap {
        ZipInputStream(zipInputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name.removePrefix("/").removePrefix("./")
                if (entry.isDirectory) {
                    put(name, Entry(name, ByteArrays.EMPTY_ARRAY, true))
                } else {
                    put(name, Entry(name, zis.readBytes(), false))
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun install(route: Route, rootPath: String) {
        route.get("$rootPath/{path...}") {
            val requestPath = call.parameters.getAll("path")?.joinToString("/")?.removePrefix("/") ?: ""

            val sanitizedPath = requestPath.replace("..", "")

            fun findEntry(targetPath: String) =
                entries[targetPath] ?: entries["./$targetPath"] ?: entries["/$targetPath"]

            fun isImplicitDirectory(targetPath: String): Boolean {
                val pathPrefix = if (targetPath.isEmpty()) "" else "$targetPath/"
                return entries.keys.any { key ->
                    key.startsWith(pathPrefix) && key != targetPath
                }
            }

            fun findIndexInDirectory(dirPath: String): Entry? {
                val indexPath = if (dirPath.isEmpty()) "index.html" else "$dirPath/index.html"
                return findEntry(indexPath)
            }

            val fragmentIndex = sanitizedPath.indexOf('#')
            val directoryPath = if (fragmentIndex != -1) {
                sanitizedPath.take(fragmentIndex).removeSuffix("/")
            } else {
                sanitizedPath.removeSuffix("/")
            }

            // Exact file match
            val exactMatch = findEntry(sanitizedPath)
            if (exactMatch != null && !exactMatch.isDirectory) {
                val contentType = ContentType.defaultForFilePath(exactMatch.name)
                call.respondBytes(exactMatch.data, contentType, HttpStatusCode.OK)
                return@get
            }

            // Directory / SPA handling
            val indexEntry = when {
                sanitizedPath.isEmpty() -> findIndexInDirectory("")
                sanitizedPath.endsWith("/") -> findIndexInDirectory(directoryPath)
                fragmentIndex != -1 -> findIndexInDirectory(directoryPath)
                isImplicitDirectory(sanitizedPath) -> findIndexInDirectory(sanitizedPath)
                else -> null
            }

            if (indexEntry != null && !indexEntry.isDirectory) {
                val contentType = ContentType.defaultForFilePath(indexEntry.name)
                call.respondBytes(indexEntry.data, contentType, HttpStatusCode.OK)
                return@get
            }

            call.notFound(sanitizedPath, "File not found in zip archive")
        }
    }
}
