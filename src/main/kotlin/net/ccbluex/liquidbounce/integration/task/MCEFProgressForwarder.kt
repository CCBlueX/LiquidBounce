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

package net.ccbluex.liquidbounce.integration.task

import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.mcef.listeners.MCEFProgressListener

class MCEFProgressForwarder(val task: Task) : MCEFProgressListener {

    /**
     * Progress update for general tasks
     *
     * @param task Task name
     * @param progress Progress
     */
    @Suppress("EmptyFunctionBlock")
    override fun onProgressUpdate(task: String, progress: Float) {}

    /**
     * If everything is complete
     */
    @Suppress("EmptyFunctionBlock")
    override fun onComplete() {}

    /**
     * File download or extraction start
     * @param taskName Task name
     */
    override fun onFileStart(taskName: String) {
        task.getOrCreateFileTask(taskName)
    }

    /**
     * File download or extraction progress
     * @param taskName Task name
     * @param bytesRead Bytes read
     * @param contentLength Total bytes
     * @param done Is download or extraction done
     */
    override fun onFileProgress(taskName: String, bytesRead: Long, contentLength: Long, done: Boolean) {
        task.getOrCreateFileTask(taskName).update(bytesRead, contentLength)
    }

    /**
     * File download or extraction end
     * @param taskName Task name
     */
    override fun onFileEnd(taskName: String) {
        task.getOrCreateFileTask(taskName).isCompleted = true
    }

}
