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

package net.ccbluex.liquidbounce.utils.network

import net.ccbluex.liquidbounce.utils.client.isNewerThanOrEquals1_21_9
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket

fun Packet<*>?.isC2SContainerPacket() =
    this is ServerboundContainerClickPacket ||
        this is ServerboundContainerButtonClickPacket ||
        this is ServerboundSetCreativeModeSlotPacket ||
        this is ServerboundContainerSlotStateChangedPacket ||
        this is ServerboundContainerClosePacket

fun Packet<*>?.isLocalPlayerDamage(): Boolean {
    return this is ClientboundDamageEventPacket && this.entityId == mc.player?.id
}

@JvmOverloads
fun Packet<*>?.isLocalPlayerVelocity(considerExplosion: Boolean = true): Boolean {
    return when (this) {
        is ClientboundSetEntityMotionPacket -> this.id == mc.player?.id
        is ClientboundExplodePacket -> this.playerKnockback.isPresent && considerExplosion
        else -> false
    }
}

fun ClientboundSetEntityMotionPacket.isMovementYFallDamage(): Boolean {
    // >= 1.21.9 -0.0783739241897089
    // < 1.21.9 -0.07825184642617344
    return this.movement.y.toRawBits() ==
        (if (isNewerThanOrEquals1_21_9) -4633060179779189496L else -4633068976409115392L)
}
