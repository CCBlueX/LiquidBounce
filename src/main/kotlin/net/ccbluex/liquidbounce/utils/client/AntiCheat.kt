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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.minecraft.resources.Identifier

/**
 * Anticheats [ServerObserver] recognizes by what they send every player after joining: the plugin channels they
 * register, the ids of their transactions (pings) and of their keep-alives. Each entry says how it was sampled.
 *
 * Every piece of evidence has a certainty of its own. Evidence for the same anticheat adds up, the most certain
 * anticheat wins and ties go to the one declared first.
 *
 * [plugins] are the plugin names that prefix their commands, lowercase as Paper registers them. They only mark
 * anticheats in the plugin list, as the command list is no reliable evidence.
 */
@Suppress("SpellCheckingInspection")
enum class AntiCheat(
    override val tag: String,
    private val plugins: Set<String>,
    private vararg val evidence: Evidence,
) : Tagged {

    /**
     * Hypixel's own anticheat, recognized by the address alone. Not sampled.
     */
    WATCHDOG("Watchdog", emptySet(), host("hypixel.net")),

    /**
     * Sampled with a 26.3 client on a Paper 26.3 and a 1.8.8 server with ViaVersion, each running only GrimAC 2.3.75,
     * joined directly and behind Velocity and BungeeCord, there also as a 1.8 client through ViaFabricPlus. The ids
     * are the same everywhere.
     */
    GRIM(
        "Grim",
        setOf("grimac"),
        steadyTransactions(step = -1, first = 0..0, certainty = 75),
        // Other anticheats count down from -1 as well
        steadyTransactions(step = -1, first = -5..-1, certainty = 40),
    ),

    /**
     * Sampled with a 26.3 client on a Paper 26.3 and a 1.8.8 server with ViaVersion, each running only
     * Intave 14.9.7. The ids differ between the two. The last two patterns come from the legacy detector and showed
     * up in no sample.
     */
    INTAVE(
        "Intave",
        setOf("intave"),
        channel("intave"),
        // Random ids from a narrow band far below the short range
        transactions(certainty = 85) { ids ->
            ids.all { it < Short.MIN_VALUE } && ids.differences().toSet().size > 1
        },
        // On 1.8 to 1.16 servers, counting up from -23767
        steadyTransactions(step = 1, first = -23767..-23762, certainty = 85),
        steadyTransactions(step = -1, first = Int.MIN_VALUE..-3001, certainty = 50),
        transactions(certainty = 50) { ids -> ids.first() < -3000 && 0 in ids },
    ),

    /**
     * Sampled like [INTAVE], with only Vulcan 2.9.7.29. The ids are the same on both servers.
     */
    VULCAN(
        "Vulcan",
        setOf("vulcan"),
        // One stray id, then counting up from the bottom of the short range
        transactions(certainty = 90) { ids ->
            ids[1] - ids[0] != 1 && ids[1] in Short.MIN_VALUE..-32700 && ids.drop(1).countsBy(1)
        },
    ),

    /**
     * Sampled like [INTAVE], with only Matrix 7.27.0. The ids are the same on both servers. The two counting
     * patterns come from the legacy detector and showed up in no sample.
     */
    MATRIX(
        "Matrix",
        setOf("matrix"),
        channel("matrix"),
        steadyTransactions(step = 1, first = 95..105, certainty = 60),
        steadyTransactions(step = 1, first = -20005..-19995, certainty = 60),
        // Random negative short ids
        transactions(certainty = 50) { ids ->
            val differences = ids.differences()
            ids.all { it in Short.MIN_VALUE..-1 } && differences.toSet().size == differences.size
        },
    ),

    /**
     * Sampled twice with a 26.3 client on anticheat-test.com, a 1.8 server running several anticheats at once. The
     * second pattern comes from the legacy detector.
     */
    KARHU(
        "Karhu",
        emptySet(),
        // One id from the bottom of the short range, then counting down from -3000
        transactions(certainty = 90) { ids ->
            ids[0] == Short.MIN_VALUE + 1 && ids[1] in -3000..-2995 && ids.drop(1).countsBy(-1)
        },
        steadyTransactions(step = -1, first = -3000..-2995, certainty = 60),
    ),

    /**
     * Sampled like [KARHU]. The first pattern comes from the legacy detector and showed up in no sample.
     */
    VERUS(
        "Verus",
        setOf("verus"),
        transactions(certainty = 60) { ids -> ids[0] == ids[1] && ids.drop(2).countsBy(1) },
        // Counts up from a random id
        steadyTransactions(step = 1, certainty = 60),
    ),

    /**
     * Sampled twice each with a 26.3 client in games on pika.host and gamster.org, 1.8 servers behind Velocity whose
     * lobbies run no anticheat. The counting down fallback comes from the legacy detector.
     */
    POLAR(
        "Polar",
        emptySet(),
        // One stray id, then counting down from a few hundred below zero
        transactions(certainty = 60) { ids ->
            val differences = ids.differences()
            differences[0] >= 100 && differences.drop(1).all { it == -1 }
        },
        steadyTransactions(step = -1, certainty = 30),
        // Its keep-alives reuse every 20th transaction id
        keepAlives(certainty = 90) { ids -> ids.count { it in Short.MIN_VALUE.toLong()..-1L } >= 2 },
    ),

    /**
     * Sampled on a Paper 26.3 server running only NoCheatPlus 3.17.1. It sends nothing a client could tell apart, so
     * only the plugin list shows it.
     */
    NOCHEATPLUS("NoCheatPlus", setOf("nocheatplus")),

    /**
     * Sampled twice each with a 26.3 client and a 1.8 client through ViaFabricPlus on a 1.8.8 server running only
     * AAC 5.0.14, which needs FoxViaRestore for the old ViaVersion API there. It sends nothing a client could tell
     * apart, so only the plugin list shows it.
     */
    AAC("AAC", setOf("aac", "aac5")),

    /**
     * Sampled like [AAC], with only Horizon BETA17. It sends nothing a client could tell apart, so only the plugin
     * list shows it.
     */
    HORIZON("Horizon", setOf("horizon")),

    /**
     * Sampled on a Paper 26.3 server running only Spartan 4.0.9 and on its official test server. It sends nothing a
     * client could tell apart, so only the plugin list shows it.
     */
    SPARTAN("Spartan", setOf("spartan")),

    /**
     * Not sampled. Only the plugin list shows it.
     */
    KAURI("Kauri", setOf("kauri")),

    /**
     * Not sampled. Only the plugin list shows it.
     */
    ANTICHEAT_RELOADED("AntiCheatReloaded", setOf("anticheatreloaded")),

    /**
     * Not sampled, as Themis only runs on 1.17 to 1.21. Only the plugin list shows it.
     */
    THEMIS("Themis", setOf("themis")),

    /**
     * Not sampled. Only the plugin list shows it.
     */
    NEGATIVITY("Negativity", setOf("negativity"));

    private fun certainty(observation: Observation): Int? {
        // Each piece of evidence that holds leaves less room for all of them being wrong
        val doubt = evidence.filter { it.matches(observation) }
            .fold(1.0) { doubt, piece -> doubt * (1 - piece.certainty / 100.0) }
        return if (doubt < 1.0) ((1 - doubt) * 100).toInt() else null
    }

    companion object {

        /**
         * The most certain anticheat, or null when nothing points at one.
         *
         * @param transactions the first transaction ids since joining, null while there are too few
         * @param keepAlives the first keep-alive ids since joining
         */
        fun guess(
            address: String?,
            channels: Set<Identifier>,
            transactions: List<Int>?,
            keepAlives: List<Long>,
        ): AntiCheatGuess? {
            // Test servers that let players pick an anticheat register the channels of all of them
            val namespaces = channels.mapTo(HashSet()) { it.namespace }
            val claiming = entries.count { antiCheat ->
                antiCheat.evidence.any { piece -> piece.namespace?.let(namespaces::contains) == true }
            }
            val trustedChannels = if (claiming > 1) emptySet() else channels
            val observation = Observation(address, trustedChannels, transactions, keepAlives)

            return entries
                .mapNotNull { antiCheat -> antiCheat.certainty(observation)?.let { AntiCheatGuess(antiCheat, it) } }
                .maxByOrNull { it.certainty }
        }

        /**
         * The anticheat behind a plugin name from the command list.
         */
        fun ofPlugin(name: String): AntiCheat? {
            val plugin = name.lowercase()
            return entries.find { plugin in it.plugins }
        }

    }

}

