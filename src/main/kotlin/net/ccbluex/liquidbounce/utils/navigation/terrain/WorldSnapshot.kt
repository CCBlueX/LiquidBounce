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

package net.ccbluex.liquidbounce.utils.navigation.terrain

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongPredicate
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent
import net.ccbluex.liquidbounce.event.events.ChunkDeltaUpdateEvent
import net.ccbluex.liquidbounce.event.events.ChunkLoadEvent
import net.ccbluex.liquidbounce.event.events.ChunkUnloadEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min

/**
 * Block states copied from the client world, safe to read from any thread. Blocks outside the copied
 * region are unknown.
 */
class WorldSnapshot internal constructor(
    private val minY: Int,
    private val height: Int,
    private val sections: Long2ObjectOpenHashMap<PalettedContainer<BlockState>?>,
    private val borderMinX: Double,
    private val borderMaxX: Double,
    private val borderMinZ: Double,
    private val borderMaxZ: Double,
) : NavigationWorld {

    override fun isKnown(x: Int, y: Int, z: Int) =
        y >= minY && y < minY + height && sections.containsKey(sectionKey(x, y, z))

    override fun isInsideBorder(x: Double, z: Double) =
        x >= borderMinX && x < borderMaxX && z >= borderMinZ && z < borderMaxZ

    override fun getBlockState(pos: BlockPos): BlockState {
        if (pos.y < minY || pos.y >= minY + height) {
            return VOID_AIR
        }

        val key = sectionKey(pos.x, pos.y, pos.z)
        val states = sections.get(key) ?: return if (sections.containsKey(key)) AIR else VOID_AIR
        return states.get(pos.x and 15, pos.y and 15, pos.z and 15)
    }

    override fun getFluidState(pos: BlockPos): FluidState = getBlockState(pos).fluidState

    override fun getBlockEntity(pos: BlockPos): BlockEntity? = null

    override fun getHeight() = height

    override fun getMinY() = minY

    private companion object {
        val AIR: BlockState = Blocks.AIR.defaultBlockState()
        val VOID_AIR: BlockState = Blocks.VOID_AIR.defaultBlockState()

        fun sectionKey(x: Int, y: Int, z: Int) = SectionPos.asLong(x shr 4, y shr 4, z shr 4)
    }

}

/**
 * A block box a search may look at.
 */
data class NavigationRegion(
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int,
) {

    operator fun contains(other: NavigationRegion) =
        other.minX >= minX && other.minY >= minY && other.minZ >= minZ &&
            other.maxX <= maxX && other.maxY <= maxY && other.maxZ <= maxZ

    companion object {

        /**
         * Every block a route of at most [maxLength] from [start] to one of [ends] can pass, with at most
         * [verticalMargin] blocks above or below them.
         */
        fun around(start: Vec3, ends: Collection<Vec3>, maxLength: Double, verticalMargin: Int): NavigationRegion {
            var minX = start.x
            var minY = start.y
            var minZ = start.z
            var maxX = start.x
            var maxY = start.y
            var maxZ = start.z
            var closest = Double.MAX_VALUE

            for (end in ends) {
                minX = min(minX, end.x)
                minY = min(minY, end.y)
                minZ = min(minZ, end.z)
                maxX = max(maxX, end.x)
                maxY = max(maxY, end.y)
                maxZ = max(maxZ, end.z)
                closest = min(closest, start.distanceTo(end))
            }

            val detour = if (closest == Double.MAX_VALUE) maxLength else max(0.0, (maxLength - closest) / 2.0)
            val horizontal = detour + EDGE
            val vertical = min(detour, verticalMargin.toDouble()) + EDGE

            return NavigationRegion(
                (minX - horizontal).toInt() - 1,
                (minY - vertical).toInt() - 1,
                (minZ - horizontal).toInt() - 1,
                (maxX + horizontal).toInt() + 1,
                (maxY + vertical).toInt() + 1,
                (maxZ + horizontal).toInt() + 1,
            )
        }

        private const val EDGE = 3.0
    }

}

/**
 * Copies of client chunk sections for route searches. A copy is kept until its section changes, so
 * repeated searches in the same area copy little.
 */
object SectionCache : EventListener {

    private const val MAX_SECTIONS = 4096

    private val copies = Long2ObjectLinkedOpenHashMap<PalettedContainer<BlockState>?>()

    /**
     * Copies [region] of [level]. Call on the render thread.
     */
    fun capture(level: ClientLevel, region: NavigationRegion): WorldSnapshot {
        val minSectionY = max(region.minY, level.minY) shr 4
        val maxSectionY = min(region.maxY, level.maxY) shr 4
        val sections = Long2ObjectOpenHashMap<PalettedContainer<BlockState>?>()

        for (chunkX in (region.minX shr 4)..(region.maxX shr 4)) {
            for (chunkZ in (region.minZ shr 4)..(region.maxZ shr 4)) {
                val chunk = level.chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) ?: continue
                for (sectionY in minSectionY..maxSectionY) {
                    val key = SectionPos.asLong(chunkX, sectionY, chunkZ)
                    sections.put(key, copyOf(key, chunk.getSection(level.getSectionIndexFromSectionY(sectionY))))
                }
            }
        }

        while (copies.size > MAX_SECTIONS) {
            copies.removeFirst()
        }

        val border = level.worldBorder
        return WorldSnapshot(
            level.minY, level.height, sections,
            border.minX, border.maxX, border.minZ, border.maxZ,
        )
    }

    /**
     * The kept copy of [section], or a new one; null for a section of only air.
     */
    private fun copyOf(key: Long, section: LevelChunkSection): PalettedContainer<BlockState>? {
        if (copies.containsKey(key)) {
            return copies.getAndMoveToLast(key)
        }

        val copy = if (section.hasOnlyAir()) null else section.states.copy()
        copies.putAndMoveToLast(key, copy)
        return copy
    }

    private fun forgetColumn(chunkX: Int, chunkZ: Int) {
        copies.keys.removeIf(LongPredicate { SectionPos.x(it) == chunkX && SectionPos.z(it) == chunkZ })
    }

    @Suppress("unused")
    private val blockChangeHandler = handler<BlockChangeEvent> { event ->
        copies.remove(SectionPos.asLong(event.blockPos))
    }

    @Suppress("unused")
    private val chunkDeltaHandler = handler<ChunkDeltaUpdateEvent> { event ->
        event.packet.runUpdates { pos, _ -> copies.remove(SectionPos.asLong(pos)) }
    }

    @Suppress("unused")
    private val chunkLoadHandler = handler<ChunkLoadEvent> { event ->
        forgetColumn(event.x, event.z)
    }

    @Suppress("unused")
    private val chunkUnloadHandler = handler<ChunkUnloadEvent> { event ->
        forgetColumn(event.pos.x, event.pos.z)
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        copies.clear()
    }

}
