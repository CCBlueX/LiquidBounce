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
 */
package net.ccbluex.liquidbounce.utils.block

import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.getValue
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.WorldChunk
import java.util.concurrent.CopyOnWriteArrayList

object ChunkScanner : EventListener, MinecraftShortcuts {

    init {
        ChunkScannerThread
    }

    private val subscribers = CopyOnWriteArrayList<BlockChangeSubscriber>()

    private val loadedChunks = LongOpenHashSet()

    @Suppress("unused")
    private val chunkLoadHandler = handler<ChunkLoadEvent> { event ->
        val chunk = world.getChunk(event.x, event.z)

        ChunkScannerThread.enqueueChunkUpdate(ChunkScannerThread.UpdateRequest.ChunkUpdateRequest(chunk))

        this.loadedChunks.add(ChunkPos.toLong(event.x, event.z))
    }

    @Suppress("unused")
    private val chunkDeltaUpdateHandler = handler<ChunkDeltaUpdateEvent> { event ->
        val chunk = world.getChunk(event.x, event.z)
        ChunkScannerThread.enqueueChunkUpdate(ChunkScannerThread.UpdateRequest.ChunkUpdateRequest(chunk))
    }

    @Suppress("unused")
    private val chunkUnloadHandler = handler<ChunkUnloadEvent> { event ->
        ChunkScannerThread.enqueueChunkUpdate(ChunkScannerThread.UpdateRequest.ChunkUnloadRequest(event.x, event.z))

        this.loadedChunks.remove(ChunkPos.toLong(event.x, event.z))
    }

    @Suppress("unused")
    private val blockChangeEvent = handler<BlockChangeEvent> { event ->
        ChunkScannerThread.enqueueChunkUpdate(
            ChunkScannerThread.UpdateRequest.BlockUpdateEvent(
                event.blockPos,
                event.newState
            )
        )
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        ChunkScannerThread.cancelCurrentJobs()
        subscribers.forEach(BlockChangeSubscriber::clearAllChunks)
        loadedChunks.clear()
    }

    fun subscribe(newSubscriber: BlockChangeSubscriber) {
        check(newSubscriber !in this.subscribers) {
            "Subscriber ${newSubscriber.javaClass.simpleName} already registered"
        }

        subscribers.add(newSubscriber)

        val world = mc.world ?: return

        logger.debug("Scanning ${this.loadedChunks.size} chunks for ${newSubscriber.javaClass.simpleName}")

        with(this.loadedChunks.longIterator()) {
            while (hasNext()) {
                val longChunkPos = nextLong()
                ChunkScannerThread.enqueueChunkUpdate(
                    ChunkScannerThread.UpdateRequest.ChunkUpdateRequest(
                        world.getChunk(
                            ChunkPos.getPackedX(longChunkPos),
                            ChunkPos.getPackedZ(longChunkPos)
                        ),
                        newSubscriber
                    )
                )
            }
        }
    }

    fun unsubscribe(oldSubscriber: BlockChangeSubscriber) {
        subscribers.remove(oldSubscriber)
        oldSubscriber.clearAllChunks()
    }

    object ChunkScannerThread {

