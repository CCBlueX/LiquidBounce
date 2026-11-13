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

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import java.util.Locale
import kotlin.math.floor
import kotlin.math.min

/**
 * Picks what to go for next among the chests and opponents a game offers, each scored as value minus way cost:
 * - chest: [CHEST_VALUE] + [CHEST_WEAK_VALUE] times the own [Gear.weakness]
 * - opponent: [OPPONENT_VALUE] + own [Gear.score] - theirs, + [LOW_HP_VALUE] times their missing health,
 *   + [BUSY_VALUE] while another player stands next to them
 * - cost: [DISTANCE_COST] per block, [CLIMB_COST] per block up and [VOID_COST] per block of void on the straight way
 *
 * While unarmed, chests within [UNARMED_CHEST_RANGE] come first. While every opponent outgears the bot by
 * [OUTGEARED_MARGIN], chests come first; with none left it holds when a refill is due within [HOLD_REFILL] seconds.
 * A chosen chest is kept until it is opened or out of reach, and only a player beating it by [SWITCH_MARGIN] takes
 * its place; any other choice gives way to one better by that margin. Games that [next] lets explore get [Seek]
 * while the bot is geared and nobody is in sight, to go and look for players.
 * Logs `plan <chest|player|hold|seek|none> <label> score=.. own=.. their=.. reason=..` when the choice changes.
 */
internal class Plan(private val chests: Chests) : MinecraftShortcuts {

    sealed class Choice(val label: String)

    class Loot(val chest: BlockPos, label: String) : Choice(label)

    class Hunt(val opponent: Player) : Choice("player ${opponent.scoreboardName}")

    /** Stays at [point] until a refill. */
    class Hold(val point: Vec3) : Choice("hold")

    /** Geared with nobody in sight: look for players, however the game can. */
    data object Seek : Choice("seek")

    private class Option(val choice: Choice?, val score: Double, val their: Gear?, val reason: String)

    var choice: Choice? = null
        private set

    private var decidedAt = NEVER
    private var reason: String? = null

    /**
     * The choice among [opponents] and [candidates] (unopened chests), both already filtered for reach by the game.
     * [armed] is a sword or axe in the inventory. With [canExplore], null means looking for more chests or players:
     * when everyone outgears the bot and no chest is left, or when it is geared and nobody is in sight.
     */
    fun next(
        opponents: Sequence<Player>, candidates: Sequence<BlockPos>, armed: Boolean, canExplore: Boolean,
    ): Choice? {
        val players = opponents.toList()
        val chestList = candidates.toList()
        val now = player.tickCount
        val current = choice
        if (now - decidedAt in 0 until DECIDE_TICKS && (current == null || isStill(current, players, chestList))) {
            return current
        }

        decidedAt = now
        val own = Gear.own()
        val next = decide(own, players, chestList, armed, canExplore)
        if (next.choice?.label != current?.label || next.choice == null && next.reason != reason) {
            log(next, own)
        }
        choice = next.choice
        reason = next.reason
        return choice
    }

    fun clear() {
        choice = null
        reason = null
        decidedAt = NEVER
    }

    private fun isStill(choice: Choice, players: List<Player>, chestList: List<BlockPos>) = when (choice) {
        is Loot -> choice.chest in chestList
        is Hunt -> choice.opponent in players
        is Hold, Seek -> true
    }

    private fun decide(
        own: Gear, players: List<Player>, chestList: List<BlockPos>, armed: Boolean, canExplore: Boolean,
    ): Option {
        val position = player.position()
        if (!armed) {
            keep(
                chestList.filter { Vec3.atCenterOf(it).closerThan(position, UNARMED_CHEST_RANGE) }
                    .map { loot(it, own, "unarmed") }
            )?.let { return it }
        }
        if (canExplore && players.isEmpty() && own.weakness <= GEARED_WEAKNESS) {
            return Option(Seek, 0.0, null, "geared nobody_in_sight")
        }

        val hunts = players.map { hunt(it, own) }
        val outgeared = hunts.isNotEmpty() &&
            hunts.all { hunt -> hunt.their?.let { it.score - own.score > OUTGEARED_MARGIN } == true }
        val lootReason = if (outgeared) "outgeared" else "weakness=${format(own.weakness)}"
        val loots = chestList.map { loot(it, own, lootReason) }
        if (!outgeared) {
            return keep(loots + hunts) ?: Option(null, 0.0, null, "nothing")
        }

        val refillIn = chests.refillIn
        return keep(loots) ?: when {
            refillIn != null && refillIn <= HOLD_REFILL ->
                Option((choice as? Hold) ?: Hold(position), 0.0, null, "outgeared refill_in=${refillIn}s")
            canExplore -> Option(null, 0.0, null, "outgeared explore")
            else -> keep(hunts)?.let { Option(it.choice, it.score, it.their, "outgeared no_chests") }
                ?: Option(null, 0.0, null, "nothing")
        }
    }

