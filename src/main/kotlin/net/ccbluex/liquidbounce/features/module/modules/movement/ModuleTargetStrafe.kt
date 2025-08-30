package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog.SpeedHypixelLowHop
import net.ccbluex.liquidbounce.utils.combat.TargetSelector
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
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
object ModuleTargetStrafe : ClientModule("TargetStrafe", Category.MOVEMENT) {

    // Configuration options
    private val modes = choices<Choice>("Mode", MotionMode, arrayOf(MotionMode)).apply { tagBy(this) }
    private val targetSelector = TargetSelector(range = float("Range", 2.95f, 0.0f..8.0f))
    private val followRange by float("FollowRange", 4f, 0.0f..10.0f).onChange {
        it.coerceAtLeast(targetSelector.maxRange)
    }

    private val requirements by multiEnumChoice<Requirements>("Requirements")

    private val requirementsMet
        get() = requirements.all { it.meets() }

    object MotionMode : Choice("Motion") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val controlDirection by boolean("ControlDirection", true)
        private val hypixel by boolean("Hypixel", false)

        init {
            tree(Validation)
            tree(AdaptiveRange)
        }

        object Validation : ToggleableConfigurable(MotionMode, "Validation", true) {

            init {
                tree(EdgeCheck)
                tree(VoidCheck)
            }

            object EdgeCheck : ToggleableConfigurable(Validation, "EdgeCheck", true) {
                val maxFallHeight by float("MaxFallHeight", 1.2f, 0.1f..4f)
            }

            object VoidCheck : ToggleableConfigurable(Validation, "VoidCheck", true) {
                val safetyExpand by float("SafetyExpand", 0.1f, 0.0f..5f)
            }

            /**
             * Validate if [point] is safe to strafe to
             */
            internal fun validatePoint(point: Vec3d): Boolean {
                if (!validateCollision(point)) {
                    return false
                }

                if (!enabled) {
                    return true
                }

                if (EdgeCheck.enabled && isCloseToFall(point)) {
                    return false
                }

                if (VoidCheck.enabled && player.wouldFallIntoVoid(
                        point,
                        safetyExpand = VoidCheck.safetyExpand.toDouble()
                    )
                ) {
                    return false
                }

                return true
            }

            private fun validateCollision(point: Vec3d, expand: Double = 0.0): Boolean {
                val hitbox = player.dimensions.getBoxAt(point).expand(expand, 0.0, expand)

                return world.isSpaceEmpty(player, hitbox)
            }

            private fun isCloseToFall(position: Vec3d): Boolean {
                position.y = floor(position.y)
                val hitbox =
                    player.dimensions
                        .getBoxAt(position)
                        .expand(-0.05, 0.0, -0.05)
                        .offset(0.0, -EdgeCheck.maxFallHeight.toDouble(), 0.0)

                return world.isSpaceEmpty(player, hitbox)
            }

        }

        object AdaptiveRange : ToggleableConfigurable(MotionMode, "AdaptiveRange", false) {
            val maxRange by float("MaxRange", 4f, 1f..5f)
            val rangeStep by float("RangeStep", 0.5f, 0.0f..1.0f)
        }

        private var direction = 1

        // Event handler for player movement
        @Suppress("unused")
        private val moveHandler = handler<PlayerMoveEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
            // If the player is not pressing any movement keys, we exit early
            if (!player.input.initial.any) {
                return@handler
            }

            if (!requirementsMet) {
                return@handler
            }

            // Get the target entity, requires a locked target
            val target = ModuleKillAura.targetTracker.target
                ?: ModuleAimbot.targetTracker.target
                ?: targetSelector.targets().firstOrNull() ?: return@handler
            val distance = sqrt((player.pos.x - target.pos.x).pow(2.0) + (player.pos.z - target.pos.z).pow(2.0))

            // return if we're too far
            if (distance > followRange) {
                return@handler
            }

            if (player.horizontalCollision) {
                direction = -direction
            }

            // Determine the direction to strafe
            if (!(player.input.untransformed.left && player.input.untransformed.right) && controlDirection) {
                when {
                    player.input.untransformed.left -> direction = -1
                    player.input.untransformed.right -> direction = 1
                }
            }

            val speed = player.sqrtSpeed
            val strafeYaw = atan2(target.pos.z - player.pos.z, target.pos.x - player.pos.x)
            var strafeVec = computeDirectionVec(strafeYaw, distance, speed, targetSelector.maxRange, direction)
            var pointCoords = player.pos.add(strafeVec)

            if (!Validation.validatePoint(pointCoords)) {
                if (!AdaptiveRange.enabled) {
                    direction = -direction
                    strafeVec = computeDirectionVec(strafeYaw, distance, speed, targetSelector.maxRange, direction)
                } else {
                    var currentRange = AdaptiveRange.rangeStep
                    while (!Validation.validatePoint(pointCoords)) {
                        strafeVec = computeDirectionVec(strafeYaw, distance, speed, currentRange, direction)
                        pointCoords = player.pos.add(strafeVec)
                        currentRange += AdaptiveRange.rangeStep
                        if (currentRange > AdaptiveRange.maxRange) {
                            direction = -direction
                            strafeVec = computeDirectionVec(
                                strafeYaw, distance, speed, targetSelector.maxRange, direction
                            )
                            break
                        }
                    }
                }
            }

            // Perform the strafing movement
            if (hypixel && ModuleSpeed.running) {
                val minSpeed = if (player.isOnGround) {
                    0.48
                } else {
                    0.281
                }

                if (SpeedHypixelLowHop.shouldStrafe) {
                    event.movement = event.movement.withStrafe(
                        yaw = toDegrees(atan2(-strafeVec.x, strafeVec.z)).toFloat(),
                        speed = player.sqrtSpeed.coerceAtLeast(minSpeed),
                        input = null
                    )
                } else {
                    event.movement = event.movement.withStrafe(
                        yaw = toDegrees(atan2(-strafeVec.x, strafeVec.z)).toFloat(),
                        speed = player.sqrtSpeed.coerceAtLeast(minSpeed),
                        strength = 0.02,
                        input = null
                    )
                }
            } else {
                event.movement = event.movement.withStrafe(
                    yaw = toDegrees(atan2(-strafeVec.x, strafeVec.z)).toFloat(),
                    speed = player.sqrtSpeed,
                    input = null
                )
            }
        }

        /**
         * Computes the direction vector for strafing
         */
        private fun computeDirectionVec(
            strafeYaw: Double,
            distance: Double,
            speed: Double,
            range: Float,
            direction: Int
        ): Vec3d {
            val yaw = strafeYaw - (0.5f * Math.PI)
            val encirclement = if (distance - range < -speed) -speed else distance - range
            val encirclementX = -sin(yaw) * encirclement
            val encirclementZ = cos(yaw) * encirclement
            val strafeX = -sin(strafeYaw) * speed * direction
            val strafeZ = cos(strafeYaw) * speed * direction
            return Vec3d(encirclementX + strafeX, 0.0, encirclementZ + strafeZ)
        }

    }

    @Suppress("unused")
    private enum class Requirements(
        override val choiceName: String,
        val meets: () -> Boolean
    ) : NamedChoice {
        SPACE("Space", {
            mc.options.jumpKey.isPressed
        }),
        SPEED("Speed", {
            ModuleSpeed.running
        }),
        KILLAURA("KillAura", {
            ModuleKillAura.running
        }),
        GROUND("Ground", {
           player.isOnGround
        });
    }
}
