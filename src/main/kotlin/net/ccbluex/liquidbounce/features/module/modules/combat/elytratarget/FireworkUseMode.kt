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

package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.interaction
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.network
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.player
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.world
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket

@Suppress("unused")
internal enum class FireworkUseMode(
    override val tag: String,
    val useFireworkSlot: (HotbarItemSlot, Int) -> Unit
) : Tagged {
    NORMAL("Normal", { slot, resetDelay ->
        useHotbarSlotOrOffhand(slot, resetDelay)
    }),
    PACKET("Packet", { slot, _ ->
        with (player.inventory.selectedSlot) {
            val slotUpdateFlag = slot !is OffHandSlot && slot.hotbarSlotForServer != this

            if (slotUpdateFlag) {
                player.inventory.selectedSlot = slot.hotbarSlotForServer
                network.send(ServerboundSetCarriedItemPacket(slot.hotbarSlotForServer))
            }

            interaction.startPrediction(world) { sequence ->
                ServerboundUseItemPacket(slot.useHand, sequence, player.yRot, player.xRot)
            }

            if (slotUpdateFlag) {
                player.inventory.selectedSlot = this
                network.send(ServerboundSetCarriedItemPacket(this))
            }
        }
    })
}
