package net.ccbluex.liquidbounce.utils.entity

import org.junit.jupiter.api.Test
import java.util.TreeSet
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntitySelectorValueGroupTest {

    @Test
    fun `allow all accepts any player`() {
        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.ALLOW_ALL, "Player", false, emptySet()))
    }

    @Test
    fun `whitelist matches names case insensitively`() {
        val names = TreeSet(String.CASE_INSENSITIVE_ORDER).apply { add("ExamplePlayer") }

        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.WHITELIST, "exampleplayer", false, names))
        assertFalse(matchesPlayer(EntitySelectorValueGroup.PlayerMode.WHITELIST, "OtherPlayer", true, names))
    }

    @Test
    fun `blacklist rejects matching names case insensitively`() {
        val names = TreeSet(String.CASE_INSENSITIVE_ORDER).apply { add("ExamplePlayer") }

        assertFalse(matchesPlayer(EntitySelectorValueGroup.PlayerMode.BLACKLIST, "EXAMPLEPLAYER", false, names))
        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.BLACKLIST, "OtherPlayer", false, names))
    }

    @Test
    fun `friend modes use relationship status`() {
        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.FRIENDS_ONLY, "Player", true, emptySet()))
        assertFalse(matchesPlayer(EntitySelectorValueGroup.PlayerMode.FRIENDS_ONLY, "Player", false, emptySet()))
        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.NON_FRIENDS_ONLY, "Player", false, emptySet()))
        assertFalse(matchesPlayer(EntitySelectorValueGroup.PlayerMode.NON_FRIENDS_ONLY, "Player", true, emptySet()))
    }
}
