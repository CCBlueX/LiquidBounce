/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.util.math.MathHelper

/**
 * Sprint module
 *
 * Sprints automatically.
 */

object ModuleSprint : Module("Sprint", Category.MOVEMENT) {

    enum class SprintMode(override val choiceName: String) : NamedChoice {
        LEGIT("Legit"),
        OMNIDIRECTIONAL("Omnidirectional"),
        OMNIROTATIONAL("Omnirotational"),
    }

    private val sprintMode by enumChoice("Mode", SprintMode.LEGIT)

    private val ignoreBlindness by boolean("IgnoreBlindness", false)
    private val ignoreHunger by boolean("IgnoreHunger", false)
    private val stopOnGround by boolean("StopOnGround", true)
    private val stopOnAir by boolean("StopOnAir", true)

    // DO NOT USE TREE TO MAKE SURE THAT THE ROTATIONS ARE NOT CHANGED
    private val rotationsConfigurable = RotationsConfigurable(this)

    fun shouldSprintOmnidirectionally() = enabled && sprintMode == SprintMode.OMNIDIRECTIONAL

    fun shouldIgnoreBlindness() = enabled && ignoreBlindness

    fun shouldIgnoreHunger() = enabled && ignoreHunger

    fun shouldPreventSprint(): Boolean {
        val deltaYaw = player.yaw - (RotationManager.currentRotation ?: return false).yaw
        val (forward, sideways) = Pair(player.input.movementForward, player.input.movementSideways)

        val hasForwardMovement = forward * MathHelper.cos(deltaYaw * 0.017453292f) + sideways *
                MathHelper.sin(deltaYaw * 0.017453292f) > 1.0E-5
        val preventSprint = (if (player.isOnGround) stopOnGround else stopOnAir)
            && !shouldSprintOmnidirectionally()
            && RotationManager.storedAimPlan?.applyVelocityFix == false && !hasForwardMovement

        return enabled && preventSprint
    }

    @Suppress("unused")
    private val omniRotationalHandler = handler<GameTickEvent> {
        // Check if omnirotational sprint is enabled
        if (sprintMode != SprintMode.OMNIROTATIONAL) {
            return@handler
        }

        val yaw = when {
            mc.options.forwardKey.isPressed && mc.options.leftKey.isPressed -> 45f
            mc.options.forwardKey.isPressed && mc.options.rightKey.isPressed -> -45f
            mc.options.backKey.isPressed && mc.options.leftKey.isPressed -> 135f
            mc.options.backKey.isPressed && mc.options.rightKey.isPressed -> -135f
            mc.options.backKey.isPressed -> 180f
            mc.options.leftKey.isPressed -> 90f
            mc.options.rightKey.isPressed -> -90f
            else -> return@handler
        }

        // todo: unhook pitch - AimPlan needs support for only yaw or pitch operation
        RotationManager.aimAt(rotationsConfigurable.toAimPlan(Rotation(player.yaw - yaw, player.pitch)), Priority.NOT_IMPORTANT,
            this@ModuleSprint)
    }


}
