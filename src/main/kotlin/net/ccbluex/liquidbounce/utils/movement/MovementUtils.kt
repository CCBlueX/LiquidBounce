package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.toDegrees
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.minecraft.client.input.Input
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2

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
