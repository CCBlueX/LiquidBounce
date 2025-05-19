package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.sneaking

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.sendSneaking

internal class NoSlowSneakingSwitch(override val parent: ChoiceConfigurable<*>) : Choice("Switch") {
    private val timingMode by enumChoice("Timing", TimingMode.PRE_POST)

    @Suppress("unused")
    private val networkTickHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        when (timingMode) {
            TimingMode.PRE_POST -> when (event.state) {
                EventState.PRE -> {
                    network.sendSneaking(false)
                }
                EventState.POST -> {
                    network.sendSneaking(true)
                }
            }
            TimingMode.PRE_TICK -> if (event.state == EventState.PRE) {
                network.sendSneaking(false)
                network.sendSneaking(true)
            }
            TimingMode.POST_TICK -> if (event.state == EventState.POST) {
                network.sendSneaking(false)
                network.sendSneaking(true)
            }
        }
    }

    private enum class TimingMode(override val choiceName: String) : NamedChoice {
        PRE_POST("PreAndPost"),
        PRE_TICK("Pre"),
        POST_TICK("Post")
    }
}
