package net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect

import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.randomUsername
import org.ahocorasick.trie.Emit
import org.ahocorasick.trie.Trie
import java.security.MessageDigest
import kotlin.random.Random

/**
 * In most gamemodes players join in the beginning and only leave. We only need to protect their names once in the
 * beginning thus the default behaviour is to only update the aho corasicks trie when a player is *added* to the list.
 */
private const val UPDATE_ON_PLAYER_REMOVAL = false

/**
 * Keeps track of the current name protect mappings and contains functions for replacement.
 */
class NameProtectMappings {
    private var usernameReplacement: Pair<String, String>? = null

    private val friendMappings = HashMap<String, String>()
    private val otherPlayerMappings = HashSet<String>()

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

        if (shouldUpdateOnPlayerRemoval && otherPlayers.size != this.otherPlayerMappings.size) {
            return true
        }

        return false
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

        val currentMapping = HashMap<String, MappingData>()

        otherPlayers.subList(0, 200.coerceAtMost(otherPlayers.size)).forEach { playerName ->
            // Prevent DoS attacks
            if (playerName.length !in 3..20) {
                return@forEach
            }

            val rng = getEntropySourceFrom(playerName)

            currentMapping[playerName] = MappingData(randomUsername(16, rng), coloringInfo.otherPlayers)
        }
        friendMappings.forEach { (name, replacement) ->
            currentMapping[name] = MappingData(replacement, coloringInfo.friends)
        }

        this.friendMappings.clear()
        this.friendMappings.putAll(friendMappings)

        this.otherPlayerMappings.clear()
        this.otherPlayerMappings.addAll(otherPlayers)

        this.usernameReplacement = username

        currentMapping[username.first] = MappingData(username.second, coloringInfo.username)

        val matcher = Trie.builder().addKeywords(currentMapping.keys).ignoreOverlaps().build()

        this.replacementInstructions = ReplacementInstructions(matcher, currentMapping)
    }

    /**
     * Returns a list of all emits, sorted by their start
     */
    fun findReplacements(text: String): List<Pair<Emit, MappingData>> {
        val currentInstructions = this.replacementInstructions ?: return emptyList()

        val result = currentInstructions.matcher.parseText(text)
            .mapTo(ArrayList()) { it to currentInstructions.replacements.get(it.keyword)!! }

        result.sortBy { it.first.start }

        return result
    }

    /**
     * It is important for synchronization purposes that this is a class with immutable fields
     */
    private class ReplacementInstructions(val matcher: Trie, val replacements: HashMap<String, MappingData>)
    class MappingData(val newName: String, val colorGetter: () -> Color4b)
    class ColoringInfo(val username: () -> Color4b, val friends: () -> Color4b, val otherPlayers: () -> Color4b)
}

private fun getEntropySourceFrom(playerName: String): Random {
    val hash = MessageDigest.getInstance("MD5");

    hash.update(playerName.toByteArray())

    val buf = hash.digest()

    val l: Long = ((buf[0].toLong() and 0xFF) shl 56) or
        ((buf.get(1).toLong() and 0xFFL) shl 48) or
        ((buf.get(2).toLong() and 0xFFL) shl 40) or
        ((buf.get(3).toLong() and 0xFFL) shl 32) or
        ((buf.get(4).toLong() and 0xFFL) shl 24) or
        ((buf.get(5).toLong() and 0xFFL) shl 16) or
        ((buf.get(6).toLong() and 0xFFL) shl 8) or
        ((buf.get(7).toLong() and 0xFFL) shl 0)

    return Random(l)
}

