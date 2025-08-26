package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.sneaking

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

internal class NoSlowSneakingAAC5(override val parent: ChoiceConfigurable<*>) : Choice("AAC5") {
    private val timingMode by enumChoice("Timing", TimingMode.PRE_POST)

    @Suppress("unused", "ComplexCondition")
    private val networkTickHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        if (timingMode == TimingMode.PRE_POST
            || event.state == EventState.PRE && timingMode == TimingMode.PRE_TICK
            || event.state == EventState.POST && timingMode == TimingMode.POST_TICK) {
            network.sendPacket(ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
        }
    }

    private enum class TimingMode(override val choiceName: String) : NamedChoice {
        PRE_POST("PreAndPost"),
        PRE_TICK("Pre"),
        POST_TICK("Post")
    }
}
