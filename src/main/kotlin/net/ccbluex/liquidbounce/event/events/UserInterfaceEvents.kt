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

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.integration.interop.protocol.event.WebSocketEvent
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerData
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.PlayerInventoryData
import net.minecraft.network.chat.Component
import net.minecraft.world.effect.MobEffectInstance

@Tag("fps")
@Suppress("unused")
class FpsChangeEvent(val fps: Int) : Event(), WebSocketEvent

@Tag("fpsLimit")
@Suppress("unused")
class FpsLimitEvent(var fps: Int) : Event()

@Tag("clientPlayerData")
@Suppress("unused")
class ClientPlayerDataEvent(val playerData: PlayerData) : Event(), WebSocketEvent

@Tag("clientPlayerEffect")
@Suppress("unused")
class ClientPlayerEffectEvent(val effects: List<MobEffectInstance>) : Event(), WebSocketEvent

@Tag("clientPlayerInventory")
@Suppress("unused")
class ClientPlayerInventoryEvent(val inventory: PlayerInventoryData) : Event(), WebSocketEvent

sealed class TitleEvent : CancellableEvent(), WebSocketEvent {
    sealed class TextContent : TitleEvent() {
        abstract var text: Component?
    }

    @Tag("title")
    class Title(override var text: Component?) : TextContent()

    @Tag("subtitle")
    class Subtitle(override var text: Component?) : TextContent()

    @Tag("titleFade")
    class Fade(var fadeInTicks: Int, var stayTicks: Int, var fadeOutTicks: Int) : TitleEvent()

    @Tag("clearTitle")
    class Clear(var reset: Boolean) : TitleEvent()
}
