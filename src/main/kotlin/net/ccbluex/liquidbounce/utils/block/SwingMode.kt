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
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.world.InteractionHand
import java.util.function.Consumer

@Suppress("unused")
enum class SwingMode(
    override val tag: String,
    val serverSwing: Boolean,
) : Tagged, Consumer<InteractionHand> {

    DO_NOT_HIDE("DoNotHide", true),
    HIDE_BOTH("HideForBoth", false),
    HIDE_CLIENT("HideForClient", true),
    HIDE_SERVER("HideForServer", false);

    fun swing(hand: InteractionHand) = accept(hand)

    override fun accept(hand: InteractionHand) {
        when (this) {
            DO_NOT_HIDE -> player.swing(hand)
            HIDE_BOTH -> {}
            HIDE_CLIENT -> network.send(ServerboundSwingPacket(hand))
            HIDE_SERVER -> player.swing(hand, false)
        }
    }
}
