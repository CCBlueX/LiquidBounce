package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoClutch
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import kotlin.math.ceil
import kotlin.math.floor

class VoidFallPrediction(val parent: EventListener) : Configurable("VoidPrediction"), EventListener {
    override fun parent() = parent

    private val voidThreshold by int("VoidLevel", 0, -256..0)
    private val ticksToPredict by int("TicksToPredict", 75, 30..120)

    var isVoidFallImminent = false
        private set

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
       update()
    }
    private fun update() { isVoidFallImminent =
        isPredictingFall() && !canReachSafeBlock() && !isBlockUnder(2.0) && !isPlayerSafe() }

    fun canReachSafeBlockFrom(pos: Vec3d = player.pos): Boolean {
        val cache = PlayerSimulationCache.getSimulationForLocalPlayer()
        val snapshots = (0 until ticksToPredict).map { cache.getSnapshotAt(it) }

        for (snapshot in snapshots) {
            val currentPos = snapshot.pos
            val blockPos = BlockPos(currentPos.x.toInt(), currentPos.y.toInt(), currentPos.z.toInt())
            val belowPos = blockPos.down()
            val belowState = world.getBlockState(belowPos)
            val isSafeLanding = !belowState.isAir && belowState.block !in ModuleAutoClutch.unsafeBlocks

            val playerBox = player.boundingBox.offset(currentPos.subtract(pos))
            val safeBlockCount = countAdjacentSafeBlocks(blockPos)
            val isNearSafeBlock = isSafeLanding && safeBlockCount >= 0
            if (isNearSafeBlock && !world.getBlockCollisions(player, playerBox).any()) {
                return true
            }
        }
        return false
    }

    fun isPlayerSafe(): Boolean {
        if (player.isSneaking && isBlockUnder(1.0)) return true
        if (!isInVoid(player.pos) || canReachSafeBlock()) return true
        if (player.velocity.y > 0.2 && canReachSafeBlock()) return true
        return false
    }

    fun isPredictingFall(): Boolean {
        if (player.isOnGround) return false
        val velY = player.velocity.y
        if (velY < -0.2 && !canReachSafeBlock()) return true
        if (player.fallDistance > 2f && velY <= 0.0 && !canReachSafeBlock()) return true

        return simulatePlayerTrajectory { pos, box, blockPos ->
            pos.y <= voidThreshold.toDouble() &&
                !box.collideBlockIntersects { !it.defaultState.isAir }
        }

    }

    fun countAdjacentSafeBlocks(center: BlockPos): Int {
        var count = 0
        for (dx in -1..1) for (dz in -1..1) {
            val nearby = BlockPos(center.x + dx, center.y - 1, center.z + dz)
            val state = nearby.getState()
            if (state != null && !state.isAir && state.block !in ModuleAutoClutch.unsafeBlocks) count++
        }
        return count
    }

    private fun canReachSafeBlock(): Boolean {
        val cache = PlayerSimulationCache.getSimulationForLocalPlayer()
        for (tick in 1..ticksToPredict) {
            val snapshot = cache.getSnapshotAt(tick)
            val pos = snapshot.pos
            val playerBox = player.boundingBox.offset(pos.subtract(player.pos))
            val blockPos = BlockPos(pos.x.toInt(), (pos.y - 0.5).toInt(), pos.z.toInt())
            val belowPos = blockPos.down()
            if (belowPos.canStandOn() && countAdjacentSafeBlocks(blockPos) >= 0 && !playerBox.collideBlockIntersects { it.defaultState.isAir }) {
                return true
            }
        }
        return false
    }

    fun isInVoid(pos: Vec3d, voidDistance: Int = -1): Boolean {
        val xRange = mutableListOf(0)
        val zRange = mutableListOf(0)

        if (pos.x - floor(pos.x) <= 0.3) xRange.add(-1)
        else if (ceil(pos.x) - pos.x <= 0.3) xRange.add(1)

        if (pos.z - floor(pos.z) <= 0.3) zRange.add(-1)
        else if (ceil(pos.z) - pos.z <= 0.3) zRange.add(1)

        val minY = if (voidDistance == -1) -64 else pos.y.toInt() - voidDistance
        val maxY = pos.y.toInt()

        for (xOffset in xRange) {
            for (zOffset in zRange) {
                for (y in minY..maxY) {
                    val blockPos = BlockPos(pos.x.toInt() + xOffset, y, pos.z.toInt() + zOffset)
                    val state = blockPos.getState()
                    if (state != null && !state.isAir) return false
                }
            }
        }
        return true
    }

    fun isBlockUnder(height: Double = 5.0): Boolean {
        val box = player.boundingBox.offset(0.0, -height, 0.0)
        return box.collideBlockIntersects { it.defaultState.isAir }
    }

    fun simulatePlayerTrajectory(checkCondition: (Vec3d, Box, BlockPos) -> Boolean): Boolean {
        val cache = PlayerSimulationCache.getSimulationForLocalPlayer()
        for (tick in 1..ticksToPredict) {
            val snapshot = cache.getSnapshotAt(tick)
            val pos = snapshot.pos
            val playerBox = player.boundingBox.offset(pos.subtract(player.pos))
            val blockPos = BlockPos(pos.x.toInt(), (pos.y - 0.5).toInt(), pos.z.toInt())
            if (checkCondition(pos, playerBox, blockPos)) {
                return true
            }
        }
        return false
    }
    fun hasSolidBlockBelow(): Boolean {
        val checkDepth = 3
        val bb = player.boundingBox
        val minX = floor(bb.minX).toInt()
        val maxX = floor(bb.maxX).toInt()
        val minZ = floor(bb.minZ).toInt()
        val maxZ = floor(bb.maxZ).toInt()

        for (dy in 0..checkDepth) {
            val y = floor(bb.minY).toInt() - dy
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val state = BlockPos(x, y, z).getState()
                    if (state != null && !state.isAir) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
