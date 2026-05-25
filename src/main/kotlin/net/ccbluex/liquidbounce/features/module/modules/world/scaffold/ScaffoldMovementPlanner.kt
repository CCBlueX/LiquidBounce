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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold

import net.ccbluex.fastutil.objectHashSetOf
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.round

object ScaffoldMovementPlanner {
    private const val MAX_LAST_PLACE_BLOCKS: Int = 4
    private const val DIRECTION_HYSTERESIS_DEGREES = 30.0F
    private const val SUPPORT_SURFACE_EPSILON = 1.0E-3
    private const val SUPPORT_OVERLAP_HYSTERESIS = 0.02

    private val lastPlacedBlocks = ArrayDeque<BlockPos>(MAX_LAST_PLACE_BLOCKS)
    private var lastPosition: BlockPos? = null
    private var lastSupportReference: SupportReference? = null
    private var lastDirectionAngle = Float.NaN

    data class SupportReference(
        val blockPos: BlockPos,
        val offsetX: Double,
        val offsetZ: Double,
    )

    private data class SupportCandidate(
        val blockPos: BlockPos,
        val overlapArea: Double,
        val surfaceDelta: Double,
        val horizontalDistanceToPlayerSqr: Double,
    ) {
        fun isBetterThan(other: SupportCandidate?): Boolean {
            return when {
                other == null -> true
                surfaceDelta + SUPPORT_SURFACE_EPSILON < other.surfaceDelta -> true
                other.surfaceDelta + SUPPORT_SURFACE_EPSILON < surfaceDelta -> false
                overlapArea > other.overlapArea + SUPPORT_OVERLAP_HYSTERESIS -> true
                overlapArea + SUPPORT_OVERLAP_HYSTERESIS < other.overlapArea -> false
                horizontalDistanceToPlayerSqr < other.horizontalDistanceToPlayerSqr -> true
                horizontalDistanceToPlayerSqr > other.horizontalDistanceToPlayerSqr -> false
                else -> false
            }
        }
    }

    /**
     * When using scaffold the player wants to follow the line and the scaffold should support them in doing so.
     * This function estimates the line the player is trying to move on while preserving the player's offset on the
     * current support block until placed block history can provide a stable line.
     */
    fun getOptimalMovementLine(directionalInput: DirectionalInput): Line? {
        val direction = chooseDirection(player.getMovementDirectionOfInput(directionalInput))

        // Keep the current in-block offset so starting away from the block center does not snap the line sideways.
        val supportReference = findBlockPlayerStandsOn() ?: return null
        lastSupportReference = supportReference

        val lastBlocksLine = fitLinesThroughLastPlacedBlocks()

        // If the recent placements match the current movement direction, follow them. Otherwise use the current
        // support block as a fresh anchor because the user probably started a new direction.
        val lineAnchor = if (lastBlocksLine != null && !divergesTooMuchFromDirection(lastBlocksLine, direction)) {
            lastBlocksLine.getNearestPointTo(player.position())
        } else {
            Vec3(
                supportReference.blockPos.x + 0.5 + supportReference.offsetX,
                player.position().y,
                supportReference.blockPos.z + 0.5 + supportReference.offsetZ
            )
        }

        // We try to make the player run on this line.
        val optimalLine = Line(lineAnchor.copy(y = player.position().y), direction)

        // Debug optimal line
        ModuleScaffold.debugGeometry("optimalLine") {
            ModuleDebug.DebuggedLine(optimalLine, if (lastBlocksLine == null) Color4b.RED else Color4b.GREEN)
        }

        return optimalLine
    }

    private fun divergesTooMuchFromDirection(lastBlocksLine: Line, direction: Vec3): Boolean {
        return lastBlocksLine.direction.dot(direction) < 0.5 // cos(60deg)
    }

