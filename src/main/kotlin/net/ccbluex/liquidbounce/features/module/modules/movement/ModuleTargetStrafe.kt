package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTargetStrafe.MotionMode.Validation.validatePoint
import net.ccbluex.liquidbounce.utils.entity.*
import net.minecraft.util.math.Vec3d
import java.lang.Math.toDegrees
import kotlin.math.*

/**
 * Target Strafe Module
 *
 * Handles strafing around a locked target.
 *
 * TODO: Implement visuals
 */
object ModuleTargetStrafe : Module("TargetStrafe", Category.MOVEMENT) {

    // Configuration options
    private val modes = choices<Choice>("Mode", MotionMode, arrayOf(MotionMode))
    private val range by float("Range", 2f, 0.0f..10.0f)
    private val requiresSpace by boolean("RequiresSpace", false)

    object MotionMode : Choice("Motion") {
        override val parent: ChoiceConfigurable<Choice> get() = modes

        private val controlDirection by boolean("ControlDirection", true)
        //private val adaptiveRange by boolean("AdaptiveRange", false) // TODO

        init {
            tree(Validation)
        }

        object Validation : ToggleableConfigurable(MotionMode, "Validation", true) {
            private val edgeCheck by boolean("EdgeCheck", true)

            init {
                tree(VoidCheck)
            }

            object VoidCheck : ToggleableConfigurable(Validation, "VoidCheck", true) {
                val safetyExpand by float("SafetyExpand", 0.1f, 0.0f..5.0f)
            }

            /**
             * Validate if [point] is safe to strafe to
             */
            internal fun validatePoint(point: Vec3d): Boolean {
                if (!this.enabled) {
                    return true
                }

                if (edgeCheck && player.wouldBeCloseToFallOff(point)) {
                    return false
                }

                if (VoidCheck.enabled && player.wouldFallIntoVoid(point,
                        safetyExpand = VoidCheck.safetyExpand.toDouble())) {
                    return false
                }

                return true
            }
        }

        private var direction = 1

        // Event handler for player movement
        val moveHandler = handler<PlayerMoveEvent> { event ->
            // If the player is not pressing any movement keys, we exit early
            if (!player.pressingMovementButton) {
                return@handler
            }

            // If the player is not pressing the jump key and requires space, we exit early
            if (requiresSpace && !mc.options.jumpKey.isPressed) {
                return@handler
            }

            if (player.horizontalCollision) {
                direction = -direction
            }

            // Determine the direction to strafe
            if (!(player.input.pressingLeft && player.input.pressingRight) && controlDirection) {
                when {
                    player.input.pressingLeft -> direction = -1
                    player.input.pressingRight -> direction = 1
                }
            }

            // Get the target entity, requires a locked target from KillAura
            val target = ModuleKillAura.targetTracker.lockedOnTarget ?: return@handler
            val speed = player.sqrtSpeed

            val distance = sqrt((player.pos.x - target.pos.x).pow(2.0) + (player.pos.z - target.pos.z).pow(2.0))
            val strafeYaw = atan2(target.pos.z - player.pos.z, target.pos.x - player.pos.x)
            val yaw = strafeYaw - (0.5f * Math.PI)

            val encirclement = if (distance - range < -speed) -speed else distance - range
            val encirclementX = -sin(yaw) * encirclement
            val encirclementZ = cos(yaw) * encirclement
            var strafeX = -sin(strafeYaw) * speed * direction
            var strafeZ = cos(strafeYaw) * speed * direction
            val pointCoords = Vec3d(player.pos.x + encirclementX + strafeX, player.pos.y, player.pos.z + encirclementZ + strafeZ)

            // todo fix validation
//            if (!validatePoint(pointCoords)) {
//                direction *= -1
//                strafeX = -sin(strafeYaw) * speed * direction
//                strafeZ = cos(strafeYaw) * speed * direction
//            }

            // Perform the strafing movement
            event.movement.strafe(
                yaw = toDegrees(atan2(-(encirclementX + strafeX), encirclementZ + strafeZ)).toFloat(),
                speed = player.sqrtSpeed,
                keyboardCheck = false
            )
        }
    }
}
