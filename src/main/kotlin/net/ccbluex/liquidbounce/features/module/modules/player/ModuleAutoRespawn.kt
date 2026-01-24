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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.minecraft.client.gui.screens.DeathScreen

/**
 * AutoRespawn module
 *
 * Automatically respawns the player after dying.
 */
object ModuleAutoRespawn : ClientModule("AutoRespawn", ModuleCategories.PLAYER) {

    // There is a delay until the button is clickable on the death screen (20 ticks)
    private val delay by int("Delay", 0, 0..20, "ticks")

    val screenHandler = sequenceHandler<ScreenEvent> {
        if (it.screen is DeathScreen) {
            if (delay > 0) {
                waitTicks(delay)
            }

            player.respawn()
            mc.setScreen(null)
        }
    }

}