    /**
     * Tries to fit a line that goes through the last placed blocks. Currently only considers the last two.
     */
    private fun fitLinesThroughLastPlacedBlocks(): Line? {
        // Take the last 2 blocks placed
        if (lastPlacedBlocks.size < 2) {
            return null
        }
        val last = lastPlacedBlocks.last()
        val secondToLast = lastPlacedBlocks[lastPlacedBlocks.size - 2]

        // Just debug stuff
        if (ModuleDebug.running) {
            debugLastPlacedBlocks(listOf(secondToLast, last))
        }

        val avgPos = secondToLast.offset(last).toVec3d() * 0.5
        val dir = last.subtract(secondToLast).toVec3d().normalize()

        // Calculate the average direction of the last placed blocks
        return Line(avgPos, dir)
    }

    private fun debugLastPlacedBlocks(lastPlacedBlocksToConsider: List<BlockPos>) {
        lastPlacedBlocksToConsider.forEachIndexed { idx, pos ->
            val alpha = ((1.0 - idx.toDouble() / lastPlacedBlocksToConsider.size.toDouble()) * 200.0).toInt()

            ModuleScaffold.debugGeometry("lastPlacedBlock$idx") {
                ModuleDebug.DebuggedBox(AABB(pos), Color4b(133, 155, 255, alpha))
            }
        }
    }

    private val offsetsToTry = doubleArrayOf(0.301, 0.0, -0.301)

    /**
     * Find the block the player stands on.
     * It considers nearby blocks below the player and ranks them by support surface height, hitbox overlap area, and
     * distance to the player. Recent support blocks are preferred when they are still close enough to the best candidate
     * to prevent line jitter at block boundaries.
     */
    private fun findBlockPlayerStandsOn(): SupportReference? {
        // Contains nearby blocks that can currently support the player.
        val candidates = objectHashSetOf<BlockPos>()

        for (xOffset in offsetsToTry) {
            for (zOffset in offsetsToTry) {
                val playerPos = player.position().toBlockPos(xOffset, -1.0, zOffset)

                val isEmpty = playerPos.state?.getCollisionShape(world, playerPos)?.isEmpty ?: true

                if (!isEmpty) {
                    candidates.add(playerPos)
                }
            }
        }

        if (candidates.isEmpty()) {
            lastSupportReference = null
            return null
        }

        var bestCandidate: SupportCandidate? = null
        var preferredLastPlaced: SupportCandidate? = null
        var preferredLastPosition: SupportCandidate? = null
        val lastPlacedBlock = lastPlacedBlocks.lastOrNull()

        for (blockPos in candidates) {
            val candidate = createSupportCandidate(blockPos)

            if (candidate.isBetterThan(bestCandidate)) {
                bestCandidate = candidate
            }

            if (blockPos == lastPlacedBlock) {
                preferredLastPlaced = candidate
            }

            if (blockPos == lastPosition) {
                preferredLastPosition = candidate
            }
        }

        val best = bestCandidate ?: return null

        fun preferStableCandidate(preferredPos: BlockPos?): SupportCandidate? {
            val preferred = when (preferredPos) {
                lastPlacedBlock -> preferredLastPlaced
                lastPosition -> preferredLastPosition
                else -> null
            } ?: return null

            if (preferred.surfaceDelta > best.surfaceDelta + SUPPORT_SURFACE_EPSILON) {
                return null
            }

            if (preferred.overlapArea + SUPPORT_OVERLAP_HYSTERESIS < best.overlapArea) {
                return null
            }

            return preferred
        }

        val chosenCandidate =
            preferStableCandidate(lastPlacedBlock)
                ?: preferStableCandidate(lastPosition)
                ?: best

        lastPosition = chosenCandidate.blockPos

        return SupportReference(
            chosenCandidate.blockPos,
            player.position().x - (chosenCandidate.blockPos.x + 0.5),
            player.position().z - (chosenCandidate.blockPos.z + 0.5),
        )
    }

