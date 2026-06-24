/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog.SpeedHypixelLowHop
import net.ccbluex.liquidbounce.render.drawCircleOutline
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.combat.TargetSelector
import net.ccbluex.liquidbounce.utils.entity.anyHorizontal
import net.ccbluex.liquidbounce.utils.entity.horizontalSpeed
import net.ccbluex.liquidbounce.utils.entity.initial
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.untransformed
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.entity.wouldFallIntoVoid
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.phys.Vec3
import net.ccbluex.liquidbounce.utils.math.yaw
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Target Strafe Module
 *
 * Handles strafing around a locked target.
 */
object ModuleTargetStrafe : ClientModule("TargetStrafe", ModuleCategories.MOVEMENT) {

    private data class StrafePlan(
        val target: LivingEntity,
        val orbitRadius: Float,
        val strafeVec: Vec3,
        val pointCoords: Vec3,
        val pointValid: Boolean,
    )

    private val renderState = object {
        @JvmField var target: LivingEntity? = null
        @JvmField var orbitRadius: Float = 0F
        @JvmField var nextPoint: Vec3 = Vec3.ZERO
        @JvmField var nextPointValid: Boolean = false

        fun reset() {
            this.target = null
            this.orbitRadius = 0F
            this.nextPoint = Vec3.ZERO
            this.nextPointValid = false
        }
    }

    private var direction = 1

    // Configuration options
    private val modes = choices("Mode", MotionMode, arrayOf(MotionMode, InputMode)).apply { tagBy(this) }
    private val range = float("Range", 2.95f, 0.0f..8.0f)
    private val targetSelector = TargetSelector(range = range)
    private val followRangeValue = float("FollowRange", 4f, 0.0f..10.0f).onChange {
        it.coerceAtLeast(targetSelector.maxRange)
    }
    private val followRange get() = followRangeValue.get()

    private val requirements by multiEnumChoice<Requirements>("Requirements")

    private fun firstTarget() =
        ModuleKillAura.targetTracker.target
            ?: ModuleAimbot.targetTracker.target
            ?: targetSelector.targets().firstOrNull()

    private val requirementsMet
        get() = requirements.all { it.meets() }

    init {
        range.onChanged { updatedRange ->
            if (followRange < updatedRange) {
                followRangeValue.set(updatedRange)
            }
        }

        tree(Planner)
        tree(Visuals)
    }

    private object Visuals : ToggleableValueGroup(ModuleTargetStrafe, "Visuals", true) {

        init {
            doNotIncludeAlways()
        }

        private val width by float("Width", 0.12f, 0.01f..1.0f)
        private val heightOffset by float("HeightOffset", 0.05f, -1.0f..1.0f)

        private val outerColor by color("OuterColor", Color4b.LIQUID_BOUNCE.alpha(100))
        private val innerColor by color("InnerColor", Color4b.LIQUID_BOUNCE.alpha(20))
        private val outlineColor by color("OutlineColor", Color4b.LIQUID_BOUNCE.alpha(180))

        private val showNextPoint by boolean("ShowNextPoint", true)
        private val pointRadius by float("PointRadius", 0.18f, 0.05f..1.0f)
        private val pointColor by color("PointColor", Color4b.LIQUID_BOUNCE.alpha(90))
        private val pointOutlineColor by color("PointOutlineColor", Color4b.LIQUID_BOUNCE.alpha(180))
        private val invalidPointColor by color("InvalidPointColor", Color4b(255, 90, 90, 90))
        private val invalidPointOutlineColor by color("InvalidPointOutlineColor", Color4b(255, 90, 90, 180))

