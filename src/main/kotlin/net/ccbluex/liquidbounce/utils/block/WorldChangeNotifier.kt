/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.event.*

object WorldChangeNotifier : Listenable {
    private val subscriber = arrayListOf<WorldChangeSubscriber>()

    val chunkLoadHandler = handler<ChunkLoadEvent> { event ->
        val region = Region.fromChunkPosition(event.x, event.z)

        notifyAllSubscribers {
            it.invalidateChunk(event.x, event.z, true)
            it.invalidate(region, true)
        }
    }

    val chunkUnloadHandler = handler<ChunkUnloadEvent> { event ->
        val region = Region.fromChunkPosition(event.x, event.z)

        notifyAllSubscribers {
            it.invalidateChunk(event.x, event.z, false)
            it.invalidate(region, false)
        }
    }

    val blockChangeEvent = handler<BlockChangeEvent> { event ->
        val region = Region.fromBlockPos(event.blockPos)

        notifyAllSubscribers {
            it.invalidateChunk(event.blockPos.x shr 4, event.blockPos.z shr 4, true)
            it.invalidate(region, true)
        }
    }

    val disconnectHandler = handler<WorldDisconnectEvent> { event ->
        notifyAllSubscribers { it.invalidateEverything() }
    }

    private fun notifyAllSubscribers(function: (WorldChangeSubscriber) -> Unit) {
        synchronized(this.subscriber) {
            this.subscriber.forEach {
                function(it)
            }
        }
    }

    fun subscribe(newSubscriber: WorldChangeSubscriber) {
        synchronized(subscriber) {
            if (this.subscriber.contains(newSubscriber)) {
                throw IllegalStateException("Subscriber already registered")
            }

            this.subscriber.add(newSubscriber)
        }
    }

    fun unsubscribe(oldSubscriber: WorldChangeSubscriber) {
        synchronized(subscriber) {
            this.subscriber.remove(oldSubscriber)
        }
    }

    interface WorldChangeSubscriber {
        /**
         * @param rescan false if the area was unloaded
         */
        fun invalidate(region: Region, rescan: Boolean)

        fun invalidateChunk(x: Int, z: Int, rescan: Boolean) {}

        /**
         * Unloads the world; no rescanning required
         */
        fun invalidateEverything()
    }
}