    private fun createSupportCandidate(blockPos: BlockPos): SupportCandidate {
        val playerBoundingBox = player.boundingBox
        val collisionShape = blockPos.state?.getCollisionShape(world, blockPos)

        var bestSurfaceDelta = Double.POSITIVE_INFINITY
        var overlapAreaOnBestSurface = 0.0

        collisionShape?.forAllBoxes { minX, _, minZ, maxX, maxY, maxZ ->
            val minX = blockPos.x + minX
            val maxX = blockPos.x + maxX
            val maxY = blockPos.y + maxY
            val minZ = blockPos.z + minZ
            val maxZ = blockPos.z + maxZ

            val overlapX = minOf(playerBoundingBox.maxX, maxX) - maxOf(playerBoundingBox.minX, minX)
            val overlapZ = minOf(playerBoundingBox.maxZ, maxZ) - maxOf(playerBoundingBox.minZ, minZ)

            if (overlapX <= 0.0 || overlapZ <= 0.0) {
                return@forAllBoxes
            }

            val surfaceDelta = abs(playerBoundingBox.minY - maxY)
            val overlapArea = overlapX * overlapZ

            when {
                surfaceDelta + SUPPORT_SURFACE_EPSILON < bestSurfaceDelta -> {
                    bestSurfaceDelta = surfaceDelta
                    overlapAreaOnBestSurface = overlapArea
                }

                abs(surfaceDelta - bestSurfaceDelta) <= SUPPORT_SURFACE_EPSILON -> {
                    overlapAreaOnBestSurface += overlapArea
                }
            }
        }

        return SupportCandidate(
            blockPos = blockPos,
            overlapArea = overlapAreaOnBestSurface,
            surfaceDelta = bestSurfaceDelta,
            horizontalDistanceToPlayerSqr = player.position()
                .horizontalDistanceToSqr(blockPos.x + 0.5, blockPos.z + 0.5),
        )
    }

    /**
     * The player can move in a lot of directions. But there are only 8 directions which make sense for scaffold to
     * follow (NORTH, NORTH_EAST, EAST, etc.). This function chooses such a direction based on the current angle.
     * i.e. if we were looking like 30° to the right, we would choose the direction NORTH_EAST (1.0, 0.0, 1.0).
     * And scaffold would move diagonally to the right.
     * The last selected direction is kept while the input angle remains close enough, which avoids oscillation near
     * 8-way direction boundaries.
     *
     * @return normalized direction vector without y value
     */
    private fun chooseDirection(currentAngle: Float): Vec3 {
        if (!lastDirectionAngle.isNaN() &&
            Mth.degreesDifferenceAbs(currentAngle, lastDirectionAngle) <= DIRECTION_HYSTERESIS_DEGREES
        ) {
            return Vec3.directionFromRotation(0.0F, lastDirectionAngle)
        }

        // Transform the angle ([-180; 180]) to [0; 8]
        val currentDirection = currentAngle / 180.0F * 4 + 4

        // Round the angle to the nearest integer, which represents the direction.
        val newDirectionNumber = round(currentDirection)
        // Do this transformation backwards, and we have an angle that follows one of the 8 directions.
        val newDirectionAngle = Mth.wrapDegrees((newDirectionNumber - 4) / 4.0F * 180.0F)
        lastDirectionAngle = newDirectionAngle

        return Vec3.directionFromRotation(0.0F, newDirectionAngle)
    }

    /**
     * Remembers the last placed blocks and removes old ones.
     */
    fun trackPlacedBlock(target: BlockPos) {
        if (target == lastPlacedBlocks.lastOrNull()) return

        while (lastPlacedBlocks.size >= MAX_LAST_PLACE_BLOCKS) {
            lastPlacedBlocks.removeFirst()
        }

        lastPlacedBlocks.add(target)
    }

    fun reset() {
        lastPosition = null
        lastSupportReference = null
        lastDirectionAngle = Float.NaN
        this.lastPlacedBlocks.clear()
    }

    fun getCurrentSupportReference(): SupportReference? = lastSupportReference
}
