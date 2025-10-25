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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.sneaking

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

internal class NoSlowSneakingSwitch(override val parent: ChoiceConfigurable<*>) : Choice("Switch") {
    private val timingMode by enumChoice("Timing", TimingMode.PRE_POST)

    @Suppress("unused")
    private val networkTickHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        when (timingMode) {
            TimingMode.PRE_POST -> when (event.state) {
                EventState.PRE -> {
                    network.sendPacket(ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                }
                EventState.POST -> {
                    network.sendPacket(ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
                }
            }
            TimingMode.PRE_TICK -> if (event.state == EventState.PRE) {
                network.sendPacket(ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                network.sendPacket(ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
            }
            TimingMode.POST_TICK -> if (event.state == EventState.POST) {
                network.sendPacket(ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                network.sendPacket(ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
            }
        }
    }

    private enum class TimingMode(override val choiceName: String) : NamedChoice {
        PRE_POST("PreAndPost"),
        PRE_TICK("Pre"),
        POST_TICK("Post")
    }
}
