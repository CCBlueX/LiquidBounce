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

package net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.ccbluex.fastutil.LfuCache
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.randomUsername
import net.ccbluex.liquidbounce.utils.kotlin.unmodifiable
import org.ahocorasick.trie.Emit
import org.ahocorasick.trie.Trie
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.random.Random

/**
 * In most gamemodes players join in the beginning and only leave. We only need to protect their names once in the
 * beginning thus the default behaviour is to only update the aho corasicks trie when a player is *added* to the list.
 */
private const val UPDATE_ON_PLAYER_REMOVAL = false

/**
 * How many distinct texts are memoized per [NameProtectMappings.ReplacementInstructions].
 */
private const val REPLACEMENT_CACHE_SIZE = 512

/**
 * Matches found by [NameProtectMappings.findReplacements], sorted by start index.
 */
typealias Replacements = List<Pair<Emit, NameProtectMappings.MappingData>>

fun interface ColorGetter {
    operator fun invoke(): Color4b
}

/**
 * Keeps track of the current name protect mappings and contains functions for replacement.
 */
class NameProtectMappings {
    private var usernameReplacement: Pair<String, String>? = null

    private var friendMappings = emptyMap<String, String>()
    private var otherPlayerMappings = emptySet<String>()

    /**
     * Replaced as a whole on every rebuild, so readers either see the previous or the next
     * fully built matcher.
     */
    @Volatile
    private var replacementInstructions: ReplacementInstructions? = null

    private fun shouldUpdate(
        usernameReplacement: Pair<String, String>,
        friendMappings: List<Pair<String, String>>,
        otherPlayers: List<String>
    ): Boolean {
        val userChanged = usernameReplacement != this.usernameReplacement
        val friendChanged = friendMappings.any { (name, replacement) -> this.friendMappings[name] != replacement }
        val otherPlayersChanged = otherPlayers.any { name -> !this.otherPlayerMappings.contains(name) }

        if (userChanged || friendChanged || otherPlayersChanged) {
            return true
        }

        if (friendMappings.size != this.friendMappings.size) {
            return true
        }

        // Make sure we update when the user disables the player name replacement.
        val shouldUpdateOnPlayerRemoval = UPDATE_ON_PLAYER_REMOVAL || otherPlayers.isEmpty()

        return shouldUpdateOnPlayerRemoval && otherPlayers.size != this.otherPlayerMappings.size
    }

    fun update(
        username: Pair<String, String>,
        friendMappings: List<Pair<String, String>>,
        otherPlayers: List<String>,
        coloringInfo: ColoringInfo
    ) {
        if (!shouldUpdate(username, friendMappings, otherPlayers)) {
            return
        }

        val currentMapping = HashMap<String, MappingData>(otherPlayers.size + friendMappings.size)

        otherPlayers.subList(0, 200.coerceAtMost(otherPlayers.size)).forEach { playerName ->
            // Prevent DoS attacks
            if (playerName.length !in 2..20) {
                return@forEach
            }

            val rng = getEntropySourceFrom(playerName)

            currentMapping[playerName] = MappingData(randomUsername(16, rng), coloringInfo.otherPlayers)
        }

        friendMappings.forEach { (name, replacement) ->
            currentMapping[name] = MappingData(replacement, coloringInfo.friends)
        }

        this.friendMappings = friendMappings.toMap()

        this.otherPlayerMappings = otherPlayers.toSet()

        this.usernameReplacement = username

        currentMapping[username.first] = MappingData(username.second, coloringInfo.username)

        val matcher = Trie.builder().addKeywords(currentMapping.keys).ignoreOverlaps().build()

        this.replacementInstructions = ReplacementInstructions(matcher, currentMapping)
    }

    /**
     * Returns a list of all emits, sorted by their start
     */
    fun findReplacements(text: CharSequence): Replacements =
        this.replacementInstructions?.match(text).orEmpty()

    /**
     * Memoized variant of [findReplacements], which must only be called from the thread that calls
     * [update] because [ReplacementInstructions.matchCached] is not thread-safe.
     */
    fun findReplacementsCached(text: CharSequence): Replacements =
        this.replacementInstructions?.matchCached(text).orEmpty()

    /**
     * A built matcher together with the mappings it resolves to.
     *
     * Immutable, so it can be swapped in for readers as a whole. The memoized matches are tied to
     * this instance, which makes them expire exactly when the mappings are rebuilt.
     */
    private class ReplacementInstructions(val matcher: Trie, val replacements: Map<String, MappingData>) {
        private val cache = LfuCache<CharSequence, Replacements>(REPLACEMENT_CACHE_SIZE)

        fun match(text: CharSequence): Replacements =
            matcher.parseText(text)
                .mapToArray { it to replacements[it.keyword]!! }
                .apply { sortBy { it.first.start } }
                .unmodifiable()

        fun matchCached(text: CharSequence): Replacements = cache.getOrPut(text) { match(text) }
    }

    class MappingData(val newName: String, val colorGetter: ColorGetter)
    class ColoringInfo(val username: ColorGetter, val friends: ColorGetter, val otherPlayers: ColorGetter)
}

private fun getEntropySourceFrom(playerName: String): Random {
    val hash = MessageDigest.getInstance("MD5").digest(playerName.toByteArray())
    // Parse the first 8 bytes to long value
    val l = ByteBuffer.wrap(hash).long
    return Random(l)
}

