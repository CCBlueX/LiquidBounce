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
import net.ccbluex.fastutil.forEachLong
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.joinAll
import net.minecraft.block.BlockState
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.WorldChunk
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.BiConsumer
import kotlin.system.measureNanoTime
import kotlin.time.measureTime

object ChunkScanner : EventListener, MinecraftShortcuts {

    init {
        ChunkScannerThread
    }

    private val subscribers = CopyOnWriteArrayList<BlockChangeSubscriber>()

    private val loadedChunks = LongOpenHashSet()

    @Suppress("unused")
    private val chunkLoadHandler = handler<ChunkLoadEvent> { event ->
        val chunk = world.getChunk(event.x, event.z)

        ChunkScannerThread.process(UpdateRequest.ChunkLoad(chunk))

        this.loadedChunks.add(ChunkPos.toLong(event.x, event.z))
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is BlockUpdateS2CPacket -> ChunkScannerThread.process(
                UpdateRequest.BlockUpdate(packet.pos, packet.state)
            )

            // All updates are in one section
            is ChunkDeltaUpdateS2CPacket -> ChunkScannerThread.process(
                UpdateRequest.ChunkSectionUpdate(packet)
            )

            is UnloadChunkS2CPacket -> ChunkScannerThread.process(
                UpdateRequest.ChunkUnload(packet.pos)
            )
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        ChunkScannerThread.cancelCurrentJobs()
        subscribers.forEach(BlockChangeSubscriber::clearAllChunks)
        loadedChunks.clear()
    }

    fun subscribe(newSubscriber: BlockChangeSubscriber) {
        if (!this.subscribers.addIfAbsent(newSubscriber)) {
            error("Subscriber ${newSubscriber.javaClass.simpleName} already registered")
        }

        val world = mc.world ?: return
        if (this.loadedChunks.isEmpty()) return

        logger.info("Scanning ${this.loadedChunks.size} chunks for ${newSubscriber.javaClass.simpleName}")

        val chunks = this.loadedChunks.mapToArray { longChunkPos ->
            world.getChunk(
                ChunkPos.getPackedX(longChunkPos),
                ChunkPos.getPackedZ(longChunkPos)
            )
        }

        ChunkScannerThread.process(
            UpdateRequest.NewSubscriber(newSubscriber, chunks)
        )
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

        private val scope = CoroutineScope(dispatcher + worldJob + CoroutineExceptionHandler { context, throwable ->
            if (throwable !is CancellationException) {
                logger.warn("Chunk update error", throwable)
            }
        })

        /**
         * Shared cache for [scope]
         */
        private val mutable = ThreadLocal.withInitial(BlockPos::Mutable)

        fun process(request: UpdateRequest) {
            if (subscribers.isEmpty()) return

            scope.launch {
                // Process the update request
                when (request) {
                    is UpdateRequest.NewSubscriber -> scanChunksForNewSubscriber(request)
                    is UpdateRequest.ChunkLoad -> scanChunk(request)

                    is UpdateRequest.ChunkSectionUpdate -> request.packet.visitUpdates { blockPos, state ->
                        subscribers.forEach {
                            it.recordBlock(blockPos, state)
                        }
                    }

                    is UpdateRequest.ChunkUnload -> subscribers.forEach {
                        it.clearChunk(request.pos)
                    }

                    is UpdateRequest.BlockUpdate -> subscribers.forEach {
                        it.recordBlock(request.blockPos, request.newState)
                    }
                }
            }
        }

        /**
         * Cancel all existing enqueue(emit) jobs and scanner jobs
         */
        fun cancelCurrentJobs() {
            worldJob.cancelChildren()
        }

        /**
         * Scans loaded chunks for new subscriber
         */
        private suspend fun CoroutineScope.scanChunksForNewSubscriber(request: UpdateRequest.NewSubscriber) {
            val duration = measureTime {
                request.chunks.forEach { chunk ->
                    if (!chunk.isEmpty) {
                        request.subscriber.chunkUpdate(chunk)
                    }
                }
                if (request.subscriber.shouldCallRecordBlockOnChunkUpdate) {
                    request.chunks.forEach { chunk ->
                        if (!chunk.isEmpty) {
                            scanChunkSections(chunk) { pos, state ->
                                request.subscriber.recordBlock(pos, state)
                            }
                        }
                    }
                }
            }

            logger.info("Scanning chunks for ${request.subscriber} took ${duration.inWholeMicroseconds}us")
        }

        /**
         * Scans single new chunk
         */
        private suspend fun CoroutineScope.scanChunk(request: UpdateRequest.ChunkLoad) {
            val chunk = request.chunk

            if (chunk.isEmpty) {
                return
            }

            val duration = measureTime {
                subscribers.mapToArray {
                    launch { it.chunkUpdate(chunk) }
                }.joinAll()

                // Contains all subscriber that want recordBlock called on a chunk update
                val subscribersForRecordBlock = subscribers.filter {
                    it.shouldCallRecordBlockOnChunkUpdate
                }

                if (subscribersForRecordBlock.isEmpty()) {
                    return@measureTime
                }

                scanChunkSections(chunk) { pos, state ->
                    subscribersForRecordBlock.forEach { it.recordBlock(pos, state) }
                }
            }

            logger.info("Scanning chunk (${chunk.pos.x}, ${chunk.pos.z}) took ${duration.inWholeMicroseconds}us")
        }

        /**
         * @see WorldChunk.getBlockState
         */
        private suspend fun CoroutineScope.scanChunkSections(
            chunk: WorldChunk,
            action: BiConsumer<BlockPos, BlockState>
        ) {
            // 0 rangeTo chunk.highestNonEmptySection
            Array(chunk.highestNonEmptySection + 1) { sectionIndex ->
                launch {
                    val startX = chunk.pos.startX
                    val startZ = chunk.pos.startZ
                    val blockPos = mutable.get()
                    val section = chunk.getSection(sectionIndex)

                    for (sectionY in 0..15) {
                        // index == (y >> 4) - (bottomY >> 4)
                        val y = (sectionIndex + (chunk.bottomY shr 4)) shl 4 or sectionY
                        for (x in 0..15) {
                            for (z in 0..15) {
                                val blockState = section.getBlockState(x, sectionY, z)
                                val pos = blockPos.set(startX or x, y, startZ or z)
                                action.accept(pos, blockState)
                            }
                        }
                    }
                }
            }.joinAll()
        }

        fun stopThread() {
            worldJob.cancel()
            logger.info("Stopped Chunk Scanner Thread!")
        }
    }

    sealed interface UpdateRequest {
        class NewSubscriber(val subscriber: BlockChangeSubscriber, val chunks: Array<out WorldChunk>) : UpdateRequest

        class ChunkLoad(val chunk: WorldChunk) : UpdateRequest

        class ChunkSectionUpdate(val packet: ChunkDeltaUpdateS2CPacket) : UpdateRequest

        class ChunkUnload(val pos: ChunkPos) : UpdateRequest

        class BlockUpdate(val blockPos: BlockPos, val newState: BlockState) : UpdateRequest
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
         */
        fun recordBlock(pos: BlockPos, state: BlockState)

        /**
         * Is called when a chunk is initially loaded or entirely updated.
         */
        fun chunkUpdate(chunk: WorldChunk)

        fun clearChunk(pos: ChunkPos)

        fun clearAllChunks()
    }

}
