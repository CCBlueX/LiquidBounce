package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.modes
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.entity.isBlockAction
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

internal object NoSlowBlockingBlink : Choice("Blink") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (event.origin != TransferOrigin.OUTGOING || !player.isBlockAction) {
            return@handler
        }

        event.action = if (event.packet is PlayerMoveC2SPacket) {
             PacketQueueManager.Action.QUEUE
        } else if (event.action == PacketQueueManager.Action.FLUSH) {
            PacketQueueManager.Action.PASS
        } else {
            return@handler
        }
    }

}
