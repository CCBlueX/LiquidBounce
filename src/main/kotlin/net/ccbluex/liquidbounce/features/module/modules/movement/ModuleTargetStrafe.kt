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

// TODO: needs visuals
object ModuleTargetStrafe : Module("TargetStrafe", Category.MOVEMENT) {

    private val modes = choices<Choice>("Mode", MotionMode, arrayOf(MotionMode))

    private val range by float("Range", 5.0f, 0.0f..10.0f)

    private val onlyOnJump by boolean("RequireSpace", false)

    object MotionMode : Choice("Motion") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val points by int("Points", 12, 2..90)

        private val adjustSpeed by boolean("AdjustSpeed", true)

        private val alternateAlgorithm by boolean("AlternateAlgorithm", false)

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

            fun validatePoint(point: Vec3d): Boolean {
                if (!this.enabled) return true

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

        val tickHandler = handler<PlayerMoveEvent> { event ->
            if (!player.pressingMovementButton) return@handler
            if (onlyOnJump && !mc.options.jumpKey.isPressed) return@handler

            if (player.horizontalCollision) {
                direction = -direction
            }

            if (player.input.pressingLeft) {
                direction = 1
            } else if (player.input.pressingRight) {
                direction = -1
            }

            val target = ModuleKillAura.targetTracker.lockedOnTarget ?: return@handler

            var targetPoints = mutableListOf<Vec3d>()

            val (targetX, targetY, targetZ) = target.pos

            for (i in 0 until points) {
                val cos = range * cos(i * DOUBLE_PI / points)
                val sin = range * sin(i * DOUBLE_PI / points)

                val pointX = targetX + cos
                val pointZ = targetZ + sin

                val point = Vec3d(pointX, targetY, pointZ)

                if (Validation.validatePoint(point)) {
                    targetPoints += point
                }
            }

            var yawOffset = 0;

            val point = if (sqrt(ModuleKillAura.targetTracker.maximumDistance) > range) {
                targetPoints.minByOrNull { it.squaredXZDistanceTo(player.pos) } ?: return@handler
            } else {
                targetPoints = targetPoints.sortedBy { it.squaredXZDistanceTo(player.pos) }.toMutableList().also {
                    it.removeFirst()
                }
                val closest = targetPoints.minByOrNull { it.squaredXZDistanceTo(player.nextTickPos) } ?: return@handler

                if (alternateAlgorithm && sqrt(closest.squaredXZDistanceTo(player.pos)) <= range + 0.1) {
                    yawOffset = direction * 90
                }

                var nextPoint: Vec3d? = null
                var lastNonNull: Vec3d? = null
                var offset = 0
                val pointCount = targetPoints.size

                do {
                    offset++
                    if (offset >= pointCount) return@handler

                    val nextIndex = targetPoints.indexOf(closest) + offset * direction

                    if (nextPoint != null) lastNonNull = nextPoint

                    nextPoint = targetPoints.getOrNull(
                        if (nextIndex < 0) {
                            pointCount - 1
                        } else if (nextIndex >= pointCount) {
                            0
                        } else {
                            nextIndex
                        }
                    )

                } while (nextPoint == null || sqrt(nextPoint.squaredXZDistanceTo(player.pos)) < player.sqrtSpeed)

                @Suppress("USELESS_ELVIS")
                // IntelliJ seems completely convinced this elvis is useless,
                // my mind says otherwise, so I'm keeping it, sorry if it is useless
                nextPoint ?: (lastNonNull ?: return@handler)
            }

            val yaw = (RotationManager.makeRotation(
                if (alternateAlgorithm) target.pos else point,
                player.pos).yaw + yawOffset).toValidYaw()
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

    }

}
