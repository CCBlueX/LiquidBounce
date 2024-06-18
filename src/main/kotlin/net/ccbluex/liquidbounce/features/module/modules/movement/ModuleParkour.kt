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

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

/**
 * Parkour module
 *
 * Automatically jumps at the very edge of a block.
 */
object ModuleParkour : Module("Parkour", Category.MOVEMENT) {

    private val edgeDistance by float("EdgeDistance", 0.01f, 0.01f..0.5f)

    @Suppress("unused")
    val tickJumpHandler = handler<MovementInputEvent> {
        val shouldJump = player.moving &&
                player.isOnGround &&
                !player.isSneaking &&
                !mc.options.sneakKey.isPressed &&
                !mc.options.jumpKey.isPressed &&
                player.isCloseToEdge(DirectionalInput(player.input), edgeDistance.toDouble())

        if (shouldJump) {
            it.jumping = true
        }
    }

}
