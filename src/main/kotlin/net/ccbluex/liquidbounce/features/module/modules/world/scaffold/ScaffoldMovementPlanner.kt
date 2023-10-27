package net.ccbluex.liquidbounce.features.module.modules.world.scaffold

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.isNeighborOfOrEquivalent
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

        val findLastPlacedBlocksAverage = findLastPlacedBlocksAverage(blockUnderPlayer)
        val lineBaseBlock = findLastPlacedBlocksAverage ?: blockUnderPlayer.toVec3d()

        if (findLastPlacedBlocksAverage == null) {
            val line = Line(Vec3d(blockUnderPlayer.x + 0.5, player.pos.y, blockUnderPlayer.z + 0.5), direction)

            ModuleDebug.debugGeometry(ModuleScaffold, "optimalLine", ModuleDebug.DebuggedLine(line, Color4b.RED))
        } else {
            val line =
                Line(
                    position =
                        Vec3d(
                            findLastPlacedBlocksAverage.x + 0.5,
                            player.pos.y,
                            findLastPlacedBlocksAverage.z + 0.5,
                        ),
                    direction = direction,
                )

            ModuleDebug.debugGeometry(ModuleScaffold, "optimalLine", ModuleDebug.DebuggedLine(line, Color4b.GREEN))
        }

        // We try to make the player run on this line
        return Line(Vec3d(lineBaseBlock.x + 0.5, player.pos.y, lineBaseBlock.z + 0.5), direction)
    }

    fun findLastPlacedBlocksAverage(blockUnderPlayer: BlockPos): Vec3d? {
        val lastPlacedBlocksToConsider =
            lastPlacedBlocks.subList(
                fromIndex = (lastPlacedBlocks.size - 2).coerceAtLeast(0),
                toIndex = (lastPlacedBlocks.size).coerceAtLeast(0),
            )

        if (lastPlacedBlocksToConsider.isEmpty()) {
            return null
        }

        lastPlacedBlocksToConsider.forEachIndexed { idx, pos ->
            val alpha = ((1.0 - idx.toDouble() / lastPlacedBlocksToConsider.size.toDouble()) * 255.0).toInt()

            ModuleDebug.debugGeometry(
                ModuleScaffold,
                "lastPlacedBlock$idx",
                ModuleDebug.DebuggedBox(Box.from(pos.toVec3d()), Color4b(alpha, alpha, 255, 127)),
            )
        }

        var currentBlock = blockUnderPlayer

        for (blockPos in lastPlacedBlocksToConsider.reversed()) {
            if (!currentBlock.isNeighborOfOrEquivalent(blockPos)) {
                return null
            }

            currentBlock = blockPos
        }

        return lastPlacedBlocksToConsider
            .fold(Vec3i.ZERO) { a, b -> a.add(b) }
            .toVec3d()
            .multiply(1.0 / lastPlacedBlocksToConsider.size.toDouble())
    }

    fun findBlockPlayerStandsOn(): BlockPos? {
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

    private fun chooseDirection(currentAngle: Float): Vec3d {
        val currentDirection = currentAngle / 180.0 * 4 + 4

        val newDirectionNumber = round(currentDirection)
        val newDirectionAngle = MathHelper.wrapDegrees((newDirectionNumber - 4) / 4.0 * 180.0 + 90.0) / 180.0 * PI

        val newDirection = Vec3d(cos(newDirectionAngle), 0.0, sin(newDirectionAngle))

        return newDirection
    }

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
