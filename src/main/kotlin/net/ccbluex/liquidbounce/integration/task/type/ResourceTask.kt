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
 *
 *
 */

package net.ccbluex.liquidbounce.integration.task.type

/**
 * A task with IO progress tracking
 */
class ResourceTask(
    name: String
) : Task(name) {

    var bytesRead = 0L
        private set
    var contentLength = 0L
        private set

    /**
     * Current read speed in bytes per second
     */
    val speed: Long
        get() {
            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
            return if (elapsedSeconds > 0) {
                (bytesRead / elapsedSeconds).toLong()
            } else {
                0
            }
        }

    fun update(bytesRead: Long, contentLength: Long) {
        this.bytesRead = bytesRead
        this.contentLength = contentLength
        this.progress = if (contentLength > 0) {
            bytesRead.toFloat() / contentLength.toFloat()
        } else {
            0f
        }
    }

}
