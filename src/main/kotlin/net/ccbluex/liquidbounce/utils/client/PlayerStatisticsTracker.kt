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
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.events.ClientPlayerStatisticsEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.text.toDeathMessage
import net.minecraft.world.entity.player.Player

@JvmRecord
data class PlayerStatistics(val joinedAt: Long, val kills: Int, val deaths: Int)

/**
 * Counts kills and deaths from death messages, since minigames never really kill a player.
 * Custom messages only count right after a fight.
 */
internal object PlayerStatisticsTracker : EventListener {

    private const val FIGHT_MS = 10_000L

    var statistics: PlayerStatistics? = null
        private set

    private val hitAt = HashMap<String, Long>()
    private var hurtAt = 0L
    private var lastHurtTime = 0

    // Proxies keep the session when they move the player to the next minigame
    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> { event ->
        hitAt.clear()
        hurtAt = 0L

        if (event.world == null) {
            statistics = null
        } else if (statistics == null) {
            publish(PlayerStatistics(System.currentTimeMillis(), kills = 0, deaths = 0))
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent>(priority = READ_FINAL_STATE) { event ->
        val target = event.entity
        if (!event.isCancelled && target is Player) {
            hitAt[target.scoreboardName] = System.currentTimeMillis()
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> { noteHurt() }

    @Suppress("unused")
    private val chatHandler = handler<ChatReceiveEvent> { event ->
        val statistics = statistics ?: return@handler
        val self = mc.player?.scoreboardName ?: return@handler
        // A death message can come in the same tick as the final hit
        noteHurt()
        val now = System.currentTimeMillis()
        hitAt.values.removeIf { now - it > FIGHT_MS }

        val death = event.textData.toDeathMessage(hitAt.keys + self) ?: return@handler
        when {
            death.victim == self && (death.exact || now - hurtAt <= FIGHT_MS) -> {
                hurtAt = 0L
                publish(statistics.copy(deaths = statistics.deaths + 1))
            }

            death.killer == self && (hitAt.remove(death.victim) != null || death.exact) ->
                publish(statistics.copy(kills = statistics.kills + 1))
        }
    }

    // The hurt animation restarts with every hit taken
    private fun noteHurt() {
        val hurtTime = mc.player?.hurtTime ?: 0
        if (hurtTime > lastHurtTime) {
            hurtAt = System.currentTimeMillis()
        }
        lastHurtTime = hurtTime
    }

    private fun publish(statistics: PlayerStatistics) {
        this.statistics = statistics
        EventManager.callEvent(ClientPlayerStatisticsEvent(statistics))
    }

}
