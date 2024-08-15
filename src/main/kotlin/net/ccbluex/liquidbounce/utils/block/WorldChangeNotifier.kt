/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler

object WorldChangeNotifier : Listenable {
    private val subscriber = arrayListOf<WorldChangeSubscriber>()

    @Suppress("unused")
    val chunkLoadHandler = handler<ChunkLoadEvent> { event ->
        val region = Region.fromChunkPosition(event.x, event.z)

        notifyAllSubscribers {
            it.invalidateChunk(event.x, event.z, true)
            it.invalidate(region, true)
        }
    }

    @Suppress("unused")
    val chunkDeltaUpdateHandler = handler<ChunkDeltaUpdateEvent> { event ->
        val region = Region.fromChunkPosition(event.x, event.z)

        notifyAllSubscribers {
            it.invalidateChunk(event.x, event.z, true)
            it.invalidate(region, true)
        }
    }

    @Suppress("unused")
    val chunkUnloadHandler = handler<ChunkUnloadEvent> { event ->
        val region = Region.fromChunkPosition(event.x, event.z)

        notifyAllSubscribers {
            it.invalidateChunk(event.x, event.z, false)
            it.invalidate(region, false)
        }
    }

    @Suppress("unused")
    val blockChangeEvent = handler<BlockChangeEvent> { event ->
        val region = Region.fromBlockPos(event.blockPos)

        notifyAllSubscribers {
            it.invalidateChunk(event.blockPos.x shr 4, event.blockPos.z shr 4, true)
            it.invalidate(region, true)
        }
    }

    @Suppress("unused")
    val disconnectHandler = handler<DisconnectEvent> {
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
            check(!this.subscriber.contains(newSubscriber)) {
                "Subscriber already registered"
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
