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

package net.ccbluex.liquidbounce.utils.world

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent
import net.ccbluex.liquidbounce.event.events.ChunkDeltaUpdateEvent
import net.ccbluex.liquidbounce.event.events.ChunkLoadEvent
import net.ccbluex.liquidbounce.event.events.ChunkUnloadEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.client.clientState
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.SectionPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.status.ChunkStatus
import kotlin.math.max
import kotlin.math.min

/** Copies of client chunk sections for route searches, kept until the section changes. */
object SectionCache : EventListener {

    private const val MAX_SECTIONS = 4096

    private val copies by clientState { Long2ObjectLinkedOpenHashMap<PalettedContainer<BlockState>?>() }

    /** Call on the render thread. */
    fun capture(level: ClientLevel, region: BlockRegion): WorldSnapshot {
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

    // Null for a section of only air.
    private fun copyOf(key: Long, section: LevelChunkSection): PalettedContainer<BlockState>? {
        if (copies.containsKey(key)) {
            return copies.getAndMoveToLast(key)
        }

        val copy = if (section.hasOnlyAir()) null else section.states.copy()
        copies.putAndMoveToLast(key, copy)
        return copy
    }

    private fun forgetColumn(chunkX: Int, chunkZ: Int) {
        copies.keys.removeIf { SectionPos.x(it) == chunkX && SectionPos.z(it) == chunkZ }
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