    /**
     * The best of [options], or the current choice when it is among them and not beaten by [SWITCH_MARGIN]; a chest
     * is never left for another chest.
     */
    private fun keep(options: List<Option>): Option? {
        val best = options.maxByOrNull { it.score } ?: return null
        val current = options.firstOrNull { it.choice?.label == choice?.label } ?: return best
        val committed = current.choice is Loot && best.choice is Loot
        return if (committed || best.score - current.score <= SWITCH_MARGIN) current else best
    }

    private fun loot(chest: BlockPos, own: Gear, reason: String): Option {
        val label = chests.label(chest)
        val value = CHEST_VALUE + CHEST_WEAK_VALUE * own.weakness
        return Option(Loot(chest, label), value - cost(Vec3.atCenterOf(chest)), null, reason)
    }

    private fun hunt(other: Player, own: Gear): Option {
        val their = Gear.of(other)
        val advantage = own.score - their.score
        val missing = 1.0 - their.health
        val busy = world.players().any {
            it !== other && it !== player && !it.isSpectator && it.distanceToSqr(other) <= BUSY_RANGE * BUSY_RANGE
        }
        val value = OPPONENT_VALUE + advantage + LOW_HP_VALUE * missing + if (busy) BUSY_VALUE else 0.0
        val reason = buildString {
            append("advantage=").append(format(advantage))
            if (missing >= LOW_HP_SHOWN) append(" low_hp")
            if (busy) append(" busy")
        }
        return Option(Hunt(other), value - cost(other.position()), their, reason)
    }

    private fun cost(goal: Vec3): Double {
        val from = player.position()
        val climb = (goal.y - from.y).coerceAtLeast(0.0)
        return from.distanceTo(goal) * DISTANCE_COST + climb * CLIMB_COST + voidBlocks(from, goal) * VOID_COST
    }

    /** Columns without a single block on the straight way, one a block, as far as the chunks are loaded. */
    private fun voidBlocks(from: Vec3, to: Vec3): Int {
        val distance = from.horizontalDistanceTo(to)
        val samples = min(floor(distance).toInt(), MAX_VOID_SAMPLES)
        var count = 0
        for (sample in 1..samples) {
            val t = sample / distance
            val x = floor(from.x + (to.x - from.x) * t).toInt()
            val z = floor(from.z + (to.z - from.z) * t).toInt()
            if (world.hasChunkAt(x, z) && world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= world.minY) {
                count++
            }
        }
        return count
    }

    private fun log(option: Option, own: Gear) {
        val line = when (val choice = option.choice) {
            is Loot -> "chest ${choice.chest.x},${choice.chest.y},${choice.chest.z}"
            is Hunt -> "player ${choice.opponent.scoreboardName}"
            is Hold -> "hold %.1f,%.1f,%.1f".format(Locale.ROOT, choice.point.x, choice.point.y, choice.point.z)
            Seek -> "seek -"
            null -> "none -"
        }
        val score = if (option.choice is Loot || option.choice is Hunt) format(option.score) else "-"
        report("plan $line score=$score own=$own their=${option.their ?: "-"} reason=${option.reason}")
        MapRecorder.event("plan", "label" to option.choice?.label, "score" to option.score, "reason" to option.reason)
    }

    private fun format(value: Double) = "%.1f".format(Locale.ROOT, value)

    private companion object {
        const val NEVER = Int.MIN_VALUE / 2
        const val DECIDE_TICKS = 10

        const val CHEST_VALUE = 8.0
        const val CHEST_WEAK_VALUE = 32.0
        const val OPPONENT_VALUE = 20.0
        const val LOW_HP_VALUE = 12.0
        const val BUSY_VALUE = 8.0
        const val BUSY_RANGE = 5.0

        /** Missing health from which an opponent's low health shows in the reason. */
        const val LOW_HP_SHOWN = 0.3

        const val DISTANCE_COST = 0.25
        const val CLIMB_COST = 0.5
        const val VOID_COST = 1.0
        const val MAX_VOID_SAMPLES = 160

        /** How much better another choice has to score to replace the current one. */
        const val SWITCH_MARGIN = 6.0

        /** Own [Gear.weakness] up to which the bot counts as geared, about full iron with an iron sword. */
        const val GEARED_WEAKNESS = 0.15

        const val OUTGEARED_MARGIN = 4.0
        const val UNARMED_CHEST_RANGE = 32.0
        const val HOLD_REFILL = 60
    }

}