/**
 * @param certainty in percent
 */
data class AntiCheatGuess(val antiCheat: AntiCheat, val certainty: Int)

internal class Evidence(
    val certainty: Int,
    val namespace: String? = null,
    val matches: (Observation) -> Boolean,
) {
    init {
        require(certainty in 1..100) { "certainty is in percent" }
    }
}

internal class Observation(
    val address: String?,
    val channels: Set<Identifier>,
    val transactions: List<Int>?,
    val keepAlives: List<Long>,
)

/**
 * The transaction id behind a ping. ViaVersion carries a transaction of a 1.8 to 1.16 server in a ping with the id
 * `(1 << 30) | (window << 16) | (action & 0xFFFF)`, anticheats always use window 0.
 */
internal fun transactionId(pingId: Int) = if (pingId ushr 16 == 0x4000) pingId.toShort().toInt() else pingId

/**
 * The server address ends with [suffix].
 */
private fun host(suffix: String, certainty: Int = 100) =
    Evidence(certainty) { it.address?.endsWith(suffix, ignoreCase = true) == true }

/**
 * The server registered a plugin channel in [namespace], e.g. `intave` for `intave:movdebug`.
 */
private fun channel(namespace: String, certainty: Int = 95) =
    Evidence(certainty, namespace) { observation -> observation.channels.any { it.namespace == namespace } }

/**
 * The first transaction ids since joining match [pattern]. There are always at least five.
 */
private fun transactions(certainty: Int, pattern: (ids: List<Int>) -> Boolean) =
    Evidence(certainty) { observation -> observation.transactions?.let(pattern) == true }

/**
 * Transaction ids that count by [step], the first one within [first].
 */
private fun steadyTransactions(step: Int, first: IntRange = Int.MIN_VALUE..Int.MAX_VALUE, certainty: Int) =
    transactions(certainty) { ids -> ids.first() in first && ids.countsBy(step) }

/**
 * The first keep-alive ids since joining match [pattern].
 */
private fun keepAlives(certainty: Int, pattern: (ids: List<Long>) -> Boolean) =
    Evidence(certainty) { observation -> pattern(observation.keepAlives) }

private fun List<Int>.differences() = zipWithNext { a, b -> b - a }

private fun List<Int>.countsBy(step: Int) = differences().all { it == step }
