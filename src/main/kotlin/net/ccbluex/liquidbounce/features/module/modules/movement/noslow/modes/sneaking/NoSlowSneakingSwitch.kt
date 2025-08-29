package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.sneaking

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.entity.copy
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket

internal class NoSlowSneakingSwitch(override val parent: ChoiceConfigurable<*>) : Choice("Switch") {
    private val timingMode by enumChoice("Timing", TimingMode.PRE_POST)

    @Suppress("unused")
    private val networkTickHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        when (timingMode) {
            TimingMode.PRE_POST -> when (event.state) {
                EventState.PRE -> {
                    network.sendPacket(PlayerInputC2SPacket(player.input.playerInput.copy(sneak = false)))
                }
                EventState.POST -> {
                    network.sendPacket(PlayerInputC2SPacket(player.input.playerInput.copy(sneak = true)))
                }
            }
            TimingMode.PRE_TICK -> if (event.state == EventState.PRE) {
                network.sendPacket(PlayerInputC2SPacket(player.input.playerInput.copy(sneak = false)))
                network.sendPacket(PlayerInputC2SPacket(player.input.playerInput.copy(sneak = true)))
            }
            TimingMode.POST_TICK -> if (event.state == EventState.POST) {
                network.sendPacket(PlayerInputC2SPacket(player.input.playerInput.copy(sneak = false)))
                network.sendPacket(PlayerInputC2SPacket(player.input.playerInput.copy(sneak = true)))
            }
        }
    }

    private enum class TimingMode(override val choiceName: String) : NamedChoice {
        PRE_POST("PreAndPost"),
        PRE_TICK("Pre"),
        POST_TICK("Post")
    }
}
