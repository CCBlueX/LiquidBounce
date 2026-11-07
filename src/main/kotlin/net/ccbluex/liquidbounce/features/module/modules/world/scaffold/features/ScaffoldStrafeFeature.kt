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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.entity.withStrafe

object ScaffoldStrafeFeature : ToggleableValueGroup(ModuleScaffold, "Strafe", false) {

    private val speed by float("Speed", 0.247f, 0.0f..5.0f)
    private val onlyOnGround by boolean("OnlyOnGround", false)

    @Suppress("unused")
    private val strafeHandler = handler<GameTickEvent> {
        if (onlyOnGround && !player.onGround()) {
            return@handler
        }

        player.deltaMovement = player.deltaMovement.withStrafe(speed = speed.toDouble())
    }
}
