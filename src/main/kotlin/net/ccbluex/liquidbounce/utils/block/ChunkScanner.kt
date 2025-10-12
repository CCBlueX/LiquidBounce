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
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
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
import java.util.function.Predicate
import kotlin.time.measureTime

object ChunkScanner : EventListener, MinecraftShortcuts {

    private val loadedChunks = LongOpenHashSet()

    private val threadLocalBlockPos = ThreadLocal.withInitial(BlockPos::Mutable)

    private val subscribers = CopyOnWriteArrayList<BlockChangeSubscriber>()

    fun subscribe(newSubscriber: BlockChangeSubscriber) {
        if (!this.subscribers.addIfAbsent(newSubscriber)) {
            error("Subscriber ${newSubscriber.debugName} already registered")
        }

        val world = mc.world ?: return
        if (this.loadedChunks.isEmpty()) return

        val chunkArray = this.loadedChunks.mapToArray { longChunkPos ->
            world.getChunk(
                ChunkPos.getPackedX(longChunkPos),
                ChunkPos.getPackedZ(longChunkPos)
            )
        }
        val chunks = ObjectArrayList.wrap(chunkArray)
        chunks.removeIf(Predicate(WorldChunk::isEmpty))
        if (chunks.isEmpty) return

        UpdateRequest.NewSubscriber(newSubscriber, chunks)
            .runAsync()
    }

    fun unsubscribe(oldSubscriber: BlockChangeSubscriber) {
        subscribers.remove(oldSubscriber)
        oldSubscriber.clearAllChunks()
    }

    @Suppress("unused")
    private val chunkLoadHandler = handler<ChunkLoadEvent>(READ_FINAL_STATE) { event ->
        val chunk = world.getChunk(event.x, event.z).takeUnless { it.isEmpty } ?: return@handler

        loadedChunks.add(ChunkPos.toLong(event.x, event.z))

        if (subscribers.isEmpty()) return@handler

        UpdateRequest.ChunkLoad(chunk).runAsync()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(READ_FINAL_STATE) { event ->
        if (subscribers.isEmpty() || event.isCancelled) return@handler

        when (val packet = event.packet) {
            is BlockUpdateS2CPacket ->
                UpdateRequest.BlockUpdate(packet.pos, packet.state).runAsync()

            // All updates are in one section
            is ChunkDeltaUpdateS2CPacket ->
                UpdateRequest.ChunkSectionUpdate(packet).runAsync()

            is UnloadChunkS2CPacket -> mc.execute {
                loadedChunks.remove(packet.pos.toLong())
                UpdateRequest.ChunkUnload(packet.pos).runAsync()
            }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent>(FIRST_PRIORITY) {
        cancelCurrentJobs()
        loadedChunks.clear()
        subscribers.forEach(BlockChangeSubscriber::clearAllChunks)
    }

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

    val scope = CoroutineScope(dispatcher + worldJob + CoroutineExceptionHandler { context, throwable ->
        if (throwable !is CancellationException) {
            logger.warn("Chunk update error", throwable)
        }
    })

    /**
     * Cancel all existing enqueue(emit) jobs and scanner jobs
     */
    fun cancelCurrentJobs() {
        worldJob.cancelChildren()
    }

    fun stopThread() {
        worldJob.cancel()
        logger.info("Stopped Chunk Scanner Thread!")
    }

    /**
     * @see WorldChunk.getBlockState
     */
    private suspend fun scanChunkSections(
        chunk: WorldChunk,
        action: BiConsumer<BlockPos, BlockState>
    ) {
        // 0 rangeTo chunk.highestNonEmptySection
        Array(chunk.highestNonEmptySection + 1) { sectionIndex ->
            scope.launch {
                val startX = chunk.pos.startX
                val startZ = chunk.pos.startZ
                val blockPos = threadLocalBlockPos.get()
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

    sealed interface UpdateRequest {
        fun runAsync() {
            scope.launch { run() }
        }

        suspend fun run()

        /**
         * Scans loaded chunks for new subscriber
         *
         * @param chunks should be non-empty
         */
        class NewSubscriber(val subscriber: BlockChangeSubscriber, val chunks: List<WorldChunk>) : UpdateRequest {
            override suspend fun run() {
                val duration = measureTime {
                    chunks.forEach {
                        subscriber.chunkUpdate(it)
                    }
                    if (subscriber.shouldCallRecordBlockOnChunkUpdate) {
                        chunks.forEach {
                            scanChunkSections(it) { pos, state ->
                                subscriber.recordBlock(pos, state, cleared = true)
                            }
                        }
                    }
                }

                logger.debug(
                    "Scanning ${chunks.size} chunks for ${subscriber.debugName} took ${duration.inWholeMicroseconds}us"
                )
            }
        }

        /**
         * Scans single new chunk
         *
         * @param chunk should be non-empty
         */
        class ChunkLoad(val chunk: WorldChunk) : UpdateRequest {
            override suspend fun run() {
                val duration = measureTime {
                    subscribers.mapToArray {
                        scope.launch { it.chunkUpdate(chunk) }
                    }.joinAll()

                    // Contains all subscriber that want recordBlock called on a chunk update
                    val subscribersForRecordBlock = subscribers.filter {
                        it.shouldCallRecordBlockOnChunkUpdate
                    }

                    if (subscribersForRecordBlock.isEmpty()) {
                        return@measureTime
                    }

                    scanChunkSections(chunk) { pos, state ->
                        subscribersForRecordBlock.forEach { it.recordBlock(pos, state, cleared = true) }
                    }
                }

                logger.debug(
                    "Scanning chunk (${chunk.pos.x}, ${chunk.pos.z}) took ${duration.inWholeMicroseconds}us"
                )
            }
        }

        class ChunkSectionUpdate(val packet: ChunkDeltaUpdateS2CPacket) : UpdateRequest {
            override suspend fun run() {
                packet.visitUpdates { blockPos, state ->
                    subscribers.forEach {
                        it.recordBlock(blockPos, state, cleared = false)
                    }
                }
            }
        }

        class ChunkUnload(val pos: ChunkPos) : UpdateRequest {
            override suspend fun run() {
                subscribers.forEach {
                    it.clearChunk(pos)
                }
            }
        }

        class BlockUpdate(val blockPos: BlockPos, val newState: BlockState) : UpdateRequest {
            override suspend fun run() {
                subscribers.forEach {
                    it.recordBlock(blockPos, newState, cleared = false)
                }
            }
        }
    }

    interface BlockChangeSubscriber {
        val debugName: String get() = javaClass.simpleName

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
         * @param pos Might be [BlockPos.Mutable]. Use copy if it needs to be saved.
         * @param state The new [BlockState] of [pos].
         * @param cleared If the block is in section already cleared. Or, does it not need to check existing records
         */
        fun recordBlock(pos: BlockPos, state: BlockState, cleared: Boolean)

        /**
         * Is called when a chunk is initially loaded or entirely updated.
         *
         * @param chunk a non-empty chunk
         */
        fun chunkUpdate(chunk: WorldChunk)

        fun clearChunk(pos: ChunkPos)

        fun clearAllChunks()
    }

}
