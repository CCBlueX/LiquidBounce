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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSprintControlFeature
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.isSlowDueToUsingItem
import net.ccbluex.liquidbounce.utils.entity.movementForward
import net.ccbluex.liquidbounce.utils.entity.movementSideways
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

/**
 * Sprint module
 *
 * Sprints automatically.
 */

object ModuleSprint : ClientModule("Sprint", ModuleCategories.MOVEMENT) {

    private enum class SprintMode(override val tag: String) : Tagged {
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
            event.yaw = getMovementDirectionOfInput(player.yRot, DirectionalInput(player.input))
        }
    }

    // DO NOT USE TREE TO MAKE SURE THAT THE ROTATIONS ARE NOT CHANGED
    private val rotations = RotationsValueGroup(this)

    @Suppress("unused")
    private val omniRotationalHandler = handler<GameTickEvent> {
        // Check if omnirotational sprint is enabled
        if (sprintMode != SprintMode.OMNIROTATIONAL) {
            return@handler
        }

        val yaw = getMovementDirectionOfInput(player.yRot, DirectionalInput(player.input))

        // todo: unhook pitch - AimPlan needs support for only yaw or pitch operation
        val rotation = Rotation(yaw, player.xRot)

        RotationManager.setRotationTarget(rotations.toRotationTarget(rotation), Priority.NOT_IMPORTANT,
            this@ModuleSprint)
    }

    private fun shouldPreventSprint(): Boolean {
        if (StopOn.USING_ITEM in stopOn && player.isSlowDueToUsingItem) {
            return true
        }

        val deltaYawRad = (player.yRot - (RotationManager.currentRotation ?: return false).yaw).toRadians()
        val forward = player.input.movementForward
        val sideways = player.input.movementSideways

        val hasForwardMovement = forward * deltaYawRad.fastCos() + sideways * deltaYawRad.fastSin() > 1.0E-5

        return (if (player.onGround()) StopOn.GROUND in stopOn else StopOn.AIR in stopOn)
            && !shouldSprintOmnidirectional
            && RotationManager.activeRotationTarget?.movementCorrection == MovementCorrection.OFF
            && !hasForwardMovement
    }

    private enum class Ignore(override val tag: String) : Tagged {
        BLINDNESS("Blindness"),
        HUNGER("Hunger"),
        COLLISION("Collision"),
    }

    private enum class StopOn(override val tag: String) : Tagged {
        GROUND("Ground"),
        AIR("Air"),
        USING_ITEM("UsingItem"),
    }
}
