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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.client.isNewerThanOrEquals1_21_5
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShieldItem

/**
 * This module allows the user to block with swords. This makes sense to be used on servers with ViaVersion.
 */
object ModuleSwordBlock : ClientModule("SwordBlock", ModuleCategories.COMBAT, aliases = listOf("OldBlocking")) {

    val onlyVisual by boolean("OnlyVisual", false)
    val hideShieldSlot by boolean("HideShieldSlot", false).doNotIncludeAlways()
    val applyToThirdPersonView by boolean("ApplyToThirdPersonView", true).doNotIncludeAlways()
    private val alwaysHideShield by boolean("AlwaysHideShield", false).doNotIncludeAlways()

    @JvmStatic
    val LivingEntity.isBlockingWithOffhandShield
        get() = isUsingItem && offhandItem.item is ShieldItem && useItem === offhandItem

    @JvmOverloads
    fun shouldHideOffhand(
        offHandStack: ItemStack = player.offhandItem,
        mainHandStack: ItemStack = player.mainHandItem
    ): Boolean {
        if (!running && !KillAuraAutoBlock.blockVisual) {
            return false
        }

        if (offHandStack.item !is ShieldItem) {
            return false
        }

        return mainHandStack.isSword || alwaysHideShield
    }

    @Suppress("UNUSED")
    private val packetHandler = sequenceHandler<PacketEvent> { event ->
        if (onlyVisual) {
            return@sequenceHandler
        }

        // If we are already on the old combat protocol or anything blockable protocol,
        // we don't need to do anything
        if (isOlderThanOrEqual1_8 || isNewerThanOrEquals1_21_5) {
            return@sequenceHandler
        }

        val packet = event.packet

        if (packet is ServerboundUseItemPacket) {
            val hand = packet.hand
            val itemInHand = player.getItemInHand(hand) // or activeItem

            if (hand == InteractionHand.MAIN_HAND && itemInHand.isSword) {
                val offHandItem = player.offhandItem
                if (offHandItem.item !is ShieldItem) {
                    // Until "now" we should get a shield from the server
                    waitTicks(1)
                    interaction.startPrediction(world) { sequence ->
                        // This time we use a new sequence
                        ServerboundUseItemPacket(
                            InteractionHand.OFF_HAND, sequence,
                            player.yRot, player.xRot
                        )
                    }
                } else {
                    event.cancelEvent()
                    // We use the old sequence
                    network.send(
                        ServerboundUseItemPacket(
                            InteractionHand.OFF_HAND, packet.sequence,
                            player.yRot, player.xRot
                        )
                    )
                }
            }
        }
    }

}
