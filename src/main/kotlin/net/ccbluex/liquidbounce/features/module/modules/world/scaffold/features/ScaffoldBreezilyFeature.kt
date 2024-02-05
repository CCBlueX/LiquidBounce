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

    private val edgeDistance by floatRange(
        "EdgeDistance", 0.45f..0.5f, 0.25f..0.5f, "blocks"
    )

    fun doBreezilyIfNeeded(event: MovementInputEvent) {
        if (!enabled || !event.directionalInput.forwards || player.isSneaking
            || ScaffoldAutoJumpFeature.isGoingDiagonal) {
            return
        }

        if (world.getBlockState(player.blockPos.offset(Direction.DOWN, 1)).block == Blocks.AIR) {
            lastAirTime = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - lastAirTime > 500) {
            return
        }

        val modX = player.x - floor(player.x)
        val modZ = player.z - floor(player.z)

        val ma = 1 - currentEdgeDistanceRandom
        var currentSideways = 0f
        when (Direction.fromRotation(player.yaw.toDouble())) {
            Direction.SOUTH -> {
                if (modX > ma) currentSideways = 1f
                if (modX < currentEdgeDistanceRandom) currentSideways = -1f
            }

            Direction.NORTH -> {
                if (modX > ma) currentSideways = -1f
                if (modX < currentEdgeDistanceRandom) currentSideways = 1f
            }

            Direction.EAST -> {
                if (modZ > ma) currentSideways = -1f
                if (modZ < currentEdgeDistanceRandom) currentSideways = 1f
            }

            Direction.WEST -> {
                if (modZ > ma) currentSideways = 1f
                if (modZ < currentEdgeDistanceRandom) currentSideways = -1f
            }
            else -> {
                // do nothing
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
        val dirInput = DirectionalInput(player.input)

        if (dirInput == DirectionalInput.NONE) {
            target ?: return null

            return getRotationForNoInput(target)
        }

        val direction = getMovementDirectionOfInput(player.yaw, dirInput) + 180

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

    private fun getRotationForStraightInput(movingYaw: Float) = Rotation(movingYaw, 80f)

    private fun getRotationForDiagonalInput(movingYaw: Float) = Rotation(movingYaw, 75.6f)

    private fun getRotationForNoInput(target: BlockPlacementTarget): Rotation {
        val axisMovement = floor(target.rotation.yaw / 90) * 90

        val yaw = axisMovement + 45
        val pitch = 75f

        return Rotation(yaw, pitch)
    }
}
