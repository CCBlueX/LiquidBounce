/*
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 - 2021 LabyMedia and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.ccbluex.liquidbounce.render.ultralight.filesystem

import com.labymedia.ultralight.plugin.filesystem.UltralightFileSystem
import net.ccbluex.liquidbounce.LiquidBounce.logger
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.*

/**
 * Ultralight browser file system
 */
class BrowserFileSystem : UltralightFileSystem {

    // Dumb implementation of a counter, but this will probably always be enough...
    // unless you have 9,223,372,036,854,775,807 files open. Please reconsider your application then!
    private var nextFileHandle: Long = 0

    // Map from handle to file channel, see class description for more details.
    private val openFiles = mutableMapOf<Long, FileChannel>()

    /**
     * This is called by Ultralight to check if a given file exists.
     *
     * Note that Ultralight might pass invalid paths, so check for them!
     *
     * @param path The path to check for a file at
     * @return `true` if the file exists, `false` otherwise
     */
    override fun fileExists(path: String): Boolean {
        log(false, "Checking if %s exists", path)
        val realPath = getPath(path)
        val exists = realPath != null && Files.exists(realPath)
        log(false, "%s %s", path, if (exists) "exists" else "does not exist")
        return exists
    }

    /**
     * Retrieves the file size for a given handle. Return -1 if the size can't be retrieved.
     *
     * @param handle The handle of the file to get the size of
     * @return The size of the opened handle, or `-1`, if the size could not be determined
     */
    override fun getFileSize(handle: Long): Long {
        log(false, "Retrieving file size of handle %d", handle)
        val channel = openFiles[handle]
        return if (channel == null) {
            // Should technically never occur unless Ultralight messed up
            log(true, "Failed to retrieve file size of handle %d, it was invalid", handle)
            -1
        } else {
            try {
                val size = channel.size()
                log(false, "File size of handle %d is %d", handle, size)
                size
            } catch (e: IOException) {
                log(true, "Exception while retrieving size of handle %d", handle)
                e.printStackTrace()
                -1
            }
        }
    }

    /**
     * Retrieves the mime type of a given file. Ultralight needs this in order to determine how to load content.
     *
     * @param path The path to check the mime type for
     * @return The mime type of the file at the given path, or `null`, if the mime type could not be determined
     */
    override fun getFileMimeType(path: String): String? {
        log(false, "Retrieving mime type of %s", path)
        val realPath = getPath(path)
        if (realPath == null) {
            // Ultralight requested an invalid path
            log(true, "Failed to retrieve mime type of %s, path was invalid", path)
            return null
        }
        return try {
            // Retrieve the mime type and log it
            val mimeType = Files.probeContentType(realPath)
            log(false, "Mime type of %s is %s", path, mimeType)
            mimeType
        } catch (e: IOException) {
            log(true, "Exception while retrieving mime type of %s", path)
            e.printStackTrace()
            null
        }
    }

    /**
     * Opens a file at the given location. Ultralight calls this when it needs to read files. Currently the parameter
     * `openForWriting` is always `false`, and a `write` method is missing from Ultralight as well.
     *
     * @param path           The path of the file to open
     * @param openForWriting Whether the file should be opened for writing
     * @return A handle to the opened file, or [.INVALID_FILE_HANDLE] if the file could not be opened
     */
    override fun openFile(path: String, openForWriting: Boolean): Long {
        log(false, "Opening file %s for %s", path, if (openForWriting) "writing" else "reading")
        val realPath = getPath(path)
        if (realPath == null) {
            log(true, "Failed to open %s, the path is invalid", path)
            return UltralightFileSystem.INVALID_FILE_HANDLE
        }

        val channel: FileChannel = try {
            // Actual open operation
            FileChannel.open(realPath, if (openForWriting) StandardOpenOption.WRITE else StandardOpenOption.READ)
        } catch (e: IOException) {
            log(true, "Exception while opening %s", path)
            e.printStackTrace()
            return UltralightFileSystem.INVALID_FILE_HANDLE
        }
        if (nextFileHandle == UltralightFileSystem.INVALID_FILE_HANDLE) {
            // Increment the handle number
            nextFileHandle = UltralightFileSystem.INVALID_FILE_HANDLE + 1
        }

        // Map the give handle
        val handle = nextFileHandle++
        openFiles[handle] = channel
        log(false, "Opened %s as handle %d", path, handle)
        return handle
    }

    /**
     * Closes the given handle. This is called by Ultralight when a file is no longer needed and its resources can be
     * disposed.
     *
     * @param handle The handle of the file to close
     */
    override fun closeFile(handle: Long) {
        log(false, "Closing handle %d", handle)
        val channel = openFiles[handle]
        if (channel != null) {
            try {
                channel.close()
                log(false, "Handle %d has been closed", handle)
            } catch (e: IOException) {
                log(true, "Exception while closing handle %d", handle)
                e.printStackTrace()
            } finally {
                openFiles.remove(handle)
            }
        } else {
            log(false, "Failed to close handle %d, it was invalid", handle)
        }
    }

    /**
     * Called by Ultralight when a chunk of data needs to be read from the file. Note that this may be called
     * multiple times on the same handle. When called on the same handle, the reader position needs to be kept, as
     * Ultralight expects the read to continue from the position where it was left of.
     *
     *
     * It currently is not possible to read files which sizes are greater than the integer limit because a
     * [ByteBuffer] is used as output. This is a bug in Ultralight Java and not Ultralight, however, due to the
     * low chances of that ever becoming an issue and the complexity of figuring out a proper solution, this is marked
     * as TODO.
     *
     * @param handle The handle of the file to read
     * @param data   Buffer to write read data into
     * @param length The amount of bytes to read from the file
     * @return The amount of bytes read from the file
     */
    override fun readFromFile(handle: Long, data: ByteBuffer, length: Long): Long {
        log(false, "Trying to read %d bytes from handle %d", length, handle)
        val channel = openFiles[handle]
        if (channel == null) {
            log(true, "Failed to read %d bytes from handle %d, it was invalid", length, handle)
            return -1
        }
        if (length > Int.MAX_VALUE) {
            log(true, "Failed to read %d bytes from handle %d, the size exceeded the limit", length, handle)
            // Not supported yet, marked as TODO
            // You should not throw Java exceptions into native code, so use it for getting a stacktrace and return -1
            UnsupportedOperationException().printStackTrace()
            return -1
        }
        return try {
            val read = channel.read(data.slice().limit(length.toInt()) as ByteBuffer).toLong()
            log(false, "Read %d bytes out of %d requested from handle %d", read, length, handle)
            read
        } catch (e: IOException) {
            log(true, "Exception occurred while reading %d bytes from handle %d", length, handle)
            e.printStackTrace()
            -1
        }
    }

    /**
     * Helper method to scratch malformed paths
     *
     * @param path The path to convert to an NIO path
     * @return The converted path, or `null`, if the path failed to convert
     */
    private fun getPath(path: String): Path? {
        return try {
            Paths.get(path)
        } catch (e: InvalidPathException) {
            null
        }
    }

    /**
     * Logs a message to the console.
     *
     * @param error Whether this is an error message
     * @param fmt   The format string
     * @param args  Arguments to format the string with
     */
    private fun log(error: Boolean, fmt: String, vararg args: Any) {
        val message = String.format(fmt, *args)
        if (error) {
            logger.error("[ERROR/FileSystem] $message")
        } else {
            logger.debug("[INFO/FileSystem] $message")
        }
    }
}
