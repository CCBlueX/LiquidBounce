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
@file:Suppress("detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.HealthUpdateEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.input.isPressed
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseValueGroup
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPoint
import net.minecraft.client.CameraType
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW

/**
 * FreeCam module
 *
 * Allows you to move out of your body.
 */
object ModuleFreeCam : ClientModule("FreeCam", ModuleCategories.RENDER, disableOnQuit = true) {

    private val speed by float("Speed", 1f, 0.1f..2f)

    /**
     * Allows to interact from the camera perspective. This is very useful to interact with blocks that
     * are behind the player or walls. Similar functionality to the GhostBlock module.
     */
    private object CameraInteract : ToggleableValueGroup(ModuleFreeCam, "AllowCameraInteract", true) {
        val lookAt by boolean("LookAt", true)
    }

    /**
     * This is useful for cancelling FreeCam on certain events.
     * For example, when the player takes damage.
     */
    private enum class CancelOn(override val tag: String) : Tagged {
        DAMAGE("Damage"),
        MOVE("Move"),
        LIQUID("Liquid"),
    }

    private val cancelOn by multiEnumChoice("CancelOn", enumSetOf<CancelOn>())

    /**
     * Navigation configuration for the FreeCam module
     */
    private object Navigation : NavigationBaseValueGroup<Unit>(ModuleFreeCam, "Navigation", false) {

        private val controlKey by key("Key", InputConstants.KEY_LCONTROL)

        val shouldBeGoing
            get() = running && controlKey != InputConstants.UNKNOWN && controlKey.isPressed

        /**
         * Creates context for navigation
         */
        override fun createNavigationContext() {
            // Nothing to do
        }

        /**
         * Calculates the desired position to move towards
         *
         * @return Target position as Vec3d
         */
        override fun calculateGoalPosition(context: Unit): Vec3? {
            return if (shouldBeGoing) {
                getCameraLookingAt()
            } else {
                null
            }
        }

    }

    private val midClickCameraTeleport by boolean("MidClickCameraTeleport", false)

    private val keepSneaking by boolean("KeepSneaking", false)

    private val rotations = tree(RotationsValueGroup(this))

    init {
        tree(CameraInteract)
        tree(Navigation)
    }

    private object PositionState {
        var available: Boolean = false
            set(value) {
                if (value) {
                    pos = player.eyePosition
                    lastPos = pos
                } else {
                    pos = Vec3.ZERO
                    lastPos = Vec3.ZERO
                }
                field = value
            }

        var pos: Vec3 = Vec3.ZERO
            private set
        private var lastPos: Vec3 = Vec3.ZERO

        fun set(target: Vec3) {
            lastPos = pos
            pos = target
        }

        fun update(velocity: Vec3) = set(pos + velocity)

        fun interpolate(tickDelta: Float) = lastPos.lerp(pos, tickDelta.toDouble())
    }

    override fun onEnabled() {
        PositionState.available = true
        super.onEnabled()
    }

    override fun onDisabled() {
        PositionState.available = false

        // Reset player rotation
        val rotation = RotationManager.currentRotation ?: RotationManager.serverRotation
        player.yRot = rotation.yaw
        player.xRot = rotation.pitch
        super.onDisabled()
    }

    @Suppress("unused")
    private val mouseHandler = handler<MouseButtonEvent> { event ->
        if (midClickCameraTeleport &&
            event.action == GLFW.GLFW_PRESS && event.button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            val target = getCameraLookingAt() ?: return@handler

            // interpolate to prevent tp into block
            PositionState.set(PositionState.pos.lerp(target, 0.9))
        }
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent>(priority = FIRST_PRIORITY) { event ->
        val speed = this.speed.toDouble()
        val yAxisMovement = when {
            event.jump -> 1.0f
            event.sneak -> -1.0f
            else -> 0.0f
        }

        ModuleDebug.debugParameter(this, "DirectionalInput", event.directionalInput)
        val velocity = Vec3.ZERO
            .withStrafe(speed, input = event.directionalInput)
            .with(Direction.Axis.Y, yAxisMovement * speed)
        ModuleDebug.debugParameter(this, "Velocity", velocity)
        PositionState.update(velocity)

        event.directionalInput = DirectionalInput.NONE
        event.jump = false
        event.sneak = false
    }

    @Suppress("unused")
    private val forceSneakHandler = handler<MovementInputEvent>(priority = OBJECTION_AGAINST_EVERYTHING) { event ->
        if (keepSneaking) {
            event.sneak = true
        }
    }

    @Suppress("unused")
    private val perspectiveHandler = handler<PerspectiveEvent> { event ->
        event.perspective = CameraType.FIRST_PERSON
    }

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val lookAt = if (Navigation.shouldBeGoing) {
            // Look at target position
            Navigation.getMovementRotation()
        } else if (CameraInteract.running && CameraInteract.lookAt) {
            // Aim at crosshair target
            val crosshairTarget = mc.hitResult ?: return@handler
            Rotation.lookingAt(crosshairTarget.location, player.eyePosition)
        } else {
            return@handler
        }

        RotationManager.setRotationTarget(rotations.toRotationTarget(lookAt),
            Priority.NOT_IMPORTANT, ModuleFreeCam)
    }

    @Suppress("unused")
    private val healthHandler = handler<HealthUpdateEvent> { event ->
        val tookDamage = event.health < event.previousHealth

        if (CancelOn.DAMAGE in cancelOn && tookDamage) {
            this.enabled = false
        }
    }

    @Suppress("unused")
    private val moveHandler = handler<PlayerMoveEvent> { event ->
        // Don't check movement.y because it's gravity / falling motion
        if (CancelOn.MOVE in cancelOn && (event.movement.y > 0 || event.movement.z > 0)) {
            this.enabled = false
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent> { event ->
        if (CancelOn.LIQUID in cancelOn && player.isInLiquid) {
            this.enabled = false
        }
    }

    fun applyCameraPosition(entity: Entity, tickDelta: Float) {
        if (!running || entity != player || !PositionState.available) {
            return
        }

        val camera = mc.gameRenderer.mainCamera

        return camera.setPosition(PositionState.interpolate(tickDelta))
    }

    fun renderPlayerFromAllPerspectives(entity: LivingEntity): Boolean {
        if (!running || entity != player) {
            return entity.isSleeping
        }

        return entity.isSleeping || !mc.gameRenderer.mainCamera.isDetached
    }

    /**
     * Modify the raycast position
     */
    fun modifyRaycast(original: Vec3, entity: Entity, tickDelta: Float): Vec3 {
        if (!running || entity != mc.player || !CameraInteract.running || !PositionState.available) {
            return original
        }

        return PositionState.interpolate(tickDelta)
    }

    fun shouldDisableCameraInteract() = running && !CameraInteract.running

    private fun getCameraLookingAt(): Vec3? {
        if (!PositionState.available) return null

        val cameraPosition = PositionState.interpolate(1f)
        val target = traceFromPoint(
            range = 200.0,
            start = cameraPosition,
            direction = mc.cameraEntity?.rotation?.directionVector ?: return null
        )

        return target.location
    }

}
