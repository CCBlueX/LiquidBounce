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

package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.player
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemUseAnimation

object InteractionTracker : EventListener {

    val isBlocking: Boolean
        get() = currentInteraction?.action == ItemUseAnimation.BLOCK
    val isMainHand: Boolean
        get() = currentInteraction?.hand == InteractionHand.MAIN_HAND
    val blockingHand: InteractionHand?
        get() = if (isBlocking) currentInteraction?.hand else null

    var currentInteraction: Interaction? = null
        private set
    private var doNotHandle = false

    internal inline fun untracked(block: () -> Unit) {
        doNotHandle = true
        runCatching {
            block()
        }.onFailure {
            logger.error("An error occurred while executing untracked block in NoSlow", it)
        }
        doNotHandle = false
    }

    @Suppress("unused")
    val packetHandler = handler<PacketEvent> {
        if (doNotHandle) {
            return@handler
        }

        when (val packet = it.packet) {
            is ServerboundPlayerActionPacket -> {
                if (packet.action == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
                    currentInteraction = null
                }
            }

            is ServerboundUseItemPacket -> {
                val action = player.getItemInHand(packet.hand).useAnimation

                currentInteraction = when (action) {
                    ItemUseAnimation.NONE -> null
                    else -> Interaction(packet.hand, action)
                }
            }

            is ServerboundSetCarriedItemPacket -> {
                currentInteraction = null
            }

        }
    }

    @JvmRecord
    data class Interaction(val hand: InteractionHand, val action: ItemUseAnimation)

    override val running
        get() = inGame

}
