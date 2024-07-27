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
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
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
    private val range by float("Range", 2f, 0.0f..8.0f)
    private val requiresSpace by boolean("RequiresSpace", false)

    object MotionMode : Choice("Motion") {
        override val parent: ChoiceConfigurable<Choice> get() = modes

        private val controlDirection by boolean("ControlDirection", true)

        init {
            tree(Validation)
            tree(AdaptiveRange)
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

        object AdaptiveRange : ToggleableConfigurable(MotionMode, "AdaptiveRange", false) {
            val maxRange by float("MaxRange", 4f, 1f..5f)
            val rangeStep by float("RangeStep", 0.5f, 0.0f..1.0f)
            val hitboxExpand by float("HitboxExpand", 0.0f, 0.0f..0.5f)
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

            var strafeVec = computeDirectionVec(strafeYaw, distance, speed, range, direction)
            var pointCoords = player.pos.add(strafeVec)

            if (!validateCollision(pointCoords)) {
                if (!AdaptiveRange.enabled) {
                    direction *= -1
                    strafeVec = computeDirectionVec(strafeYaw, distance, speed, range, direction)
                } else {
                    var currentRange = 0.0f
                    while (!validateCollision(pointCoords)) {
                        strafeVec = computeDirectionVec(strafeYaw, distance, speed, currentRange, direction)
                        pointCoords = player.pos.add(strafeVec)
                        currentRange += AdaptiveRange.rangeStep
                        if (currentRange > AdaptiveRange.maxRange) {
                            direction *= -1
                            strafeVec = computeDirectionVec(strafeYaw, distance, speed, range, direction)
                            break
                        }
                    }
                }
            }

            // Perform the strafing movement
            event.movement.strafe(
                yaw = toDegrees(atan2(-strafeVec.x, strafeVec.z)).toFloat(),
                speed = player.sqrtSpeed,
                keyboardCheck = false
            )
        }

        /**
         * Computes the direction vector for strafing
         */
        fun computeDirectionVec(strafeYaw: Double, distance: Double, speed: Double, range: Float, direction: Int): Vec3d {
            val yaw = strafeYaw - (0.5f * Math.PI)
            var encirclement = if (distance - range < -speed) -speed else distance - range
            var encirclementX = -sin(yaw) * encirclement
            var encirclementZ = cos(yaw) * encirclement
            var strafeX = -sin(strafeYaw) * speed * direction
            var strafeZ = cos(strafeYaw) * speed * direction
            return Vec3d(encirclementX + strafeX, 0.0, encirclementZ + strafeZ)
        }

        fun validateCollision(point: Vec3d, expand: Double = 0.0): Boolean {
            val hitbox = player.dimensions.getBoxAt(point).expand(expand, 0.0, expand)

            return world.isSpaceEmpty(player, hitbox)
        }
    }
}
