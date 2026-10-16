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

package net.ccbluex.liquidbounce.integration.task.type

import net.ccbluex.liquidbounce.features.addon.AddonApi
import java.io.InputStream
import java.io.OutputStream
import java.time.InstantSource
import java.time.temporal.ChronoUnit

/**
 * A task with IO progress tracking
 */
@AddonApi
class ResourceTask internal constructor(
    name: String,
    private val clock: InstantSource,
) : Task(name) {

    constructor(name: String) : this(name, InstantSource.system())

    var bytesRead = 0L
        private set
    var contentLength = 0L
        private set

    /**
     * Read speed in bytes per second, sampled over [SAMPLE_INTERVAL_MS] instead of averaged over
     * the whole task lifetime, so stalls and speed changes are actually visible.
     */
    @Volatile
    var speed = 0L
        private set

    private var lastSampleTime = clock.instant()
    private var lastSampleBytes = 0L

    @JvmOverloads
    fun update(bytesRead: Long, contentLength: Long, done: Boolean = false) {
        val now = clock.instant()
        val elapsed = lastSampleTime.until(now, ChronoUnit.MILLIS)

        when {
            done -> speed = 0L
            // A retried or resumed download restarts the byte counter, so restart sampling too
            bytesRead < lastSampleBytes -> {
                speed = 0L
                lastSampleTime = now
                lastSampleBytes = bytesRead
            }
            elapsed >= SAMPLE_INTERVAL_MS -> {
                speed = (bytesRead - lastSampleBytes) * 1000L / elapsed
                lastSampleTime = now
                lastSampleBytes = bytesRead
            }
        }

        this.bytesRead = bytesRead
        this.contentLength = contentLength
        this.progress = if (contentLength > 0) {
            bytesRead.toFloat() / contentLength.toFloat()
        } else {
            0f
        }
    }

    @JvmOverloads
    fun transferring(input: InputStream, output: OutputStream, contentLength: Long = -1) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            output.write(buffer, 0, bytesRead)
            total += bytesRead
            this.update(total, contentLength, false)
        }
        this.update(total, contentLength, true)
    }

    private companion object {
        const val SAMPLE_INTERVAL_MS = 500L
    }

}
