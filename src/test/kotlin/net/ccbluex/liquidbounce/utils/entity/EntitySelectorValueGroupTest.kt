package net.ccbluex.liquidbounce.utils.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntitySelectorValueGroupTest {

    @Test
    fun `allow all accepts any player`() {
        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.ALLOW_ALL, "Player", false, emptyList()))
    }

    @Test
    fun `whitelist matches names case insensitively`() {
        val names = listOf("ExamplePlayer")

        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.WHITELIST, "exampleplayer", false, names))
        assertFalse(matchesPlayer(EntitySelectorValueGroup.PlayerMode.WHITELIST, "OtherPlayer", true, names))
    }

    @Test
    fun `blacklist rejects matching names case insensitively`() {
        val names = listOf("ExamplePlayer")

        assertFalse(matchesPlayer(EntitySelectorValueGroup.PlayerMode.BLACKLIST, "EXAMPLEPLAYER", false, names))
        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.BLACKLIST, "OtherPlayer", false, names))
    }

    @Test
    fun `friend modes use relationship status`() {
        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.FRIENDS_ONLY, "Player", true, emptyList()))
        assertFalse(matchesPlayer(EntitySelectorValueGroup.PlayerMode.FRIENDS_ONLY, "Player", false, emptyList()))
        assertTrue(matchesPlayer(EntitySelectorValueGroup.PlayerMode.NON_FRIENDS_ONLY, "Player", false, emptyList()))
        assertFalse(matchesPlayer(EntitySelectorValueGroup.PlayerMode.NON_FRIENDS_ONLY, "Player", true, emptyList()))
    }
}
