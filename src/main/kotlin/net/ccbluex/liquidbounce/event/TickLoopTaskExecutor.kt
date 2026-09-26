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
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.ReportedException
import org.slf4j.LoggerFactory

/**
 * Schedules tasks that must run inside [net.minecraft.client.Minecraft.tick].
 */
object TickLoopTaskExecutor {

    private val logger = LoggerFactory.getLogger("$CLIENT_NAME/TickLoopTaskExecutor")

    @Volatile
    var isInTickLoop = false
        private set

    private val pendingTasks = ArrayDeque<Runnable>()

    fun executeInTickLoop(runnable: Runnable) {
        if (isInTickLoop) {
            runnable.run()
            return
        }

        mc.execute {
            pendingTasks.addLast(runnable)
        }
    }

    fun onTickLoopStart() {
        isInTickLoop = true

        while (true) {
            val task = pendingTasks.removeFirstOrNull() ?: break
            try {
                task.run()
            } catch (e: ReportedException) {
                throw e
            } catch (t: Throwable) {
                logger.error("Unhandled exception thrown by tick-loop task", t)
            }
        }
    }

    fun onTickLoopCompleted() {
        isInTickLoop = false
    }

}