        override fun onDisabled() {
            renderState.reset()
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            val state = renderState
            val target = state.target ?: return@handler
            if (target.isRemoved) {
                renderState.reset()
                return@handler
            }

            renderEnvironmentForWorld(event.matrixStack) {
                val orbitOuterRadius = state.orbitRadius + width / 2f
                val orbitInnerRadius = (state.orbitRadius - width / 2f).coerceAtLeast(0f)
                val orbitPosition = target.interpolateCurrentPosition(event.partialTicks)
                    .add(0.0, heightOffset.toDouble(), 0.0)

                withPositionRelativeToCamera(orbitPosition) {
                    drawGradientCircle(orbitOuterRadius, orbitInnerRadius, outerColor, innerColor)
                    drawCircleOutline(orbitOuterRadius, outlineColor)
                }

                if (!showNextPoint) {
                    return@renderEnvironmentForWorld
                }

                val markerColor = if (state.nextPointValid) pointColor else invalidPointColor
                val markerOutlineColor = if (state.nextPointValid) pointOutlineColor else invalidPointOutlineColor

                withPositionRelativeToCamera(state.nextPoint.add(0.0, heightOffset.toDouble(), 0.0)) {
                    drawGradientCircle(pointRadius, 0f, markerColor, Color4b.TRANSPARENT)
                    drawCircleOutline(pointRadius, markerOutlineColor)
                }
            }
        }
    }

    object Planner : ToggleableValueGroup(ModuleTargetStrafe, "Planner", true) {
        val controlDirection by boolean("ControlDirection", true)

        init {
            tree(Validation)
            tree(AdaptiveRange)
        }

        object Validation : ToggleableValueGroup(Planner, "Validation", true) {

            init {
                tree(EdgeCheck)
                tree(VoidCheck)
            }

            object EdgeCheck : ToggleableValueGroup(Validation, "EdgeCheck", true) {
                val maxFallHeight by float("MaxFallHeight", 1.2f, 0.1f..4f)
            }

            object VoidCheck : ToggleableValueGroup(Validation, "VoidCheck", true) {
                val safetyExpand by float("SafetyExpand", 0.1f, 0.0f..5f)
            }

            /**
             * Validate if [point] is safe to strafe to
             */
            internal fun validatePoint(point: Vec3): Boolean {
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

            private fun validateCollision(point: Vec3, expand: Double = 0.0): Boolean {
                val hitbox = player.dimensions.makeBoundingBox(point).inflate(expand, 0.0, expand)
                return world.noCollision(player, hitbox)
            }

            private fun isCloseToFall(position: Vec3): Boolean {
                position.y = floor(position.y)
                val hitbox = player.dimensions
                    .makeBoundingBox(position)
                    .inflate(-0.05, 0.0, -0.05)
                    .move(0.0, -EdgeCheck.maxFallHeight.toDouble(), 0.0)
                return world.noCollision(player, hitbox)
            }
        }

        object AdaptiveRange : ToggleableValueGroup(Planner, "AdaptiveRange", false) {
            val maxRange by float("MaxRange", 4f, 1f..5f)
            val rangeStep by float("RangeStep", 0.5f, 0.1f..1.0f)
        }
    }

    object MotionMode : Mode("Motion") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val hypixel by boolean("Hypixel", false)

        // Event handler for player movement
        @Suppress("unused")
        private val moveHandler = handler<PlayerMoveEvent>(priority = MODEL_STATE) { event ->
            if (event.type != MoverType.SELF) {
                return@handler
            }

            if (!player.input.initial.anyHorizontal) {
                renderState.reset()
                return@handler
            }

            val strafePlan = computeStrafePlan(
                speed = player.horizontalSpeed,
                controlInput = DirectionalInput(player.input.untransformed)
            ) ?: return@handler

            if (!strafePlan.pointValid) {
                return@handler
            }

            // Perform the strafing movement
            if (hypixel && ModuleSpeed.running) {
                val minSpeed = if (player.onGround()) {
                    0.48
                } else {
                    0.281
                }

                if (SpeedHypixelLowHop.shouldStrafe) {
                    event.movement = event.movement.withStrafe(
                        yaw = strafePlan.strafeVec.yaw,
                        speed = player.horizontalSpeed.coerceAtLeast(minSpeed),
                        input = null
                    )
                } else {
                    event.movement = event.movement.withStrafe(
                        yaw = strafePlan.strafeVec.yaw,
                        speed = player.horizontalSpeed.coerceAtLeast(minSpeed),
                        strength = 0.02,
                        input = null
                    )
                }
            } else {
                event.movement = event.movement.withStrafe(
                    yaw = strafePlan.strafeVec.yaw,
                    speed = player.horizontalSpeed,
                    input = null
                )
            }
        }
    }

