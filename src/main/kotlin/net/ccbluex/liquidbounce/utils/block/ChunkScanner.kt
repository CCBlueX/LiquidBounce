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
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.chunk.WorldChunk
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

object ChunkScanner : Listenable {
    private val subscriber = arrayListOf<BlockChangeSubscriber>()

    private val loadedChunks = hashSetOf<ChunkLocation>()

    val chunkLoadHandler = handler<ChunkLoadEvent> { event ->
        val chunk = mc.world!!.getChunk(event.x, event.z)

        ChunkScannerThread.enqueueChunkUpdate(ChunkScannerThread.UpdateRequest.ChunkUpdateRequest(chunk))

        this.loadedChunks.add(ChunkLocation(event.x, event.z))
    }

    val chunkUnloadHandler = handler<ChunkUnloadEvent> { event ->
        ChunkScannerThread.enqueueChunkUpdate(ChunkScannerThread.UpdateRequest.ChunkUnloadRequest(event.x, event.z))

        this.loadedChunks.remove(ChunkLocation(event.x, event.z))
    }

    val blockChangeEvent = handler<BlockChangeEvent> { event ->
        ChunkScannerThread.enqueueChunkUpdate(ChunkScannerThread.UpdateRequest.BlockUpdateEvent(event.blockPos, event.newState))
    }

    val disconnectHandler = handler<WorldDisconnectEvent> { event ->
        synchronized(this) {
            this.subscriber.forEach(BlockChangeSubscriber::clearAllChunks)
        }

        this.loadedChunks.clear()
    }

    fun subscribe(newSubscriber: BlockChangeSubscriber) {
        if (this.subscriber.contains(newSubscriber)) {
            throw IllegalStateException("Subscriber already registered")
        }

        this.subscriber.add(newSubscriber)

        val world = mc.world ?: return

        logger.debug("Scanning ${this.loadedChunks.size} chunks for ${newSubscriber.javaClass.simpleName}")

        for (loadedChunk in this.loadedChunks) {
            ChunkScannerThread.enqueueChunkUpdate(
                ChunkScannerThread.UpdateRequest.ChunkUpdateRequest(
                    world.getChunk(
                        loadedChunk.x,
                        loadedChunk.z
                    ),
                    newSubscriber
                )
            )
        }
    }

    fun unsubscribe(oldSubscriber: BlockChangeSubscriber) {
        this.subscriber.remove(oldSubscriber)

        synchronized(this) {
            oldSubscriber.clearAllChunks()
        }
    }

    object ChunkScannerThread {
        val chunkUpdateQueue = ArrayBlockingQueue<UpdateRequest>(600)

        private val thread = thread {
            while (true) {
                try {
                    val chunkUpdate = this.chunkUpdateQueue.take()

                    synchronized(ChunkScanner) {
                        when (chunkUpdate) {
                            is UpdateRequest.ChunkUpdateRequest -> scanChunk(chunkUpdate)
                            is UpdateRequest.ChunkUnloadRequest -> removeMarkedBlocksFromChunk(chunkUpdate.x, chunkUpdate.z)
                            is UpdateRequest.BlockUpdateEvent -> {
                                for (sub in subscriber) {
                                    sub.recordBlock(chunkUpdate.blockPos, chunkUpdate.newState, false)
                                }
                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }

        fun enqueueChunkUpdate(request: UpdateRequest) {
            this.chunkUpdateQueue.put(request)
        }

        /**
         * Scans the chunks for a block
         */
        private fun scanChunk(request: UpdateRequest.ChunkUpdateRequest) {
            val chunk = request.chunk

            if (chunk.isEmpty) {
                return
            }

            val start = System.nanoTime()

            for (x in 0 until 16) {
                for (y in 0 until chunk.height) {
                    for (z in 0 until 16) {
                        val pos = BlockPos(x + chunk.pos.startX, y, z + chunk.pos.startZ)
                        val blockState = chunk.getBlockState(pos)

                        if (request.singleSubscriber == null) {
                            for (sub in subscriber) {
                                sub.recordBlock(pos, blockState, true)
                            }
                        } else {
                            request.singleSubscriber.recordBlock(pos, blockState, true)
                        }
                    }
                }
            }

            logger.debug("Scanning chunk ${chunk.pos.x} ${chunk.pos.x} took ${(System.nanoTime() - start) / 1000}us")
        }

        private fun removeMarkedBlocksFromChunk(x: Int, z: Int) {
            subscriber.forEach { it.clearChunk(x, z) }
        }

        fun stopThread() {
            this.thread.interrupt()
        }

        sealed class UpdateRequest {
            class ChunkUpdateRequest(val chunk: WorldChunk, val singleSubscriber: BlockChangeSubscriber? = null) : UpdateRequest()
            class ChunkUnloadRequest(val x: Int, val z: Int) : UpdateRequest()
            class BlockUpdateEvent(val blockPos: BlockPos, val newState: BlockState) : UpdateRequest()
        }
    }

    interface BlockChangeSubscriber {
        /**
         * Registers a block update and asks the subscriber to make a decision about what should be done.
         *
         * @param cleared true if the section the block is in was already cleared
         */
        fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean)
        fun clearChunk(x: Int, z: Int)
        fun clearAllChunks()
    }

    data class ChunkLocation(val x: Int, val z: Int)
}
