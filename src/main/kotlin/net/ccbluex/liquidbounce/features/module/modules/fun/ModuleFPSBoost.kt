package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import kotlin.math.roundToInt
import kotlin.random.Random

object ModuleFPSBoost : ClientModule("FPSBoost", Category.FUN, aliases = arrayOf("FPSSpoof")) {

    private val modes = choices("Mode",
        Static,
        arrayOf(Static, Dynamic, SmoothRandom)
    ).apply { tagBy(this) }

    fun getModifiedFPS(originalFPS: Int): Int {
        if (!running) return originalFPS
        return modes.activeChoice.getModifiedFPS(originalFPS)
    }

    abstract class FPSBoostMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<FPSBoostMode>
            get() = modes

        abstract fun getModifiedFPS(originalFPS: Int): Int
    }

    object Static : FPSBoostMode("Static") {
        private val targetFPS by int("TargetFPS", 120, 0..2147483647)

        override fun getModifiedFPS(originalFPS: Int): Int {
            return targetFPS
        }
    }

    object Dynamic : FPSBoostMode("Dynamic") {
        private val multiplier by float("Multiplier", 1.5f, 0.1f..5f)

        override fun getModifiedFPS(originalFPS: Int): Int {
            return (originalFPS * multiplier).roundToInt()
        }
    }

    object SmoothRandom : FPSBoostMode("Random") {
        private val baseRange by intRange("Range", 1024..1337, 0..99999999)
        private val fastChangeSpeed by float("FastSpeed", 0.1f, 0.01f..1f)
        private val slowChangeSpeed by float("SlowSpeed", 0.02f, 0.001f..0.1f)

        private var fastTarget = baseRange.start
        private var slowTarget = baseRange.start
        private var currentFPS = baseRange.start.toFloat()

        override fun getModifiedFPS(originalFPS: Int): Int {
            val time = System.currentTimeMillis()

            if (time % 5000 < 50) {
                slowTarget = Random.nextInt(baseRange.start, baseRange.endInclusive + 1)
            }

            if (time % 300 < 20) {
                fastTarget = slowTarget + Random.nextInt(-2, 3)
            }

            val targetFPS = (slowTarget + (fastTarget - slowTarget) * 0.5f)
            currentFPS += (targetFPS - currentFPS) * fastChangeSpeed
            currentFPS += (slowTarget - currentFPS) * slowChangeSpeed

            return currentFPS.roundToInt()
        }
    }

}
