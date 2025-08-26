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
package net.ccbluex.liquidbounce.features.command.commands.ingame

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

/**
 * Center command
 *
 * Centers you at your current position.
 */
object CommandCenter : CommandFactory, EventListener {

    var state = CenterHandlerState.INACTIVE

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("center")
            .requiresIngame()
            .handler { _, _ -> state = CenterHandlerState.APPLY_ON_NEXT_EVENT }
            .build()
    }

    @Suppress("unused")
    private val moveHandler =
        handler<PlayerNetworkMovementTickEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
            if (it.state == EventState.POST) {
                return@handler
            }

            val pos = player.blockPos.toCenterPos()
            val delta = player.pos.subtract(pos)
            it.x = delta.x
            it.y = delta.y
            it.z = delta.z
            state = CenterHandlerState.INACTIVE
        }

    override val running: Boolean
        get() = super.running && inGame && state == CenterHandlerState.APPLY_ON_NEXT_EVENT

    enum class CenterHandlerState {
        INACTIVE,
        APPLY_ON_NEXT_EVENT
    }

}
