package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.blockingHand
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.modes
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.untracked
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand

internal object NoSlowBlockingSwitch : Choice("Switch") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    private val timingMode by enumChoice("Timing", TimingMode.PRE_POST)

    @Suppress("unused")
    val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        if (blockingHand == Hand.MAIN_HAND) {
            when (timingMode) {
                TimingMode.PRE_TICK -> {
                    if (event.state == EventState.PRE) {
                        untracked {
                            network.sendPacket(UpdateSelectedSlotC2SPacket(
                                (player.inventory.selectedSlot + 1) % 8)
                            )
                            network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
//                            interaction.sendSequencedPacket(world) { sequence ->
//                                PlayerInteractItemC2SPacket(blockingHand, sequence)
//                            }
                        }
                    }
                }
                TimingMode.POST_TICK -> {
                    if (event.state == EventState.POST) {
                        untracked {
                            network.sendPacket(UpdateSelectedSlotC2SPacket(
                                (player.inventory.selectedSlot + 1) % 8)
                            )
                            network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
//                            interaction.sendSequencedPacket(world) { sequence ->
//                                PlayerInteractItemC2SPacket(blockingHand, sequence)
//                            }
                        }
                    }
                }

                /**
                 * On PreAndPost, we first switch to the off-hand slot, then back to the main-hand slot and
                 * start blocking again.
                 */
                TimingMode.PRE_POST -> {
                    when (event.state) {
                        EventState.PRE -> {
                            untracked {
                                network.sendPacket(UpdateSelectedSlotC2SPacket(
                                    (player.inventory.selectedSlot + 1) % 8)
                                )
                            }
                        }

                        EventState.POST -> {
                            untracked {
                                network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                                interaction.sendSequencedPacket(world) { sequence ->
                                    PlayerInteractItemC2SPacket(blockingHand, sequence)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    enum class TimingMode(override val choiceName: String) : NamedChoice {
        PRE_POST("PreAndPost"),
        PRE_TICK("Pre"),
        POST_TICK("Post")
    }

}
