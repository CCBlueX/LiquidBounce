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
package net.ccbluex.liquidbounce.utils.navigation

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkNavigator
import net.minecraft.world.phys.Vec3

/**
 * Base class for features that walk to a goal, with a [WalkNavigator] that aims with the feature's [rotations].
 */
abstract class NavigationBaseValueGroup<T>(
    parent: EventListener? = null,
    name: String,
    enabled: Boolean,
    rotations: RotationsValueGroup? = null,
    allowBreak: Boolean = false,
    allowPlace: Boolean = false,
    allowParkour: Boolean = true,
    showRoute: Boolean = true,
) : ToggleableValueGroup(parent, name, enabled) {

    private val navigator = WalkNavigator(this, this, rotations, allowBreak, allowPlace, allowParkour, showRoute)

    /**
     * Whether a block is being mined or placed for the path.
     */
    val isActing: Boolean
        get() = navigator.isActing

    /**
     * Creates context for navigation
     */
    protected abstract fun createNavigationContext(): T

    /**
     * Calculates the desired position to move towards
     *
     * @return Target position as Vec3d
     */
    protected abstract fun calculateGoalPosition(context: T): Vec3?

    /**
     * How close to the goal is close enough, in blocks.
     */
    protected open fun goalRadius(context: T): Double = DEFAULT_GOAL_RADIUS

    /**
     * Adds to the movement while walking straight on, such as jumping towards a target.
     *
     * @param event Movement input event to modify
     */
    protected open fun handleMovementAssist(event: MovementInputEvent, context: T) = Unit

    /**
     * Gets rotation based on movement and target
     *
     * @return Movement rotation
     */
    open fun getMovementRotation(): Rotation = navigator.movementRotation()

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent>(
        priority = CRITICAL_MODIFICATION
    ) { event ->
        val context = createNavigationContext()
        val goal = calculateGoalPosition(context)
        if (goal == null) {
            navigator.halt()
            return@handler
        }

        ModuleDebug.debugGeometry(
            this,
            "Goal",
            ModuleDebug.DebuggedPoint(goal, Color4b.BLUE, size = 0.4)
        )

        if (navigator.navigate(event, goal, goalRadius(context))) {
            handleMovementAssist(event, context)
        }
    }

    override fun onDisabled() {
        navigator.reset()
    }

    private companion object {
        const val DEFAULT_GOAL_RADIUS = 1.0
    }

}
