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

package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.modes
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.blockingHand
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.isBlocking
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.untracked
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket

internal object NoSlowBlockingSwitch : Choice("Switch") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    private val timingMode by enumChoice("Timing", TimingMode.PRE_POST)

    @Suppress("unused")
    val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        // This should if done correctly only work with main-hand blocking.
        // But as we know from experience often things are not done correctly on anti-cheats.
        // Main-hand blocking only applies when using VFP 1.8 client-side protocol translation.

        if (isBlocking) {
            when (timingMode) {
                TimingMode.PRE_TICK -> {
                    if (event.state == EventState.PRE) {
                        untracked {
                            network.sendPacket(UpdateSelectedSlotC2SPacket(
                                (player.inventory.selectedSlot + 1) % 8)
                            )
                            network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))

                            // For some reason we do not have to re-interact with the item to start blocking again.
                            // The server will still think we are blocking.
                        }
                    }
                }
                TimingMode.POST_TICK -> {
                    if (event.state == EventState.POST) {
                        untracked {
                            network.sendPacket(UpdateSelectedSlotC2SPacket(
                                (player.inventory.selectedSlot + 1) % 8)
                            )
                            network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))

                            // For some reason we do not have to re-interact with the item to start blocking again.
                            // The server will still think we are blocking.
                        }
                    }
                }

                /**
                 * On PreAndPost, we first switch to the off-hand slot, then back to the main-hand slot and
                 * start blocking again.
                 */
                TimingMode.PRE_POST -> {
                    when (event.state) {
                        EventState.PRE -> {
                            untracked {
                                network.sendPacket(UpdateSelectedSlotC2SPacket(
                                    (player.inventory.selectedSlot + 1) % 8)
                                )
                            }
                        }

                        EventState.POST -> {
                            untracked {
                                network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                                interaction.sendSequencedPacket(world) { sequence ->
                                    PlayerInteractItemC2SPacket(blockingHand, sequence, player.yaw, player.pitch)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private enum class TimingMode(override val choiceName: String) : NamedChoice {
        PRE_POST("PreAndPost"),
        PRE_TICK("Pre"),
        POST_TICK("Post")
    }

}
