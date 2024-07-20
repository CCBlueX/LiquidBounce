package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.modes
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.blockingHand
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.isBlocking
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.untracked
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket

internal object NoSlowBlockingInteract : Choice("Interact") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    @Suppress("unused")
    val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        if (isBlocking) {
            if (event.state == EventState.POST) {
                untracked {
                    network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                    interaction.sendSequencedPacket(world) { sequence ->
                        PlayerInteractItemC2SPacket(blockingHand, sequence, player.yaw, player.pitch)
                    }
                }
            }
        }
    }

}