        /**
         * When the first request comes in, the dispatcher and the scope will be initialized,
         * and its parallelism cannot be modified
         */
        private val dispatcher = Dispatchers.Default
            .limitedParallelism((Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(2))

        /**
         * The parent job for the current client world.
         * All children will be cancelled on [WorldChangeEvent].
         */
        private val worldJob = SupervisorJob()

        private val scope = CoroutineScope(dispatcher + worldJob)

        private val eventFlow = MutableSharedFlow<UpdateRequest>()

        /**
         * Shared cache for [scope]
         */
        private val mutable by ThreadLocal.withInitial(BlockPos::Mutable)

        /**
         * A standalone [Job] to dispatch all [UpdateRequest] from [eventFlow]
         */
        private val collectorJob = scope.launch(Job()) {
            eventFlow.collect { chunkUpdate ->
                // Discard current request when world is null
                if (mc.world == null) {
                    delay(50L)
                    return@collect
                }

                // Process the update request
                launch {
                    try {
                        when (chunkUpdate) {
                            is UpdateRequest.ChunkUpdateRequest -> scanChunk(chunkUpdate)

                            is UpdateRequest.ChunkUnloadRequest -> subscribers.forEach {
                                it.clearChunk(chunkUpdate.x, chunkUpdate.z)
                            }

                            is UpdateRequest.BlockUpdateEvent -> subscribers.forEach {
                                it.recordBlock(chunkUpdate.blockPos, chunkUpdate.newState, cleared = false)
                            }
                        }
                    } catch (e: Throwable) {
                        logger.warn("Chunk update error", e)
                    }
                }
            }
        }

        fun enqueueChunkUpdate(request: UpdateRequest) {
            scope.launch {
                eventFlow.emit(request)
            }
        }

        /**
         * Cancel all existing enqueue(emit) jobs and scanner jobs
         */
        fun cancelCurrentJobs() {
            worldJob.cancelChildren()
        }

        /**
         * Scans the chunks for a block
         */
        private suspend fun scanChunk(request: UpdateRequest.ChunkUpdateRequest) {
            val chunk = request.chunk

            if (chunk.isEmpty) {
                return
            }

            val start = System.nanoTime()

            val currentSubscriber = request.singleSubscriber?.let { listOf(it) } ?: subscribers

            when (currentSubscriber.size) {
                0 -> return
                1 -> currentSubscriber.first().chunkUpdate(chunk.pos.x, chunk.pos.z)
                else -> currentSubscriber.map {
                    scope.launch { it.chunkUpdate(chunk.pos.x, chunk.pos.z) }
                }.joinAll()
            }

            // Contains all subscriber that want recordBlock called on a chunk update
            val subscribersForRecordBlock = currentSubscriber.filter {
                it.shouldCallRecordBlockOnChunkUpdate
            }.toTypedArray()

            if (subscribersForRecordBlock.isEmpty()) {
                return
            }

            val startX = chunk.pos.startX
            val startZ = chunk.pos.startZ

            /**
             * @see WorldChunk.getBlockState
             */
            (0..chunk.highestNonEmptySection).map { sectionIndex ->
                scope.launch {
                    val section = chunk.getSection(sectionIndex)
                    for (sectionY in 0..15) {
                        // index == (y >> 4) - (bottomY >> 4)
                        val y = (sectionIndex + (chunk.bottomY shr 4)) shl 4 or sectionY
                        for (x in 0..15) {
                            for (z in 0..15) {
                                val blockState = section.getBlockState(x, sectionY, z)
                                val pos = mutable.set(startX or x, y, startZ or z)
                                subscribersForRecordBlock.forEach { it.recordBlock(pos, blockState, cleared = true) }
                            }
                        }
                    }
                }
            }.joinAll()

            logger.debug("Scanning chunk (${chunk.pos.x}, ${chunk.pos.z}) took ${(System.nanoTime() - start) / 1000}us")
        }

        fun stopThread() {
            worldJob.cancel()
            collectorJob.cancel()
            logger.info("Stopped Chunk Scanner Thread!")
        }

        sealed interface UpdateRequest {
            class ChunkUpdateRequest(val chunk: WorldChunk, val singleSubscriber: BlockChangeSubscriber? = null) :
                UpdateRequest

            class ChunkUnloadRequest(val x: Int, val z: Int) : UpdateRequest

            class BlockUpdateEvent(val blockPos: BlockPos, val newState: BlockState) : UpdateRequest
        }
    }

    interface BlockChangeSubscriber {
        /**
         * If this is true [recordBlock] is called on chunk updates and on single block updates.
         * This might be inefficient for some modules, so they can choose to not call that method on chunk updates.
         */
        val shouldCallRecordBlockOnChunkUpdate: Boolean
            get() = true

        /**
         * Registers a block update and asks the subscriber to make a decision about what should be done.
         * This method must be **thread-safe**.
         *
         * @param pos DON'T directly save it to a container Property (Field in Java), save a copy instead
         * @param cleared true, if the section the block is in was already cleared
         */
        fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean)

        /**
         * Is called when a chunk is initially loaded or entirely updated.
         */
        fun chunkUpdate(x: Int, z: Int)
        fun clearChunk(x: Int, z: Int)
        fun clearAllChunks()
    }

}
