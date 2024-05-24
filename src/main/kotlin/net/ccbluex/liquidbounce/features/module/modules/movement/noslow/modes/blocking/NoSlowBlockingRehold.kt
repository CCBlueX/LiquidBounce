package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.Block.blockingHand
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.Block.modes
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.Block.nextIsIgnored
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand

object NoSlowBlockingRehold : Choice("Rehold") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    @Suppress("unused")
    val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        if (blockingHand == Hand.MAIN_HAND) {
            when (event.state) {
                EventState.PRE -> {
                    nextIsIgnored = true
                    network.sendPacket(UpdateSelectedSlotC2SPacket((0..8).random()))
                }

                EventState.POST -> {
                    nextIsIgnored = true
                    network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))

                    nextIsIgnored = true
                    interaction.sendSequencedPacket(world) { sequence ->
                        PlayerInteractItemC2SPacket(blockingHand, sequence)
                    }
                }
            }
        }
    }

}
