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

import okio.BufferedSink
import okio.appendingSink
import okio.buffer
import java.io.File

/**
 * Appends to [file] through one open handle, taken on the first write and kept until [close].
 *
 * Every write is flushed, so the file can be read while it grows. A write that loses the race
 * against [close] is dropped instead of throwing.
 */
class AppendLog(private val file: File) : AutoCloseable {

    private var sink: BufferedSink? = null
    private var closed = false

    @Synchronized
    fun append(block: BufferedSink.() -> Unit) {
        if (closed) {
            return
        }

        val opened = sink ?: file.appendingSink().buffer().also { sink = it }
        opened.block()
        opened.flush()
    }

    fun appendLine(line: String) = append { writeUtf8(line).writeByte('\n'.code) }

    @Synchronized
    override fun close() {
        closed = true
        sink?.close()
    }

}
