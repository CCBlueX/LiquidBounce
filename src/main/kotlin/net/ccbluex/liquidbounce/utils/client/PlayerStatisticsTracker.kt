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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientPlayerStatisticsEvent
import net.ccbluex.liquidbounce.event.events.PlayerDeathEvent
import net.ccbluex.liquidbounce.event.events.PlayerKillEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.session.sessionState

@JvmRecord
data class PlayerStatistics(val joinedAt: Long, val kills: Int, val deaths: Int)

internal object PlayerStatisticsTracker : EventListener {

    var statistics: PlayerStatistics? by sessionState { null }
        private set

    // Proxies keep the session when they move the player to the next minigame
    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> { event ->
        if (event.world == null) {
            statistics = null
        } else if (statistics == null) {
            publish(PlayerStatistics(System.currentTimeMillis(), kills = 0, deaths = 0))
        }
    }

    @Suppress("unused")
    private val killHandler = handler<PlayerKillEvent> {
        statistics?.let { publish(it.copy(kills = it.kills + 1)) }
    }

    @Suppress("unused")
    private val deathHandler = handler<PlayerDeathEvent> {
        statistics?.let { publish(it.copy(deaths = it.deaths + 1)) }
    }

    private fun publish(statistics: PlayerStatistics) {
        this.statistics = statistics
        EventManager.callEvent(ClientPlayerStatisticsEvent(statistics))
    }

}
