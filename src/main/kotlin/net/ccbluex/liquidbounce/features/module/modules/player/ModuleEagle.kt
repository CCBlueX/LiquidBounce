/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.StateUpdateEvent
import net.minecraft.block.SideShapeType
import net.minecraft.util.math.Direction

/**
 * A eagle module
 *
 * Legit trick to build faster.
 */
object ModuleEagle : Module("Eagle", Category.PLAYER) {

    val repeatable = handler<StateUpdateEvent> {
        // Check if player is on the edge and is NOT flying
        val pos = player.blockPos.down()
        val isAir = !pos.getState()!!.isSideSolid(mc.world!!, pos, Direction.UP, SideShapeType.CENTER) && !player.abilities.flying

        if (isAir) {
            it.state.enforceEagle = true
        }
    }

}
