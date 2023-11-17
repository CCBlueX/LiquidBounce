package net.ccbluex.liquidbounce.features.module.modules.world.scaffold

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.QuickAccess.player
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

object ScaffoldMovementPlanner {
    private const val MAX_LAST_PLACE_BLOCKS: Int = 4

    private val lastPlacedBlocks = ArrayDeque<BlockPos>(MAX_LAST_PLACE_BLOCKS)
    private var lastPosition: BlockPos? = null

    /**
     * When using scaffold the player wants to follow the line and the scaffold should support them in doing so.
     * This function calculates this ideal line that the player should move on.
     */
    fun getOptimalMovementLine(directionalInput: DirectionalInput): Line? {
        val direction =
            chooseDirection(
                getMovementDirectionOfInput(
                    player.yaw,
                    directionalInput,
                ),
            )

        // Is this a good way to find the block center?
        val blockUnderPlayer = findBlockPlayerStandsOn() ?: return null

        val lastBlocksAvg = findLastPlacedBlocksAverage()
        val lineBaseBlock = lastBlocksAvg ?: blockUnderPlayer.toVec3d()

        // We try to make the player run on this line
        val optimalLine = Line(Vec3d(lineBaseBlock.x + 0.5, player.pos.y, lineBaseBlock.z + 0.5), direction)

        // Debug optimal line
        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "optimalLine",
            ModuleDebug.DebuggedLine(optimalLine, if (lastBlocksAvg == null) Color4b.RED else Color4b.GREEN)
        )

        return optimalLine
    }

    /**
     * Calculates the average position of last 2 placed blocks.
     * It used as a starting position for the optimal movement line. This is especially important if wanted
     * to move diagonally.
     */
    private fun findLastPlacedBlocksAverage(): Vec3d? {
        // Take the last 2 blocks placed
        val lastPlacedBlocksToConsider =
            lastPlacedBlocks.subList(
                fromIndex = (lastPlacedBlocks.size - 2).coerceAtLeast(0),
                toIndex = (lastPlacedBlocks.size).coerceAtLeast(0),
            )

        if (lastPlacedBlocksToConsider.isEmpty()) {
            return null
        }

        // Just debug stuff
        lastPlacedBlocksToConsider.forEachIndexed { idx, pos ->
            val alpha = ((1.0 - idx.toDouble() / lastPlacedBlocksToConsider.size.toDouble()) * 255.0).toInt()

            ModuleDebug.debugGeometry(
                ModuleScaffold,
                "lastPlacedBlock$idx",
                ModuleDebug.DebuggedBox(Box.from(pos.toVec3d()), Color4b(alpha, alpha, 255, 127)),
            )
        }

        // Calculate the average direction of the last placed blocks
        return lastPlacedBlocksToConsider
            .fold(Vec3i.ZERO) { a, b -> a.add(b) }
            .toVec3d()
            .multiply(1.0 / lastPlacedBlocksToConsider.size.toDouble())
    }

    /**
     * Find the block the player stands on.
     * It considers all blocks which the player's hitbox collides with and chooses one. If the player stands on the last
     * block this function returned, this block is preferred.
     */
    private fun findBlockPlayerStandsOn(): BlockPos? {
        val offsetsToTry = arrayOf(0.301, 0.0, -0.301)
        val candidates = mutableListOf<BlockPos>()

        for (xOffset in offsetsToTry) {
            for (zOffset in offsetsToTry) {
                val playerPos = player.pos.add(xOffset, -1.0, zOffset).toBlockPos()

                if (playerPos.canStandOn()) {
                    candidates.add(playerPos)
                }
            }
        }

        if (lastPosition in candidates) {
            return lastPosition
        }

        val currPosition = candidates.firstOrNull()

        lastPosition = currPosition

        return currPosition
    }

    /**
     * The player can move in a lot of directions. But there are only 8 directions which make sense for scaffold to
     * follow (NORTH, NORTH_EAST, EAST, etc.). This function chooses such a direction based on the current angle.
     * i.e. if we were looking like 30° to the right, we would choose the direction NORTH_EAST (1.0, 0.0, 1.0).
     * And scaffold would move diagonally to the right.
     */
    private fun chooseDirection(currentAngle: Float): Vec3d {
        // Transform the angle ([-180; 180]) to [0; 8]
        val currentDirection = currentAngle / 180.0 * 4 + 4

        // Round the angle to the nearest integer, which represents the direction.
        val newDirectionNumber = round(currentDirection)
        // Do this transformation backwards, and we have an angle that follows one of the 8 directions.
        val newDirectionAngle = MathHelper.wrapDegrees((newDirectionNumber - 4) / 4.0 * 180.0 + 90.0) / 180.0 * PI

        return Vec3d(cos(newDirectionAngle), 0.0, sin(newDirectionAngle))
    }

    /**
     * Remembers the last placed blocks and removes old ones.
     */
    fun trackPlacedBlock(target: BlockPlacementTarget) {
        lastPlacedBlocks.add(target.placedBlock)

        while (lastPlacedBlocks.size > MAX_LAST_PLACE_BLOCKS)
            lastPlacedBlocks.removeFirst()
    }

    fun reset() {
        lastPosition = null
        this.lastPlacedBlocks.clear()
    }
}