    object InputMode : Mode("Input") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused")
        private val inputHandler = handler<MovementInputEvent>(priority = MODEL_STATE) { event ->
            if (!event.directionalInput.isMoving) {
                renderState.reset()
                return@handler
            }

            val strafePlan = computeStrafePlan(
                speed = player.horizontalSpeed,
                controlInput = event.directionalInput
            ) ?: return@handler

            if (!strafePlan.pointValid) {
                return@handler
            }

            val movementDegrees = getDegreesRelativeToView(strafePlan.strafeVec, player.yRot)
            event.directionalInput = getDirectionalInputForDegrees(
                directionalInput = DirectionalInput.NONE,
                dgs = movementDegrees
            )
        }
    }

    /**
     * Computes the shared target-strafe plan used by both movement and input modes.
     */
    @Suppress("CognitiveComplexMethod")
    private fun computeStrafePlan(speed: Double, controlInput: DirectionalInput): StrafePlan? {
        if (!requirementsMet) {
            renderState.reset()
            return null
        }

        // Get the target entity, requires a locked target
        val target = firstTarget() ?: run {
            renderState.reset()
            return null
        }

        val playerPos = player.position()
        val targetPos = target.position()
        val distance = playerPos.horizontalDistanceTo(targetPos)

        // return if we're too far
        if (distance > followRange) {
            renderState.reset()
            return null
        }

        if (player.horizontalCollision) {
            direction = -direction
        }

        // Determine the direction to strafe
        if (Planner.controlDirection && !(controlInput.left && controlInput.right)) {
            when {
                controlInput.left -> direction = -1
                controlInput.right -> direction = 1
            }
        }

        val strafeYaw = atan2(targetPos.z - playerPos.z, targetPos.x - playerPos.x)
        fun createPlan(range: Float): StrafePlan {
            val strafeVec = computeDirectionVec(strafeYaw, distance, speed, range, direction)
            val pointCoords = playerPos.add(strafeVec)
            return StrafePlan(
                target = target,
                orbitRadius = range,
                strafeVec = strafeVec,
                pointCoords = pointCoords,
                pointValid = Planner.Validation.validatePoint(pointCoords),
            )
        }

        var plan = createPlan(targetSelector.maxRange)

        if (!plan.pointValid) {
            if (!Planner.AdaptiveRange.enabled) {
                direction = -direction
                plan = createPlan(targetSelector.maxRange)
            } else {
                var currentRange = Planner.AdaptiveRange.rangeStep
                while (!plan.pointValid) {
                    plan = createPlan(currentRange)
                    currentRange += Planner.AdaptiveRange.rangeStep

                    if (currentRange > Planner.AdaptiveRange.maxRange) {
                        direction = -direction
                        plan = createPlan(targetSelector.maxRange)
                        break
                    }
                }
            }
        }

        renderState.target = plan.target
        renderState.orbitRadius = plan.orbitRadius
        renderState.nextPoint = plan.pointCoords
        renderState.nextPointValid = plan.pointValid

        return plan
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
    ): Vec3 {
        val yaw = strafeYaw - Mth.HALF_PI
        val encirclement = maxOf(-speed, distance - range)
        val encirclementX = -sin(yaw) * encirclement
        val encirclementZ = cos(yaw) * encirclement
        val strafeX = -sin(strafeYaw) * speed * direction
        val strafeZ = cos(strafeYaw) * speed * direction
        return Vec3(encirclementX + strafeX, 0.0, encirclementZ + strafeZ)
    }

    @Suppress("unused")
    private enum class Requirements(
        override val tag: String,
        val meets: () -> Boolean
    ) : Tagged {
        SPACE("Space", {
            mc.options.keyJump.isDown
        }),
        SPEED("Speed", {
            ModuleSpeed.running
        }),
        KILLAURA("KillAura", {
            ModuleKillAura.running
        }),
        GROUND("Ground", {
            player.onGround()
        });
    }
}
