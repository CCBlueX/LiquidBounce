package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.features.module.modules.player.ModuleEagle
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.toDegrees
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.client.input.Input
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.jvm.optionals.getOrNull
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class DirectionalInput(
    val forwards: Boolean,
    val backwards: Boolean,
    val left: Boolean,
    val right: Boolean
) {
    constructor(input: Input) : this(input.pressingForward, input.pressingBack, input.pressingLeft, input.pressingRight)

    companion object {
        val NONE = DirectionalInput(forwards = false, backwards = false, left = false, right = false)
    }
}

val HORIZONTAL_DIRECTIONS: Array<Direction> = arrayOf(
    Direction.NORTH,
    Direction.EAST,
    Direction.SOUTH,
    Direction.WEST,
)

/**
 * Returns the yaw difference the position is from the player position
 *
 * @param positionRelativeToPlayer relative position to player
 */
fun getDegreesRelativeToPlayerView(positionRelativeToPlayer: Vec3d): Float {
    val player = mc.player!!
    val yaw = RotationManager.currentRotation?.yaw ?: player.yaw

    val optimalYaw =
        atan2(positionRelativeToPlayer.z, positionRelativeToPlayer.x).toFloat()
    val currentYaw = MathHelper.wrapDegrees(yaw).toRadians()

    return MathHelper.wrapDegrees((optimalYaw - currentYaw).toDegrees())
}

fun getDirectionalInputForDegrees(
    directionalInput: DirectionalInput,
    dgs: Float,
    deadAngle: Float = 20.0F
): DirectionalInput {
    var forwards = directionalInput.forwards
    var backwards = directionalInput.backwards
    var left = directionalInput.left
    var right = directionalInput.right

    if (dgs in -90.0F + deadAngle..90.0F - deadAngle) {
        forwards = true
    } else if (dgs < -90.0 - deadAngle || dgs > 90.0 + deadAngle) {
        backwards = true
    }

    if (dgs in 0.0F + deadAngle..180.0F - deadAngle) {
        right = true
    } else if (dgs in -180.0F + deadAngle..0.0F - deadAngle) {
        left = true
    }

    return DirectionalInput(forwards, backwards, left, right)
}

fun findEdgeCollision(from: Vec3d, to: Vec3d, currentPos: BlockPos = from.toBlockPos()): Vec3d? {
    if (!currentPos.canStandOn()) {
        // If the line enters this box, the player would fall of this block
        val fallOfBox = getFallOfBox(currentPos)

        val locationWherePlayerFallsOf = fallOfBox.raycast(
            to.add(to.subtract(from).normalize().multiply(sqrt(2.0))),
            from
        ).getOrNull()

        if (locationWherePlayerFallsOf != null) {
            return locationWherePlayerFallsOf
        }
    }

    // As long as the player moves inside this box, everything is ok.
    val blockSafetyArea = Box(currentPos).expand(0.3, 1.0, 0.3)

    val pointWhereLineLeavesBlock = blockSafetyArea.raycast(to, from).getOrNull()

    // The checked line ends within this block. No edge collisions where found
    if (pointWhereLineLeavesBlock == null)
        return null

    return findEdgeCollision(
        from = pointWhereLineLeavesBlock,
        to = to,
        currentPos = pointWhereLineLeavesBlock.toBlockPos()
    )
}

fun getFallOfBox(currentPos: BlockPos): Box {
    var minX = 0.0
    var minZ = 0.0
    var maxX = 1.0
    var maxZ = 1.0

    if (currentPos.add(1, 0, 0).canStandOn()) {
        maxX = 0.7
    }
    if (currentPos.add(-1, 0, 0).canStandOn()) {
        minX = 0.3
    }
    if (currentPos.add(0, 0, 1).canStandOn()) {
        maxZ = 0.7
    }
    if (currentPos.add(0, 0, -1).canStandOn()) {
        minZ = 0.3
    }

    return Box(Vec3d.of(currentPos).add(minX, 0.0, minZ), Vec3d.of(currentPos).add(maxX, 1.5, maxZ))
}
