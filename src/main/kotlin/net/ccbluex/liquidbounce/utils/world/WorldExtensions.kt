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

@file:Suppress("TooManyFunctions")
package net.ccbluex.liquidbounce.utils.world

import com.google.common.base.Predicates
import net.ccbluex.fastutil.asObjectList
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MixinLevelInvoker
import net.ccbluex.liquidbounce.utils.math.expandToCube
import net.minecraft.core.BlockPos
import net.minecraft.util.AbortableIterationConsumer
import net.minecraft.world.attribute.BedRule
import net.minecraft.world.attribute.EnvironmentAttributes
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.EntityGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.level.entity.LevelEntityGetter
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * @return if water and ice evaporates in this world (e.g. nether)
 */
val Level.waterEvaporates: Boolean
    get() = this.environmentAttributes().getDimensionValue(EnvironmentAttributes.WATER_EVAPORATES)

val Level.bedRule: BedRule
    get() = this.environmentAttributes().getDimensionValue(EnvironmentAttributes.BED_RULE)

val Level.respawnAnchorWorks: Boolean
    get() = this.environmentAttributes().getDimensionValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS)

/**
 * Returns the loaded section slice from section 0 through [ChunkAccess.highestFilledSectionIndex].
 */
val ChunkAccess.filledSections: List<LevelChunkSection>
    get() = this.sections.asObjectList(offset = 0, length = this.highestFilledSectionIndex + 1)

/**
 * Iterates all blocks in a specific section index and exposes world-space block positions.
 *
 * The [mutable] instance is reused across callbacks for allocation-free chunk scanning.
 *
 * @see LevelChunk.getBlockState
 */
inline fun LevelChunk.forEachSectionBlock(
    sectionIndex: Int,
    mutable: BlockPos.MutableBlockPos = BlockPos.MutableBlockPos(),
    action: (BlockPos, BlockState) -> Unit,
) {
    val section = this.getSection(sectionIndex)
    val startX = this.pos.minBlockX
    val startY = this.sectionBottomY(sectionIndex)
    val startZ = this.pos.minBlockZ
    section.forEachBlock { localX, localY, localZ, state ->
        action(mutable.set(startX or localX, startY or localY, startZ or localZ), state)
    }
}

/**
 * Iterates all 4096 block states in a section and provides local section coordinates (0..15).
 *
 * @see LevelChunk.getBlockState
 */
inline fun LevelChunkSection.forEachBlock(action: (localX: Int, localY: Int, localZ: Int, BlockState) -> Unit) {
    for (localY in 0..15) {
        for (localZ in 0..15) {
            for (localX in 0..15) {
                val blockState = this.getBlockState(localX, localY, localZ)
                action(localX, localY, localZ, blockState)
            }
        }
    }
}

/**
 * Converts a section index to the section base world Y (multiple of 16).
 *
 * `index == (y >> 4) - (bottomY >> 4)`
 */
fun ChunkAccess.sectionBottomY(index: Int): Int = (index + (this.minY shr 4)) shl 4

inline fun <reified T : Entity> EntityGetter.getEntitiesInCube(
    midPos: Vec3,
    range: Double,
    predicate: Predicate<T> = Predicates.alwaysTrue(),
): MutableList<T> {
    return getEntitiesOfClass(
        T::class.java,
        midPos.expandToCube(range),
        predicate,
    ) // -> ArrayList
}

fun EntityGetter.getEntitiesInCube(
    midPos: Vec3,
    range: Double,
    exclusion: Entity? = null,
    predicate: Predicate<Entity> = Predicates.alwaysTrue(),
): MutableList<Entity> {
    val size = range * 2.0
    val box = AABB.ofSize(midPos, size, size, size)
    return getEntities(exclusion, box, predicate) // -> ArrayList
}

val Level.entityGetter: LevelEntityGetter<Entity>
    inline get() = (this as MixinLevelInvoker).invokeGetEntities()

fun <B : Entity, T : B> LevelEntityGetter<B>.forEach(
    type: EntityTypeTest<B, T>,
    consumer: Consumer<T>,
) = this.get(type, AbortableIterationConsumer.forConsumer(consumer))

fun <B : Entity, T : B, C : MutableCollection<in T>> LevelEntityGetter<B>.filterTo(
    destination: C,
    type: EntityTypeTest<B, T>,
    predicate: Predicate<T>,
): C {
    this.forEach(type) { if (predicate.test(it)) destination += it }
    return destination
}

fun <B : Entity, T : B> LevelEntityGetter<B>.filter(
    type: EntityTypeTest<B, T>,
    predicate: Predicate<T>,
): List<T> = this.filterTo(ArrayList(), type, predicate)

fun <B : Entity, T : B> LevelEntityGetter<B>.firstOrNull(
    type: EntityTypeTest<B, T>,
    predicate: Predicate<T>,
): T? {
    var ref: T? = null
    this.get(type) {
        if (predicate.test(it)) {
            ref = it
            AbortableIterationConsumer.Continuation.ABORT
        } else {
            AbortableIterationConsumer.Continuation.CONTINUE
        }
    }
    return ref
}

fun <B : Entity, T : B> LevelEntityGetter<B>.none(
    type: EntityTypeTest<B, T>,
    predicate: Predicate<T>,
): Boolean = firstOrNull(type, predicate) == null

fun <B : Entity, T : B> LevelEntityGetter<B>.any(
    type: EntityTypeTest<B, T>,
    predicate: Predicate<T>,
): Boolean = firstOrNull(type, predicate) != null

private val localEntityIdGenerator = AtomicInteger(-1)

/**
 * Allocates a unique negative entity ID for a locally spawned entity.
 *
 * Server entity IDs are always positive, and [Level.getNextEntityId] returns 0 on the client,
 * which [net.minecraft.client.multiplayer.ClientLevel.addEntity] rejects by throwing
 * `Tried to access entity ID before ID assignment`.
 */
fun Level.nextLocalEntityId(): Int = localEntityIdGenerator.getAndDecrement()
