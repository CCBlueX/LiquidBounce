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

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.moving

/**
 * Parkour module
 *
 * Automatically jumps at the very edge of a block.
 */
object ModuleParkour : ClientModule("Parkour", Category.MOVEMENT) {

    @Suppress("unused")
    private val simulatedTickHandler = handler<MovementInputEvent> { event ->
        val simulatedPlayer = PlayerSimulationCache.getSimulationForLocalPlayer()
        val shouldJump = player.moving &&
                player.isOnGround &&
                !player.isSneaking &&
                !mc.options.sneakKey.isPressed &&
                !mc.options.jumpKey.isPressed &&
                !simulatedPlayer.getSnapshotAt(1).onGround

        if (shouldJump) {
            event.jump = true
        }
    }

}
