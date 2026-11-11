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
 * register, the ids of their transactions (pings) and of their keep-alives.
 *
 * Every piece of evidence has a certainty of its own. Evidence for the same anticheat adds up, the most certain
 * anticheat wins and ties go to the one declared first. Entries without evidence only show in the plugin list.
 *
 * `@testedOn` lists the servers an entry was sampled on, `@testedWith` the clients that joined them, `@untested`
 * marks entries never sampled.
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
     * @untested
     */
    WATCHDOG("Watchdog", emptySet(), host("hypixel.net")),

    /**
     * @testedOn Paper 26.3, only GrimAC 2.3.75
     * @testedOn 1.8.8 with ViaVersion 5.12.2, only GrimAC 2.3.75
     * @testedOn Velocity and BungeeCord, lobby without anticheat, then Paper 26.3 with GrimAC 2.3.75
     * @testedWith 26.3
     * @testedWith 1.8 through ViaFabricPlus
     */
    GRIM(
        "Grim",
        setOf("grimac"),
        steadyTransactions(step = -1, first = 0..0, certainty = 75),
        // Other anticheats count down from -1 as well
        steadyTransactions(step = -1, first = -5..-1, certainty = 40),
    ),

    /**
     * @testedOn Paper 26.3, only Intave 14.9.7
     * @testedOn 1.8.8 with ViaVersion 5.12.2, only Intave 14.9.7
     * @testedWith 26.3
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
        // Legacy detector, in no sample
        steadyTransactions(step = -1, first = Int.MIN_VALUE..-3001, certainty = 50),
        // Legacy detector, in no sample
        transactions(certainty = 50) { ids -> ids.first() < -3000 && 0 in ids },
    ),

    /**
     * @testedOn Paper 26.3, only Vulcan 2.9.7.29
     * @testedOn 1.8.8 with ViaVersion 5.12.2, only Vulcan 2.9.7.29
     * @testedWith 26.3
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
     * @testedOn Paper 26.3, only Matrix 7.27.0
     * @testedOn 1.8.8 with ViaVersion 5.12.2, only Matrix 7.27.0
     * @testedWith 26.3
     */
    MATRIX(
        "Matrix",
        setOf("matrix"),
        channel("matrix"),
        // Legacy detector, in no sample
        steadyTransactions(step = 1, first = 95..105, certainty = 60),
        // Legacy detector, in no sample
        steadyTransactions(step = 1, first = -20005..-19995, certainty = 60),
        // Random negative short ids
        transactions(certainty = 50) { ids ->
            val differences = ids.differences()
            ids.all { it in Short.MIN_VALUE..-1 } && differences.toSet().size == differences.size
        },
    ),

    /**
     * @testedOn anticheat-test.com, 1.8 with several anticheats
     * @testedWith 26.3
     */
    KARHU(
        "Karhu",
        emptySet(),
        // One id from the bottom of the short range, then counting down from -3000
        transactions(certainty = 90) { ids ->
            ids[0] == Short.MIN_VALUE + 1 && ids[1] in -3000..-2995 && ids.drop(1).countsBy(-1)
        },
        // Legacy detector, in no sample
        steadyTransactions(step = -1, first = -3000..-2995, certainty = 60),
    ),

    /**
     * @testedOn anticheat-test.com, 1.8 with several anticheats
     * @testedWith 26.3
     */
    VERUS(
        "Verus",
        setOf("verus"),
        // Legacy detector, in no sample
        transactions(certainty = 60) { ids -> ids[0] == ids[1] && ids.drop(2).countsBy(1) },
        // Counts up from a random id
        steadyTransactions(step = 1, certainty = 60),
    ),

    /**
     * @testedOn pika.host, 1.8 behind Velocity
     * @testedOn gamster.org, 1.8 behind Velocity
     * @testedWith 26.3
     */
    POLAR(
        "Polar",
        emptySet(),
        // One stray id, then counting down from a few hundred below zero
        transactions(certainty = 60) { ids ->
            val differences = ids.differences()
            differences[0] >= 100 && differences.drop(1).all { it == -1 }
        },
        // Legacy detector, in no sample
        steadyTransactions(step = -1, certainty = 30),
        // Its keep-alives reuse every 20th transaction id
        keepAlives(certainty = 90) { ids -> ids.count { it in Short.MIN_VALUE.toLong()..-1L } >= 2 },
    ),

    /**
     * @testedOn Paper 26.3, only NoCheatPlus 3.17.1
     * @testedWith 26.3
     */
    NOCHEATPLUS("NoCheatPlus", setOf("nocheatplus")),

    /**
     * @testedOn 1.8.8 with ViaVersion 5.12.2 and FoxViaRestore, only AAC 5.0.14
     * @testedWith 26.3
     * @testedWith 1.8 through ViaFabricPlus
     */
    AAC("AAC", setOf("aac", "aac5")),

    /**
     * @testedOn 1.8.8 with ViaVersion 5.12.2, only Horizon BETA17
     * @testedWith 26.3
     * @testedWith 1.8 through ViaFabricPlus
     */
    HORIZON("Horizon", setOf("horizon")),

    /**
     * @testedOn Paper 26.3, only Spartan 4.0.9
     * @testedOn minecraft.vagdedes.com, its official test server
     * @testedWith 26.3
     */
    SPARTAN("Spartan", setOf("spartan")),

    /**
     * @untested
     */
    KAURI("Kauri", setOf("kauri")),

    /**
     * @untested
     */
    ANTICHEAT_RELOADED("AntiCheatReloaded", setOf("anticheatreloaded")),

    /**
     * @untested runs only on 1.17 to 1.21
     */
    THEMIS("Themis", setOf("themis")),

    /**
     * @untested
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
