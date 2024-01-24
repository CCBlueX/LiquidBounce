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
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall.modes
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * SpoofGround mode for the NoFall module.
 * This mode spoofs the 'onGround' flag in PlayerMoveC2SPacket to prevent fall damage.
 */
internal object NoFallHypixel : Choice("Hypixel") {

    private var managedToReset = false
    var waitUntilGround = true

    /**
     * Specifies the parent configuration for this mode
     */
    override val parent: ChoiceConfigurable
        get() = modes

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is PlayerMoveC2SPacket) {
            if (packet.onGround) {
                managedToReset = false
                waitUntilGround = false
                FakeLag.flush()
            } else {
                if (waitUntilGround) {
                    return@handler
                }

                val fallDistance = player.fallDistance

                // If we are between 2.1 and 20 blocks of fall distance, we want to lag (blink) and
                // set the onGround flag to true, so we don't take any fall damage.
                if (fallDistance > 2.1 && fallDistance < 20) {
                    managedToReset = false
                    packet.onGround = true
                } else {
                    // However, if we are above 20 blocks of fall distance, we want to reset the lag
                    // and rewrite our previous packets to set the onGround flag to false, so they are not spoofed
                    // anymore
                    if (fallDistance >= 20 && !managedToReset) {
                        // Rewrite the packet queue and set all PlayerMoveC2SPacket's onGround flag to false
                        FakeLag.rewriteAndFlush<PlayerMoveC2SPacket> { packet ->
                            packet.onGround = false
                        }

                        managedToReset = true
                    }
                }
            }
        }
    }

    /**
     * Tells the FakeLag feature whether it should lag or not, depending on the fall distance of the player.
     * After 1.7 blocks of fall distance, it makes sense to start lagging (blink),
     * after 20 blocks of fall distance we want to stop lagging (reset) - we will take fall distance.
     * If we land after less than 20 blocks of fall distance, we want to stop lagging (reset) as well
     * and won't take any fall damage, since the packets are spoofed to be on-ground.
     *
     * This logic can be seen above in the [packetHandler] as well.
     */
    fun shouldLag() =
        (isActive && ModuleNoFall.enabled) && player.fallDistance > 1.7 && (player.fallDistance < 21 || !managedToReset)

}
