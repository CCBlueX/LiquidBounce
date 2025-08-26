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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower

import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.isBlockBelow
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE

object ScaffoldTowerPulldown : ScaffoldTower("Pulldown") {

    private val triggerMotion by float("Trigger", 0.1f, 0.0f..0.2f, "Y/v")

    @Suppress("unused")
    private val jumpHandler = sequenceHandler<PlayerJumpEvent>(priority = READ_FINAL_STATE) { event ->
        if (event.motion == 0f || event.isCancelled) {
            return@sequenceHandler
        }

        // Wait until we can proceed with our tower
        waitUntil { player.velocity.y < triggerMotion && !player.isOnGround }
        if (!isBlockBelow) return@sequenceHandler

        player.velocity.y = -1.0
    }

}
