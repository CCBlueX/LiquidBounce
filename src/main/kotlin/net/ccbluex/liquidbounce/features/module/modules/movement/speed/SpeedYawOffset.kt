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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.Priority

/**
 * Makes you go faster by strategically strafing
 */
object SpeedYawOffset : ToggleableConfigurable(ModuleSpeed, "YawOffset", false) {

    private val yawOffsetMode by enumChoice("YawOffsetMode", YawOffsetMode.AIR)
    private val rotationsConfigurable = RotationsConfigurable(this)

    private var yaw = 0f

    @Suppress("unused")
    private val yawOffsetHandler = handler<RotationUpdateEvent> {
        when (yawOffsetMode) {
            YawOffsetMode.GROUND -> groundYawOffset() // makes you strafe more on ground
            YawOffsetMode.AIR -> airYawOffset() // 45deg strafe on air
            YawOffsetMode.CONSTANT -> constantYawOffset()
        }

        val rotation = Rotation(player.yaw - yaw, player.pitch)

        RotationManager.setRotationTarget(
            rotationsConfigurable.toRotationTarget(rotation),
            Priority.NOT_IMPORTANT,
            ModuleSpeed
        )
    }

    private fun groundYawOffset(): Float {
        yaw = if (player.isOnGround) {
            when {
                mc.options.forwardKey.isPressed && mc.options.leftKey.isPressed -> 45f
                mc.options.forwardKey.isPressed && mc.options.rightKey.isPressed -> -45f
                mc.options.backKey.isPressed && mc.options.leftKey.isPressed -> 135f
                mc.options.backKey.isPressed && mc.options.rightKey.isPressed -> -135f
                mc.options.backKey.isPressed -> 180f
                mc.options.leftKey.isPressed -> 90f
                mc.options.rightKey.isPressed -> -90f
                else -> 0f
            }
        } else {
            0f
        }
        return 0f
    }

    private fun constantYawOffset(): Float {
        yaw = when {
            mc.options.forwardKey.isPressed && mc.options.leftKey.isPressed -> 45f
            mc.options.forwardKey.isPressed && mc.options.rightKey.isPressed -> -45f
            mc.options.backKey.isPressed && mc.options.leftKey.isPressed -> 135f
            mc.options.backKey.isPressed && mc.options.rightKey.isPressed -> -135f
            mc.options.backKey.isPressed -> 180f
            mc.options.leftKey.isPressed -> 90f
            mc.options.rightKey.isPressed -> -90f
            else -> 0f
        }

        return 0f
    }

    private fun airYawOffset(): Float {
        yaw = when {
            !player.isOnGround &&
                mc.options.forwardKey.isPressed &&
                !mc.options.leftKey.isPressed &&
                !mc.options.rightKey.isPressed
                -> -45f

            else -> 0f
        }
        return 0f
    }

    private enum class YawOffsetMode(override val choiceName: String) : NamedChoice {
        GROUND("Ground"),
        AIR("Air"),
        CONSTANT("Constant")
    }

}
