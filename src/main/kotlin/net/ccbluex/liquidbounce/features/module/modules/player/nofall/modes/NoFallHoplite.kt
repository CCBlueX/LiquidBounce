/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */
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
