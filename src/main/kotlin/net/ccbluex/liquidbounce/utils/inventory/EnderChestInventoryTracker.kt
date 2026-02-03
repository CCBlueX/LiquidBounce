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

package net.ccbluex.liquidbounce.utils.inventory

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientPlayerInventoryEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.network.isC2SContainerPacket
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerInventoryData
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack

object EnderChestInventoryTracker : MinecraftShortcuts, EventListener {

    private val DEFAULT = Array(27) { ItemStack.EMPTY }.asList()

    private val flow = MutableStateFlow(DEFAULT)
    @Volatile
    private var isInEnderChestScreen = false

    init {
        ioScope.launch {
            flow.collect {
                val player = mc.player ?: return@collect
                EventManager.callEvent(ClientPlayerInventoryEvent(PlayerInventoryData.fromPlayer(player)))
            }
        }
    }

    val stacks: List<ItemStack> get() = flow.value

    private fun Screen.isEnderChest() = this is ContainerScreen
        && menu.typeOrNull === MenuType.GENERIC_9x3
        && title.string == Component.translatable("container.enderchest").string

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> {
        track()
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        flow.value = DEFAULT
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet.isC2SContainerPacket()) {
            track()
        }
    }

    private fun track() {
        val screen = mc.screen as? ContainerScreen ?: run {
            isInEnderChestScreen = false
            return
        }

        if (screen.isEnderChest()) {
            mc.schedule {
                isInEnderChestScreen = true
                flow.value = screen.menu.slots
                    .filter { it.container !== player.inventory }
                    .map { it.item.copy() }
            }
        } else {
            isInEnderChestScreen = false
        }
    }

}
