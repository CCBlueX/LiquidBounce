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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.consume

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.math.Direction

/**
 * tested on mineblaze.net
 */

internal class NoSlowConsumeIntave14(override val parent: ChoiceConfigurable<*>) : Choice("Intave14") {
    private val mode by enumChoice("Mode", Mode.RELEASE)

    private fun releasePacket() {
        network.sendPacket(
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                player.blockPos,
                Direction.UP
            )
        )
    }

    @Suppress("unused")
    private val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        if (event.state == EventState.PRE) {
            when (mode) {
                Mode.RELEASE -> {
                    if (player.isUsingItem) {
                        releasePacket()
                    }

                    if (player.itemUseTime == 3) {
                        player.stopUsingItem()
                        releasePacket()
                    }
                }

                Mode.NEW -> {
                    if (player.itemUseTime <= 2 || player.itemUseTimeLeft == 0) {
                        releasePacket()
                    }
                }
            }
        }
    }

    private enum class Mode(override val choiceName: String) : NamedChoice {
        RELEASE("Release"),
        NEW("New")
    }
}
