package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldMovementPlanner
import net.ccbluex.liquidbounce.utils.client.QuickAccess.player
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.util.math.Vec3d

object ScaffoldStabilizeMovementFeature : ToggleableConfigurable(ModuleScaffold, "StabilizeMovement", true) {
    private const val MAX_CENTER_DEVIATION: Double = 0.2
    private const val MAX_CENTER_DEVIATION_IF_MOVING_TOWARDS: Double = 0.075

    val moveEvent =
        handler<MovementInputEvent>(priority = -10) { event ->
            val optimalLine = ModuleScaffold.currentOptimalLine ?: return@handler
            val currentInput = event.directionalInput

            val nearestPointOnLine = optimalLine.getNearestPointTo(player.pos)

            val vecToLine = nearestPointOnLine.subtract(player.pos)
            val horizontalVelocity = Vec3d(player.velocity.x, 0.0, player.velocity.z)
            val isRunningTowardsLine = vecToLine.dotProduct(horizontalVelocity) > 0.0

            val maxDeviation =
                if (isRunningTowardsLine) {
                    MAX_CENTER_DEVIATION_IF_MOVING_TOWARDS
                } else {
                    MAX_CENTER_DEVIATION
                }

            if (nearestPointOnLine.squaredDistanceTo(player.pos) < maxDeviation * maxDeviation) {
                return@handler
            }

            val dgs = getDegreesRelativeToView(nearestPointOnLine.subtract(player.pos), player.yaw)

            val newDirectionalInput = getDirectionalInputForDegrees(DirectionalInput.NONE, dgs, deadAngle = 0.0F)

            val frontalAxisBlocked = currentInput.forwards || currentInput.backwards
            val sagitalAxisBlocked = currentInput.right || currentInput.left

            event.directionalInput =
                DirectionalInput(
                    if (frontalAxisBlocked) currentInput.forwards else newDirectionalInput.forwards,
                    if (frontalAxisBlocked) currentInput.backwards else newDirectionalInput.backwards,
                    if (sagitalAxisBlocked) currentInput.left else newDirectionalInput.left,
                    if (sagitalAxisBlocked) currentInput.right else newDirectionalInput.right,
                )
        }
}
