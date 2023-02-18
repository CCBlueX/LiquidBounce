/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

import net.ccbluex.liquidbounce.event.*

object TickStateManager : Listenable {
    var enforcedState = ForcedState()

    private val tickEventHandler = handler<GameTickEvent> {
        val stateEvent = StateUpdateEvent()

        EventManager.callEvent(stateEvent)

        this.enforcedState = stateEvent.state
    }
}

class StateUpdateEvent : Event() {
    val state: ForcedState = ForcedState()
}

class ForcedState {
    var enforceEagle: Boolean? = null
}
