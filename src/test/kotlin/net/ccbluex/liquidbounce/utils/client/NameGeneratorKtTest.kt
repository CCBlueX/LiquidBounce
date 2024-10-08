package net.ccbluex.liquidbounce.utils.client

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class NameGeneratorKtTest {
    @Test
    fun testUsernameLength() {
        val rng = Random(1337)

        val alreadySeenUsernames = HashSet<String>()

        for (i in 0..1000) {
            val randomUsername = randomUsername(16, rng)

            assert(randomUsername.length in 3..16) { "'$randomUsername' does not fit size requirements. [$i]" }
            assert(alreadySeenUsernames.add(randomUsername)) { "'$randomUsername' was generated twice [$i]" }
        }
        println(alreadySeenUsernames)
    }
}
