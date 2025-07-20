package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.player
import net.minecraft.item.consume.UseAction
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand

object InteractionTracker : EventListener {

    val isBlocking: Boolean
        get() = currentInteraction?.action == UseAction.BLOCK
    val isMainHand: Boolean
        get() = currentInteraction?.hand == Hand.MAIN_HAND
    val blockingHand: Hand?
        get() = if (isBlocking) currentInteraction?.hand else null

    var currentInteraction: Interaction? = null
        private set
    private var doNotHandle = false

    internal fun untracked(block: () -> Unit) {
        doNotHandle = true
        runCatching {
            block()
        }.onFailure {
            logger.error("An error occurred while executing untracked block in NoSlow", it)
        }
        doNotHandle = false
    }

    @Suppress("unused")
    val packetHandler = handler<PacketEvent> {
        if (doNotHandle) {
            return@handler
        }

        when (val packet = it.packet) {
            is PlayerActionC2SPacket -> {
                if (packet.action == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                    currentInteraction = null
                }
            }

            is PlayerInteractItemC2SPacket -> {
                val action = player.getStackInHand(packet.hand).useAction

                currentInteraction = when (action) {
                    UseAction.NONE -> null
                    else -> Interaction(packet.hand, action)
                }
            }

            is UpdateSelectedSlotC2SPacket -> {
                currentInteraction = null
            }

        }
    }

    data class Interaction(val hand: Hand, val action: UseAction)

    override val running
        get() = inGame

}
