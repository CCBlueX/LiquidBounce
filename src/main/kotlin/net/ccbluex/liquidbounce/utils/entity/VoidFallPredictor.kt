@file:Suppress("detekt:all")
package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoClutch
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

open class VoidFallPredictor(
    voidThreshold: Int = -64,
    ticksToPredict: Int = 2
) : Configurable("VoidPrediction"), EventListener {
    private val voidThreshold by int("VoidLevel", voidThreshold, -256..0)
    private val ticksToPredict by int("TicksToPredict", ticksToPredict, 0..120)
    var isVoidFallImminent = false
        private set

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        update()
    }

    private fun update() {
        if (player.boundingBox == null) {
            isVoidFallImminent = false
            return
        }

        isVoidFallImminent =
            isPredictingFall() && !canReachSafeBlock() && !isBlockUnder(2.0) && !isPlayerSafe()
    }

    fun isPlayerSafe(): Boolean {
        if (player.isSneaking && isBlockUnder(1.0)) return true
        if (!isInVoid(player.pos) || canReachSafeBlock()) return true
        if (player.velocity.y > 0.2 && canReachSafeBlock()) return true
        return false
    }

    fun isPredictingFall(): Boolean {
        val velY = player.velocity.y
        if (velY < -0.2 && !canReachSafeBlock()) return true
        if (player.fallDistance > 2f && velY <= 0.0 && !canReachSafeBlock()) return true

        return simulatePlayerTrajectory { pos, box, _ ->
            val voidLevelReached = pos.y <= voidThreshold.toDouble()
            val noSolidBelow = !box.collideBlockIntersects { !it.defaultState.isAir }
            val inVoidArea = isInVoid(pos)
            voidLevelReached || (inVoidArea && noSolidBelow)
        }
    }

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

    private fun canReachSafeBlock(): Boolean {
        val cache = PlayerSimulationCache.getSimulationForLocalPlayer()
        for (tick in 1..ticksToPredict) {
            val snapshot = cache.getSnapshotAt(tick)
            val pos = snapshot.pos
            val playerBox = player.boundingBox.offset(pos.subtract(player.pos))
            val blockPos = BlockPos(pos.x.toInt(), (pos.y - 0.5).toInt(), pos.z.toInt())
            val belowPos = blockPos.down()
            if (belowPos.canStandOn()
                && countAdjacentSafeBlocks(blockPos) >= 0
                && !playerBox.collideBlockIntersects { it.defaultState.isAir }) {
                return true
            }
        }
        return false
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
}
