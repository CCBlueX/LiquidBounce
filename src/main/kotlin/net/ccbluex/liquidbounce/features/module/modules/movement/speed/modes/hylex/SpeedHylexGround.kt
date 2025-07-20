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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.hylex

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.minecraft.entity.effect.StatusEffects

/**
 * Hylex Ground
 *
 * Works because of a silly exemption from Hylex
 * @author @liquidsquid1
 */
class SpeedHylexGround(override val parent: ChoiceConfigurable<*>) : Choice("HylexGround") {

    private var groundTicks = 0

    override fun enable() {
        groundTicks = 0
        super.enable()
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (!player.isOnGround) {
            groundTicks = 0
            return@tickHandler
        }

        groundTicks++

        if (groundTicks <= 5) {
            return@tickHandler
        }

        if (player.hurtTime >= 1) {
            return@tickHandler
        }

        if ((player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) >= 1) {
            return@tickHandler
        }

        if (!(mc.options.leftKey.isPressed || mc.options.rightKey.isPressed)) {
            player.velocity = player.velocity.multiply(
                1.2174,
                1.0,
                1.2174
            )
            return@tickHandler
        }

        player.velocity = player.velocity.multiply(
            1.214,
            1.0,
            1.214
        )
    }
}
