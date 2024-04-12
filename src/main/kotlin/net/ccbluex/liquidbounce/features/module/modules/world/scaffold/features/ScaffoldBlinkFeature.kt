package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Chronometer

object ScaffoldBlinkFeature : ToggleableConfigurable(ModuleScaffold, "Blink", false) {

    private val time by intRange("Time", 50..250, 0..3000, "ms")
    private val fallCancel by boolean("FallCancel", true)

    private var pulseTime = 0L
    private val pulseTimer = Chronometer()

    val shouldBlink
        get() = handleEvents() && (!player.isOnGround || !pulseTimer.hasElapsed(pulseTime))

    fun onBlockPlacement() {
        pulseTime = time.random().toLong()
    }

    val repeatable = repeatable {
        if (fallCancel && player.fallDistance > 0.5f) {
            FakeLag.cancel()
            onBlockPlacement()
        }

        if (pulseTimer.hasElapsed(pulseTime)) {
            pulseTimer.reset()
        }
    }


}
