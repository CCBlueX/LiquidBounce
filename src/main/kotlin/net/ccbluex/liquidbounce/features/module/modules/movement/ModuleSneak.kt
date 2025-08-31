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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.SneakNetworkEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.entity.moving
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

/**
 * Sneak module
 *
 * Automatically sneaks all the time.
 */
object ModuleSneak : ClientModule("Sneak", Category.MOVEMENT) {

    var modes = choices("Mode", Vanilla, arrayOf(Legit, Vanilla, Switch)).apply { tagBy(this) }
    var notDuringMove by boolean("NotDuringMove", false)

    private object Legit : Choice("Legit") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        private val inputHandler = handler<MovementInputEvent> { event ->
            if (player.moving && notDuringMove) {
                return@handler
            }

            // Temporarily override sneaking
            event.sneak = true
        }

    }

    private object Vanilla : Choice("Vanilla") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        private val sneakNetworkHandler = handler<SneakNetworkEvent> { event ->
            if (player.moving && notDuringMove) {
                return@handler
            }

            event.sneak = true
        }

    }

    private object Switch : Choice("Switch") {

        var networkSneaking = false

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        val networkTick = handler<PlayerNetworkMovementTickEvent> { event ->
            if (player.moving && notDuringMove) {
                disable()
                return@handler
            }

            when (event.state) {
                EventState.PRE -> {
                    if (networkSneaking) {
                        network.sendPacket(
                            ClientCommandC2SPacket(
                                player,
                                ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY
                            )
                        )
                        networkSneaking = false
                    }
                }

                EventState.POST -> {
                    if (networkSneaking) {
                        network.sendPacket(
                            ClientCommandC2SPacket(
                                player,
                                ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY
                            )
                        )
                        networkSneaking = true
                    }
                }
            }
        }

        override fun disable() {
            if (networkSneaking) {
                network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                networkSneaking = false
            }
        }
    }

}
