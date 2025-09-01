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
package net.ccbluex.liquidbounce.utils.navigation

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MinecraftAutoJumpEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.util.math.Vec3d

/**
 * Base class for navigation-related features that handles common movement functionality
 */
abstract class NavigationBaseConfigurable<T>(
    parent: EventListener? = null,
    name: String,
    enabled: Boolean
) : ToggleableConfigurable(parent, name, enabled) {

    private val autoAction by multiEnumChoice("Auto", AutoAction.entries)

    private inline val autoJump get() = AutoAction.JUMP in autoAction
    private inline val autoSwim get() = AutoAction.SWIM in autoAction
    private inline val autoSprint get() = AutoAction.SPRINT in autoAction

    /**
     * Creates context for navigation
     */
    protected abstract fun createNavigationContext(): T

    /**
     * Calculates the desired position to move towards
     *
     * @return Target position as Vec3d
     */
    protected abstract fun calculateGoalPosition(context: T): Vec3d?

    /**
     * Handles additional movement mechanics like swimming and jumping
     *
     * @param event Movement input event to modify
     */
    @Suppress("ComplexCondition")
    protected open fun handleMovementAssist(event: MovementInputEvent, context: T) {
        if ((autoSwim && player.isTouchingWater) || (autoJump && player.horizontalCollision)) {
            event.jump = true
        }
    }

    /**
     * Converts goal position into directional input
     *
     * @param currentInput Current directional input
     * @param goal Target position to move towards
     * @return Calculated directional input
     */
    private fun calculateDirectionalInput(currentInput: DirectionalInput, goal: Vec3d): DirectionalInput {
        val degrees = getDegreesRelativeToView(goal.subtract(player.pos), player.yaw)
        return getDirectionalInputForDegrees(currentInput, degrees, deadAngle = 20.0F)
    }

    /**
     * Gets rotation based on movement and target
     *
     * @return Movement rotation
     */
    open fun getMovementRotation(): Rotation {
        val movementYaw = getMovementDirectionOfInput(player.yaw, DirectionalInput(player.input))
        val movementPitch = 0.0f

        return Rotation(movementYaw, movementPitch)
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent>(
        priority = CRITICAL_MODIFICATION
    ) { event ->
        val context = createNavigationContext()
        val goal = calculateGoalPosition(context) ?: return@handler

        ModuleDebug.debugGeometry(
            this,
            "Goal",
            ModuleDebug.DebuggedPoint(goal, Color4b.BLUE, size = 0.4)
        )

        event.directionalInput = calculateDirectionalInput(event.directionalInput, goal)
        handleMovementAssist(event, context)
    }

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (!autoSprint || !event.directionalInput.isMoving) {
            return@handler
        }

        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            event.sprint = true
        }
    }

    @Suppress("unused")
    private val autoJumpHandler = handler<MinecraftAutoJumpEvent> { event ->
        if (autoJump) {
            event.autoJump = true
        }
    }

    private enum class AutoAction(override val choiceName: String) : NamedChoice {
        JUMP("Jump"),
        SWIM("Swim"),
        SPRINT("Sprint")
    }
}
