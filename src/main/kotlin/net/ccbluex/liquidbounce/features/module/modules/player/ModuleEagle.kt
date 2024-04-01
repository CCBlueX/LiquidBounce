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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

/**
 * An eagle module
 *
 * Legit trick to build faster.
 */
object ModuleEagle : Module("Eagle", Category.PLAYER, aliases = arrayOf("FastBridge", "BridgeAssistant")) {

    val edgeDistance by float("EagleEdgeDistance", 0.4f, 0.01f..1.3f)

    val repeatable = handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
        val shouldBeActive = !player.abilities.flying && player.isOnGround

        if (shouldBeActive && player.isCloseToEdge(it.directionalInput, edgeDistance.toDouble())) {
            it.sneaking = true
        }
    }

}
