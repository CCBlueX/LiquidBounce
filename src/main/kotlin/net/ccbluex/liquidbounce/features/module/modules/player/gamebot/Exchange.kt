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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** The bot's side of the blows traded with one opponent, from [startTick] to [lastTick], starting at [startHp]. */
internal class Exchange(val startTick: Int, private val startHp: Float) {

    var lastTick = startTick

    var sent = 0
    var swings = 0
    var landed = 0
    var blockingAtAttack = 0
    var critAttempts = 0
    var critsShown = 0
    var damage = 0.0

    var takenHits = 0
    var takenHp = 0.0

    /** What the bot held at its last attack, and the damage of its attacks before armor. */
    private var held = "none"
    private var hitDamageSum = 0.0

    private var distanceSum = 0.0
    private var distanceMin = Double.MAX_VALUE
    private var distanceMax = 0.0

    @Suppress("LongParameterList")
    fun attack(tick: Int, distance: Double, blocking: Boolean, crit: Boolean, held: String, hitDamage: Double) {
        lastTick = tick
        sent++
        this.held = held
        hitDamageSum += hitDamage
        distanceSum += distance
        distanceMin = min(distanceMin, distance)
        distanceMax = max(distanceMax, distance)
        if (blocking) {
            blockingAtAttack++
        }
        if (crit) {
            critAttempts++
        }
    }

    fun taken(tick: Int) {
        lastTick = tick
        takenHits++
    }

    /** Clicks per second over the time from the first to the last blow, at least one second; [hp] is the bot's now. */
    fun format(hp: Float): String {
        val seconds = ((lastTick - startTick + 1).toDouble() / TICKS_PER_SECOND).coerceAtLeast(1.0)
        val ratio = if (sent > 0) landed.toDouble() / sent else 0.0
        val distance = if (sent > 0) {
            "${distanceMin.format()}/${(distanceSum / sent).format()}/${distanceMax.format()}"
        } else {
            "none"
        }
        val hitDamage = if (sent > 0) hitDamageSum / sent else 0.0
        return "sent=$sent landed=$landed ratio=${"%.2f".format(Locale.ROOT, ratio)} " +
            "cps=${(sent / seconds).format()} swings=$swings dist=$distance " +
            "blocking_at_attack=$blockingAtAttack crits=$critAttempts/$critsShown dmg=${damage.format()} " +
            "taken=$takenHits/${takenHp.format()} dur=${seconds.format()}s " +
            "held=$held est_hit=${hitDamage.format()} hp_start=${startHp.toDouble().format()} " +
            "hp_end=${hp.toDouble().format()}"
    }

    private fun Double.format() = "%.1f".format(Locale.ROOT, this)

    private companion object {
        const val TICKS_PER_SECOND = 20
    }

}
