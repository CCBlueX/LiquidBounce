package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket

/**
 * @anticheat Grim
 * @anticheatVersion 2.3.60
 */
class NoSlowSharedGrim2860MC18(override val parent: ChoiceConfigurable<*>) : Choice("Grim2860-1.8") {

    @Suppress("unused")
    val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        if (player.isUsingItem && event.state == EventState.PRE) {
            // Switch slots so grim exempts noslow...
            // Introduced with https://github.com/GrimAnticheat/Grim/issues/874
            network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot % 8 + 1))
            network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
        }
    }

}
