package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.modes
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.blockingHand
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.isBlocking
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.untracked
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

internal object NoSlowBlockingReuse : Choice("Reuse") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    private val timingMode by enumChoice("Timing", TimingMode.PRE_POST)

    @Suppress("unused")
    private val networkTickHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        if (isBlocking) {
            when (timingMode) {
                TimingMode.PRE_TICK -> {
                    if (event.state == EventState.PRE) {
                        untracked {
                            network.sendPacket(PlayerActionC2SPacket(
                                PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN
                            ))
                            interaction.sendSequencedPacket(world) { sequence ->
                                PlayerInteractItemC2SPacket(blockingHand, sequence, player.yaw, player.pitch)
                            }
                        }
                    }
                }

                TimingMode.POST_TICK -> {
                    if (event.state == EventState.POST) {
                        untracked {
                            network.sendPacket(PlayerActionC2SPacket(
                                PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN
                            ))
                            interaction.sendSequencedPacket(world) { sequence ->
                                PlayerInteractItemC2SPacket(blockingHand, sequence, player.yaw, player.pitch)
                            }
                        }
                    }
                }

                TimingMode.PRE_POST -> {
                    when (event.state) {
                        EventState.PRE -> {
                            untracked {
                                network.sendPacket(PlayerActionC2SPacket(
                                    PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN
                                ))
                            }
                        }

                        EventState.POST -> {
                            untracked {
                                interaction.sendSequencedPacket(world) { sequence ->
                                    PlayerInteractItemC2SPacket(blockingHand, sequence, player.yaw, player.pitch)
                                }
                            }
                        }
                    }
                }
            }
        }


        if (isBlocking) {
            when (event.state) {
                EventState.PRE -> {

                }

                EventState.POST -> {
                    untracked {
                        interaction.sendSequencedPacket(world) { sequence ->
                            PlayerInteractItemC2SPacket(blockingHand, sequence, player.yaw, player.pitch)
                        }
                    }
                }
            }
        }
    }

    private enum class TimingMode(override val choiceName: String) : NamedChoice {
        PRE_POST("PreAndPost"),
        PRE_TICK("Pre"),
        POST_TICK("Post")
    }


}
