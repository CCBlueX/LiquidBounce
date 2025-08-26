/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.input.isPressed
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.navigation.NavigationBaseConfigurable
import net.minecraft.client.option.Perspective
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

/**
 * FreeCam module
 *
 * Allows you to move out of your body.
 */
object ModuleFreeCam : ClientModule("FreeCam", Category.RENDER, disableOnQuit = true) {

    private val speed by float("Speed", 1f, 0.1f..2f)

    /**
     * Allows to interact from the camera perspective. This is very useful to interact with blocks that
     * are behind the player or walls. Similar functionality to the GhostBlock module.
     */
    private object CameraInteract : ToggleableConfigurable(ModuleFreeCam, "AllowCameraInteract", true) {
        val lookAt by boolean("LookAt", true)
    }

    /**
     * Navigation configuration for the FreeCam module
     */
    private object Navigation : NavigationBaseConfigurable<Unit>(ModuleFreeCam, "Navigation", false) {

        private val controlKey by key("Key", InputUtil.GLFW_KEY_LEFT_CONTROL)

        val shouldBeGoing
            get() = running && controlKey != InputUtil.UNKNOWN_KEY && controlKey.isPressed

        /**
         * Creates context for navigation
         */
        override fun createNavigationContext() { }

        /**
         * Calculates the desired position to move towards
         *
         * @return Target position as Vec3d
         */
        override fun calculateGoalPosition(context: Unit): Vec3d? {
            if (!shouldBeGoing) {
                return null
            }

            val pos = pos ?: return null
            val cameraPosition = pos.interpolate(1f)
            val target = raycast(
                range = 100.0,
                start = cameraPosition,
                direction = mc.cameraEntity?.rotation?.directionVector ?: return null
            )

            return target.pos
        }

    }

    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    init {
        tree(CameraInteract)
        tree(Navigation)
    }

    private data class PositionPair(var pos: Vec3d, var lastPos: Vec3d) {
        operator fun plusAssign(velocity: Vec3d) {
            lastPos = pos
            pos += velocity
        }

        fun interpolate(tickDelta: Float) = Vec3d(
            lastPos.x + (pos.x - lastPos.x) * tickDelta,
            lastPos.y + (pos.y - lastPos.y) * tickDelta,
            lastPos.z + (pos.z - lastPos.z) * tickDelta
        )

    }

    private var pos: PositionPair? = null

    override fun onEnabled() {
        updatePosition(Vec3d.ZERO)
        super.onEnabled()
    }

    override fun onDisabled() {
        pos = null

        // Reset player rotation
        val rotation = RotationManager.currentRotation ?: RotationManager.serverRotation
        player.yaw = rotation.yaw
        player.pitch = rotation.pitch
        super.onDisabled()
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
        val velocity = Vec3d.of(Vec3i.ZERO)
            .withStrafe(speed, input = event.directionalInput)
            .withAxis(Direction.Axis.Y, yAxisMovement * speed)
        ModuleDebug.debugParameter(this, "Velocity", velocity.toString())
        updatePosition(velocity)

        event.directionalInput = DirectionalInput.NONE
        event.jump = false
        event.sneak = false
    }

    @Suppress("unused")
    private val perspectiveHandler = handler<PerspectiveEvent> { event ->
        event.perspective = Perspective.FIRST_PERSON
    }

    @Suppress("unused")
    private val rotationHandler = handler<RotationUpdateEvent> {
        val lookAt = if (Navigation.shouldBeGoing) {
            // Look at target position
            Navigation.getMovementRotation()
        } else if (CameraInteract.running && CameraInteract.lookAt) {
            // Aim at crosshair target
            val crosshairTarget = mc.crosshairTarget ?: return@handler
            Rotation.lookingAt(crosshairTarget.pos, player.eyePos)
        } else {
            return@handler
        }

        RotationManager.setRotationTarget(rotationsConfigurable.toRotationTarget(lookAt),
            Priority.NOT_IMPORTANT, ModuleFreeCam)
    }

    fun applyCameraPosition(entity: Entity, tickDelta: Float) {
        val camera = mc.gameRenderer.camera

        if (!running || entity != player) {
            return
        }

        return camera.setPos(pos?.interpolate(tickDelta) ?: return)
    }

    fun renderPlayerFromAllPerspectives(entity: LivingEntity): Boolean {
        if (!running || entity != player) {
            return entity.isSleeping
        }

        return entity.isSleeping || !mc.gameRenderer.camera.isThirdPerson
    }

    /**
     * Modify the raycast position
     */
    fun modifyRaycast(original: Vec3d, entity: Entity, tickDelta: Float): Vec3d {
        if (!running || entity != mc.player || !CameraInteract.running) {
            return original
        }

        return pos?.interpolate(tickDelta) ?: original
    }

    fun shouldDisableCameraInteract() = running && !CameraInteract.running

    private fun updatePosition(velocity: Vec3d) {
        pos = (pos ?: PositionPair(player.eyePos, player.eyePos)).apply { this += velocity }
    }

}
