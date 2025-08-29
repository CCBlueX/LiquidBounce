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
package net.ccbluex.liquidbounce.features.module.modules.movement.noweb

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleAvoidHazards
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebAir
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebGrimBreak
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebIntave14
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.NoWebStrafe
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.util.math.BlockPos

/**
 * NoWeb module
 *
 * Disables web slowdown.
 */
object ModuleNoWeb : ClientModule("NoWeb", Category.MOVEMENT) {

    init {
        enableLock()
    }

    val modes = choices(
        "Mode", NoWebAir, arrayOf(
            NoWebAir,
            NoWebGrimBreak,
            NoWebIntave14,
            NoWebStrafe
        )
    ).apply { tagBy(this) }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (ModuleAvoidHazards.enabled && ModuleAvoidHazards.cobWebs) {
            ModuleAvoidHazards.enabled = false

            notification(
                "Compatibility error", "NoWeb is incompatible with AvoidHazards",
                NotificationEvent.Severity.ERROR
            )
            waitTicks(40)
        }
    }

    /**
     * Handle cobweb collision
     *
     * @see net.minecraft.block.CobwebBlock.onEntityCollision
     * @return if we should cancel the slowdown effect
     */
    fun handleEntityCollision(pos: BlockPos): Boolean {
        if (!running) {
            return false
        }

        return modes.activeChoice.handleEntityCollision(pos)
    }
}
