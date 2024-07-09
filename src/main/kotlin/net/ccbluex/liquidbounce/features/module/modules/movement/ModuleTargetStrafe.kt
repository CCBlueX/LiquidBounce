package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.ccbluex.liquidbounce.utils.math.squaredXZDistanceTo
import net.minecraft.util.math.Vec3d
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

        private val points by int("Points", 12, 2..90)
        private val adjustSpeed by boolean("AdjustSpeed", false)
        private val alternateAlgorithm by boolean("AlternateAlgorithm", true)

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

        private const val DOUBLE_PI = PI * 2
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
            if (!(player.input.pressingLeft && player.input.pressingRight)) {
                when {
                    player.input.pressingLeft -> direction = 1
                    player.input.pressingRight -> direction = -1
                }
            }

            // Get the target entity, requires a locked target from KillAura
            val target = ModuleKillAura.targetTracker.lockedOnTarget ?: return@handler

            // Get potential points for strafing around the target
            val targetPoints = getTargetPoints(target.pos)
            var yawOffset = 0

            val point = if (sqrt(ModuleKillAura.targetTracker.maximumDistance) > range) {
                targetPoints.minByOrNull { it.squaredXZDistanceTo(player.pos) } ?: return@handler
            } else {
                val sortedPoints = targetPoints.sortedBy { it.squaredXZDistanceTo(player.pos) }.toMutableList()

                if (sortedPoints.size <= 1) {
                    return@handler
                }

                sortedPoints.removeFirst()

                val closestPoint = sortedPoints.minByOrNull { it.squaredXZDistanceTo(player.nextTickPos) }
                    ?: return@handler

                if (alternateAlgorithm && sqrt(closestPoint.squaredXZDistanceTo(player.pos)) <= range + 0.1) {
                    yawOffset = direction * 90
                }

                findNextPoint(sortedPoints, closestPoint) ?: return@handler
            }

            // Calculate the rotation for strafing
            val rotationPoint = if (alternateAlgorithm) target.pos else point
            val yaw = (RotationManager.makeRotation(rotationPoint, player.pos).yaw + yawOffset).toValidYaw()

            // Perform the strafing movement
            event.movement.strafe(
                yaw = yaw,
                speed = if (adjustSpeed) {
                    min(player.sqrtSpeed, sqrt(point.squaredXZDistanceTo(player.pos)))
                } else {
                    player.sqrtSpeed
                },
                keyboardCheck = false
            )
        }

        /**
         * Generate points around the target for strafing
         *
         * @param targetPos The position of the target
         */
        private fun getTargetPoints(targetPos: Vec3d): List<Vec3d> {
            val (targetX, targetY, targetZ) = targetPos
            return (0 until points).mapNotNull { i ->
                val cos = range * cos(i * DOUBLE_PI / points)
                val sin = range * sin(i * DOUBLE_PI / points)
                val point = Vec3d(targetX + cos, targetY, targetZ + sin)
                if (Validation.validatePoint(point)) point else null
            }
        }

        /**
         * Find the next valid point for strafing
         */
        private fun findNextPoint(sortedPoints: MutableList<Vec3d>, closestPoint: Vec3d): Vec3d? {
            var nextPoint: Vec3d? = null
            var lastNonNull: Vec3d? = null
            var offset = 0

            do {
                offset++
                if (offset >= sortedPoints.size) {
                    break
                }

                val nextIndex = (sortedPoints.indexOf(closestPoint) + offset * direction).let {
                    when {
                        it < 0 -> sortedPoints.size - 1
                        it >= sortedPoints.size -> 0
                        else -> it
                    }
                }

                if (nextPoint != null) {
                    lastNonNull = nextPoint
                }
                nextPoint = sortedPoints.getOrNull(nextIndex)
            } while (nextPoint == null || sqrt(nextPoint.squaredXZDistanceTo(player.pos)) < player.sqrtSpeed)

            return nextPoint ?: lastNonNull
        }
    }
}
