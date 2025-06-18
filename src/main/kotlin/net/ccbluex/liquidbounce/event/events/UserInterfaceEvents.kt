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

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerInventoryData
import net.ccbluex.liquidbounce.utils.client.Nameable
import net.minecraft.text.Text

@Nameable("fps")
@WebSocketEvent
@Suppress("unused")
class FpsChangeEvent(val fps: Int) : Event()

@Nameable("clientPlayerData")
@WebSocketEvent
@Suppress("unused")
class ClientPlayerDataEvent(val playerData: PlayerData) : Event() {
    companion object {
        fun fromPlayerStatistics(stats: PlayerData) = ClientPlayerDataEvent(stats)
    }
}

@Nameable("clientPlayerInventory")
@WebSocketEvent
@Suppress("unused")
class ClientPlayerInventoryEvent(val inventory: PlayerInventoryData) : Event() {
    companion object {
        fun fromPlayerInventory(inventory: PlayerInventoryData) = ClientPlayerInventoryEvent(inventory)
    }
}

sealed class TitleEvent : CancellableEvent() {
    sealed class TextContent : TitleEvent() {
        abstract var text: Text?
    }

    @Nameable("title")
    @WebSocketEvent
    class Title(override var text: Text?) : TextContent()

    @Nameable("subtitle")
    @WebSocketEvent
    class Subtitle(override var text: Text?) : TextContent()

    @Nameable("titleFade")
    @WebSocketEvent
    class Fade(var fadeInTicks: Int, var stayTicks: Int, var fadeOutTicks: Int) : TitleEvent()

    @Nameable("clearTitle")
    @WebSocketEvent
    class Clear(var reset: Boolean) : TitleEvent()
}
