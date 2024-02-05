package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.block.Blocks
import net.minecraft.util.math.Direction
import kotlin.math.floor
import kotlin.math.round

object ScaffoldBreezilyFeature : ToggleableConfigurable(ModuleScaffold, "Breezily", false) {

    private var lastSideways = 0f
    private var lastAirTime = 0L
    private var currentEdgeDistanceRandom = 0.45

    private val edgeDistance by floatRange("EdgeDistance", 0.45f..0.5f, 0.25f..0.5f, "blocks")

     fun doBreezilyIfNeeded(event: MovementInputEvent) {
        if (!enabled) return
        if (!event.directionalInput.forwards) return
         if (mc.player!!.isSneaking) return
         if (ScaffoldAutoJumpFeature.isGoingDiagonal) return

         if (mc.world!!.getBlockState(mc.player!!.blockPos.offset(Direction.DOWN, 1)).block == Blocks.AIR) {
             lastAirTime = System.currentTimeMillis()
         }

        if (System.currentTimeMillis() - lastAirTime > 500) return

        val modX = mc.player!!.x - floor(mc.player!!.x)
        val modZ = mc.player!!.z - floor(mc.player!!.z)

        val ma = 1 - currentEdgeDistanceRandom
        var currentSideways = 0f
        when (Direction.fromRotation(mc.player!!.yaw.toDouble()).toString()) {
            "south" -> {
                if (modX > ma) currentSideways = 1f
                if (modX < currentEdgeDistanceRandom) currentSideways = -1f
            }

            "north" -> {
                if (modX > ma) currentSideways = -1f
                if (modX < currentEdgeDistanceRandom) currentSideways = 1f
            }

            "east" -> {
                if (modZ > ma) currentSideways = -1f
                if (modZ < currentEdgeDistanceRandom) currentSideways = 1f
            }

            "west" -> {
                if (modZ > ma) currentSideways = 1f
                if (modZ < currentEdgeDistanceRandom) currentSideways = -1f
            }
        }

        if (lastSideways != currentSideways && currentSideways != 0f) {
            lastSideways = currentSideways
            currentEdgeDistanceRandom = edgeDistance.random()
        }

        event.directionalInput = DirectionalInput(
            event.directionalInput.forwards,
            event.directionalInput.backwards,
            lastSideways == -1f,
            lastSideways == 1f
        )
    }

    fun optimizeRotation(target: BlockPlacementTarget?): Rotation? {
        val dirInput = DirectionalInput(net.ccbluex.liquidbounce.utils.client.player.input)

        if (dirInput == DirectionalInput.NONE) {
            target ?: return null

            return getRotationForNoInput(target)
        }

        val direction = getMovementDirectionOfInput(net.ccbluex.liquidbounce.utils.client.player.yaw, dirInput) + 180

        // Round to 45°-steps (NORTH, NORTH_EAST, etc.)
        val movingYaw = round(direction / 45) * 45
        val isMovingStraight = movingYaw % 90 == 0f

        ScaffoldAutoJumpFeature.isGoingDiagonal = !isMovingStraight

        return if (isMovingStraight) {
            getRotationForStraightInput(movingYaw)
        } else {
            getRotationForDiagonalInput(movingYaw)
        }

    }

    private fun getRotationForStraightInput(movingYaw: Float): Rotation {
        return Rotation(movingYaw, 80f)
    }

    private fun getRotationForDiagonalInput(movingYaw: Float): Rotation {
        return Rotation(movingYaw, 75.6f)
    }

    private fun getRotationForNoInput(target: BlockPlacementTarget): Rotation {
        val axisMovement = floor(target.rotation.yaw / 90) * 90

        val yaw = axisMovement + 45
        val pitch = 75f

        return Rotation(yaw, pitch)
    }
}
