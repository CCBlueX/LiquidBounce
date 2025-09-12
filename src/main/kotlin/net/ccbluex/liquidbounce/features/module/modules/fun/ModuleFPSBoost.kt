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
        private val targetFPS by int("TargetFPS", 120, 10..1000)

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
        private val minFPS by int("MinFPS", 60, 10..1000)
        private val maxFPS by int("MaxFPS", 240, 10..1000)
        private val changeSpeed by float("ChangeSpeed", 0.2f, 0.01f..1f)

        private var currentTarget = minFPS
        private var currentFPS = minFPS.toFloat()

        override fun getModifiedFPS(originalFPS: Int): Int {
            if (System.currentTimeMillis() % 3000 < 50 || currentTarget !in minFPS..maxFPS) {
                currentTarget = Random.Default.nextInt(minFPS, maxFPS + 1)
            }
            currentFPS += (currentTarget - currentFPS) * changeSpeed

            return currentFPS.roundToInt()
        }
    }
}
