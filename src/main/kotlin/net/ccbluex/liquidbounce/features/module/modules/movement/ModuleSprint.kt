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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSprintControlFeature
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.math.MathHelper

/**
 * Sprint module
 *
 * Sprints automatically.
 */

object ModuleSprint : ClientModule("Sprint", Category.MOVEMENT) {

    private enum class SprintMode(override val choiceName: String) : NamedChoice {
        LEGIT("Legit"),
        OMNIDIRECTIONAL("Omnidirectional"),
        OMNIROTATIONAL("Omnirotational"),
    }

    private val sprintMode by enumChoice("Mode", SprintMode.LEGIT)

    private val ignore by multiEnumChoice<Ignore>("Ignore")

    /**
     * This is used to stop sprinting when the player is not moving forward
     * without a velocity fix enabled.
     */
    private val stopOn by multiEnumChoice("StopOn", StopOn.entries)

    val shouldSprintOmnidirectional: Boolean
        get() = running && sprintMode == SprintMode.OMNIDIRECTIONAL ||
            ScaffoldSprintControlFeature.allowOmnidirectionalSprint

    val shouldIgnoreBlindness
        get() = running && Ignore.BLINDNESS in ignore

    val shouldIgnoreHunger
        get() = running && Ignore.HUNGER in ignore

    val shouldIgnoreCollision
        get() = running && Ignore.COLLISION in ignore

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = CRITICAL_MODIFICATION) { event ->
        if (!event.directionalInput.isMoving) {
            return@handler
        }

        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            event.sprint = true
        }
    }

    @Suppress("unused")
    private val sprintPreventionHandler = handler<SprintEvent> { event ->
        // In this case we want to prevent sprinting on movement tick only,
        // because otherwise you could guess from the input change that this is automated.
        if (event.source == SprintEvent.Source.MOVEMENT_TICK && shouldPreventSprint()) {
            event.sprint = false
        }
    }

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> { event ->
        if (sprintMode == SprintMode.OMNIDIRECTIONAL && shouldSprintOmnidirectional) {
            // Allows us to sprint boost in every direction
            event.yaw = getMovementDirectionOfInput(player.yaw, DirectionalInput(player.input))
        }
    }

    // DO NOT USE TREE TO MAKE SURE THAT THE ROTATIONS ARE NOT CHANGED
    private val rotationsConfigurable = RotationsConfigurable(this)

    @Suppress("unused")
    private val omniRotationalHandler = handler<GameTickEvent> {
        // Check if omnirotational sprint is enabled
        if (sprintMode != SprintMode.OMNIROTATIONAL) {
            return@handler
        }

        val yaw = getMovementDirectionOfInput(player.yaw, DirectionalInput(player.input))

        // todo: unhook pitch - AimPlan needs support for only yaw or pitch operation
        val rotation = Rotation(yaw, player.pitch)

        RotationManager.setRotationTarget(rotationsConfigurable.toRotationTarget(rotation), Priority.NOT_IMPORTANT,
            this@ModuleSprint)
    }

    @Suppress("MagicNumber")
    fun shouldPreventSprint(): Boolean {
        val deltaYaw = player.yaw - (RotationManager.currentRotation ?: return false).yaw
        val (forward, sideways) = Pair(player.input.movementForward, player.input.movementSideways)

        val hasForwardMovement = forward * MathHelper.cos(deltaYaw * 0.017453292f) + sideways *
            MathHelper.sin(deltaYaw * 0.017453292f) > 1.0E-5
        val preventSprint = (if (player.isOnGround) StopOn.GROUND in stopOn else StopOn.AIR in stopOn)
            && !shouldSprintOmnidirectional
            && RotationManager.activeRotationTarget?.movementCorrection == MovementCorrection.OFF
            && !hasForwardMovement

        return running && preventSprint
    }

    private enum class Ignore(override val choiceName: String) : NamedChoice {
        BLINDNESS("Blindness"),
        HUNGER("Hunger"),
        COLLISION("Collision")
    }

    private enum class StopOn(override val choiceName: String) : NamedChoice {
        GROUND("Ground"),
        AIR("Air")
    }
}
