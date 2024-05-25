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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.ModuleNoSlow
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedGrim2860
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared.NoSlowSharedGrim2860MC18
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.network.listener.ServerPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.UseAction

internal object NoSlowBlock : ToggleableConfigurable(ModuleNoSlow, "Blocking", true) {

    val forwardMultiplier by float("Forward", 1f, 0.2f..1f)
    val sidewaysMultiplier by float("Sideways", 1f, 0.2f..1f)
    val onlySlowOnServerSide by boolean("OnlySlowOnServerSide", false)

    val modes = choices<Choice>(this, "Choice", { it.choices[0] }) {
        arrayOf(
            NoneChoice(it),
            NoSlowBlockingReuse,
            NoSlowBlockingSwitch,
            NoSlowBlockingBlink,
            NoSlowSharedGrim2860(it),
            NoSlowSharedGrim2860MC18(it)
        )
    }

    /**
     * The hand that is currently blocking on the server
     *
     * Why are we not using [player.isBlocking] instead? Because on certain modules, we do block client-side,
     * but not server-side. This is the case for [ModuleKillAura] for example.
     */
    var blockingHand: Hand? = null
    private var doNotHandle = false

    internal fun untracked(block: () -> Unit) {
        doNotHandle = true
        runCatching {
            block()
        }.onFailure {
            logger.error("An error occurred while executing untracked block in NoSlow", it)
        }
        doNotHandle = false
    }

    @Suppress("unused")
    val packetHandler = handler<PacketEvent>(ignoreCondition = true) {
        when (val packet = it.packet) {
            is PlayerActionC2SPacket -> {
                // Ignores our own module packets
                if (doNotHandle) {
                    return@handler
                }

                if (packet.action == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                    blockingHand = null
                }
            }

            is PlayerInteractItemC2SPacket -> {
                // Ignores our own module packets
                if (doNotHandle) {
                    return@handler
                }

                if (player.getStackInHand(packet.hand).useAction == UseAction.BLOCK) {
                    blockingHand = packet.hand
                }
            }

            is UpdateSelectedSlotC2SPacket -> {
                // Ignores our own module packets
                if (doNotHandle) {
                    return@handler
                }

                blockingHand = null
            }

        }
    }

    override fun handleEvents(): Boolean {
        if (!super.handleEvents() || !inGame) {
            return false
        }

        // Check if we are using a block item
        return player.isUsingItem && player.activeItem.useAction == UseAction.BLOCK
    }

}

