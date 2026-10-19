/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.network.protocol.game.ServerboundPunchPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.component.SwingAnimation

@Suppress("unused")
@AddonApi
enum class SwingMode(
    override val tag: String,
    val serverSwing: Boolean,
) : Tagged {

    DO_NOT_HIDE("DoNotHide", true),
    HIDE_BOTH("HideForBoth", false),
    HIDE_CLIENT("HideForClient", true),
    HIDE_SERVER("HideForServer", false);

    /**
     * Attack/Break swing, mirroring [net.minecraft.client.Minecraft.startAttack].
     */
    fun swing(hand: InteractionHand) = swing(hand, player.getItemInHand(hand).attackAnimation, true)

    /**
     * Interaction swing, mirroring [net.minecraft.client.Minecraft.startUseItem]: the swing is only
     * predicted locally and the server broadcasts it from its own interaction result. Hence
     * [HIDE_SERVER] cannot hide it, and no [ServerboundPunchPacket] is sent.
     */
    fun interactSwing(hand: InteractionHand) =
        swing(hand, player.getItemInHand(hand).interactAnimation, false)

    private fun swing(hand: InteractionHand, animation: SwingAnimation, punch: Boolean) {
        if (this == HIDE_BOTH) {
            return
        }

        if (this != HIDE_CLIENT) {
            player.swing(hand, animation, false)
        }

        if (punch && serverSwing) {
            network.send(ServerboundPunchPacket.INSTANCE)
        }
    }
}
