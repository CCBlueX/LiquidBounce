package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand

/**
 * @anticheat Grim
 * @anticheatVersion 2.3.60
 */
class NoSlowSharedGrim2860(override val parent: ChoiceConfigurable<*>) : Choice("Grim2860") {

    @Suppress("unused")
    val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        if (player.isUsingItem && event.state == EventState.PRE) {
            val hand = player.activeHand

            if (hand == Hand.MAIN_HAND) {
                // Send offhand interact packet
                // so that grim focuses on offhand noslow checks that don't exist.
                network.sendPacket(PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0))
            } else if (hand == Hand.OFF_HAND) {
                // Switch slots (based on 1.8 grim switch noslow)
                network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot % 8 + 1))
                network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
            }
        }
    }

}
