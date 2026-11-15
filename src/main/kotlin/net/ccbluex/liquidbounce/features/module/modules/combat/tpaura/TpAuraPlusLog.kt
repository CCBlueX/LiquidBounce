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
package net.ccbluex.liquidbounce.features.module.modules.combat.tpaura

import net.ccbluex.liquidbounce.utils.client.mc
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The debug log of [ModuleTpAuraPlus]: every stage from routing to attacking, including why an engagement
 * ended, goes to `logs/tpauraplus.log` in the game directory. Lines are flushed right away, so the log
 * survives a crash.
 */
object TpAuraPlusLog {

    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private var writer: BufferedWriter? = null
    private var broken = false

    val isWriting: Boolean
        get() = writer != null

    fun open() {
        close()
        broken = false
        try {
            val dir = File(mc.gameDirectory, "logs")
            dir.mkdirs()
            writer = BufferedWriter(FileWriter(File(dir, "tpauraplus.log"), true))
        } catch (_: IOException) {
            broken = true
            writer = null
        }
    }

    fun close() {
        try {
            writer?.flush()
            writer?.close()
        } catch (_: IOException) {
            // Nothing to do about it.
        }
        writer = null
    }

    /** One line: `13:30:01.123 | t=1234 | event | key=value ...`. */
    @Synchronized
    fun log(tick: Int, event: String, details: String = "") {
        val writer = writer ?: return
        val line = buildString {
            append(LocalTime.now().format(clock))
            append(" | t=").append(tick)
            append(" | ").append(event)
            if (details.isNotEmpty()) {
                append(" | ").append(details)
            }
        }
        try {
            writer.write(line)
            writer.newLine()
            writer.flush()
        } catch (_: IOException) {
            if (!broken) {
                broken = true
                close()
            }
        }
    }

    /** Joins `key=value` pairs with spaces, skipping null values. */
    fun details(vararg pairs: Pair<String, Any?>): String =
        pairs.joinToString(" ") { (key, value) -> "$key=$value" }

}
