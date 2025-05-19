package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.sneaking

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.sendSneaking

internal class NoSlowSneakingAAC5(override val parent: ChoiceConfigurable<*>) : Choice("AAC5") {
    private val timingMode by enumChoice("Timing", TimingMode.PRE_POST)

    @Suppress("unused", "ComplexCondition")
    private val networkTickHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        if (timingMode == TimingMode.PRE_POST
            || event.state == EventState.PRE && timingMode == TimingMode.PRE_TICK
            || event.state == EventState.POST && timingMode == TimingMode.POST_TICK) {
            network.sendSneaking(true)
        }
    }

    private enum class TimingMode(override val choiceName: String) : NamedChoice {
        PRE_POST("PreAndPost"),
        PRE_TICK("Pre"),
        POST_TICK("Post")
    }
}
