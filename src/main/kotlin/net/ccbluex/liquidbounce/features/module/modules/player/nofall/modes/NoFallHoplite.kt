package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

internal object NoFallHoplite : Choice("Hoplite") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    /*
    * Seems to be taken off Grim's GitHub. Issue #1275
    *
    * Code taken from LiquidBounce's Issue List and reworked it a bit.
    * Made it only send the necessary packet.
    * Naming Reason: Hoplite is one of the few servers that use Latest grim and that this bypasess on.
    * MccIsland's Grim Fork has managed to patch this bypass.
    *
    */
    val tickHandler = handler<PlayerTickEvent> {
        if (!player.isOnGround && player.fallDistance > 2f) {
            // Goes up a tiny bit to stop fall damage on 1.17+ servers.
            // Abuses Grim 1.17 extra packets to not flag timer.
            network.sendPacket(PlayerMoveC2SPacket.Full(player.x, player.y + 1.0E-9, player.z,
                player.yaw, player.pitch, player.isOnGround))

            player.onLanding()
        }

    }
}
