package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.modes
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.isBlocking
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

internal object NoSlowBlockingBlink : Choice("Blink") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    fun shouldLag(packet: Packet<*>?): FakeLag.LagResult? {
        if (!isActive || !handleEvents() || !player.isBlocking) {
            return null
        }

        return if (packet is PlayerMoveC2SPacket) {
            FakeLag.LagResult.QUEUE
        } else {
            FakeLag.LagResult.PASS
        }
    }

}
