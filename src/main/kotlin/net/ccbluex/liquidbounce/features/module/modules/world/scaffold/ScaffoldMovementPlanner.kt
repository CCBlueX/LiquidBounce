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

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.math.bottomCenter
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.round

@Suppress("TooManyFunctions")
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
    ) : Comparable<SupportCandidate> {
        override fun compareTo(other: SupportCandidate): Int {
            return when {
                surfaceDelta + SUPPORT_SURFACE_EPSILON < other.surfaceDelta -> -1
                other.surfaceDelta + SUPPORT_SURFACE_EPSILON < surfaceDelta -> 1
                overlapArea > other.overlapArea + SUPPORT_OVERLAP_HYSTERESIS -> -1
                overlapArea + SUPPORT_OVERLAP_HYSTERESIS < other.overlapArea -> 1
                horizontalDistanceToPlayerSqr < other.horizontalDistanceToPlayerSqr -> -1
                horizontalDistanceToPlayerSqr > other.horizontalDistanceToPlayerSqr -> 1
                else -> 0
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
        val supportReference = findSupportReferenceUnderPlayer() ?: return null
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

        val secondToLastCenter = secondToLast.bottomCenter
        val lastCenter = last.bottomCenter
        val avgPos = secondToLastCenter.add(lastCenter).scale(0.5)
        val dir = lastCenter.subtract(secondToLastCenter).normalize()

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
     * Find the support block reference the player is currently standing on.
     * It samples nearby blocks below the player, ranks them by support surface height, hitbox overlap area, and
     * distance to the player, then keeps a stable previous choice when it is still close enough.
     */
    private fun findSupportReferenceUnderPlayer(): SupportReference? {
        val candidates = collectSupportCandidates()
        if (candidates.isEmpty()) {
            lastSupportReference = null
            lastPosition = null
            return null
        }

        val bestCandidate = candidates.values.minOrNull() ?: return null
        val chosenCandidate = chooseStableSupportCandidate(candidates, bestCandidate)

        lastPosition = chosenCandidate.blockPos

        return SupportReference(
            chosenCandidate.blockPos,
            player.position().x - (chosenCandidate.blockPos.x + 0.5),
            player.position().z - (chosenCandidate.blockPos.z + 0.5),
        )
    }

    private fun collectSupportCandidates(): Map<BlockPos, SupportCandidate> {
        // [offsetsToTry] makes the result map has up to 4 entries
        val candidates = Object2ObjectArrayMap<BlockPos, SupportCandidate>()

        for (xOffset in offsetsToTry) {
            for (zOffset in offsetsToTry) {
                val blockPos = player.position().toBlockPos(xOffset, -1.0, zOffset)

                if (candidates.containsKey(blockPos)) continue

                val collisionShape = blockPos.state?.getCollisionShape(world, blockPos) ?: continue

                if (!collisionShape.isEmpty) {
                    candidates[blockPos] = createSupportCandidate(blockPos)
                }
            }
        }

        return candidates
    }

    private fun chooseStableSupportCandidate(
        candidates: Map<BlockPos, SupportCandidate>,
        bestCandidate: SupportCandidate,
    ): SupportCandidate {
        val lastPlacedBlock = lastPlacedBlocks.lastOrNull()
        val preferredLastPlaced = candidates[lastPlacedBlock]
        val preferredLastPosition = candidates[lastPosition]

        return preferredLastPlaced?.takeIf { it.isStableComparedTo(bestCandidate) }
            ?: preferredLastPosition?.takeIf { it.isStableComparedTo(bestCandidate) }
            ?: bestCandidate
    }

    private fun SupportCandidate.isStableComparedTo(bestCandidate: SupportCandidate): Boolean {
        if (this.surfaceDelta > bestCandidate.surfaceDelta + SUPPORT_SURFACE_EPSILON) {
            return false
        }

        if (this.overlapArea + SUPPORT_OVERLAP_HYSTERESIS < bestCandidate.overlapArea) {
            return false
        }

        return true
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
