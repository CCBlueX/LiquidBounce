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
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.client.isBlocksAttacksExisting
import net.ccbluex.liquidbounce.utils.entity.isInHand
import net.ccbluex.liquidbounce.utils.entity.usingItemOrNull
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.ShieldItem

/**
 * Shows blocking with a shield as 1.8 sword blocking. A sword that cannot block by itself (1.9 to 1.21.3) uses the
 * shield as well, which ViaVersion puts into the offhand for 1.8 servers.
 */
object ModuleSwordBlock : ClientModule("SwordBlock", ModuleCategories.COMBAT, aliases = listOf("OldBlocking")) {

    val onlyVisual by boolean("OnlyVisual", false)
    val fakeOnPressing by boolean("FakeOnPressing", false).doNotIncludeAlways()
    val hideShieldSlot by boolean("HideShieldSlot", false).doNotIncludeAlways()
    val applyToThirdPersonView by boolean("ApplyToThirdPersonView", true).doNotIncludeAlways()
    private val alwaysHideShield by boolean("AlwaysHideShield", false).doNotIncludeAlways()

    private val LivingEntity.shouldApplySwordBlockAnimation: Boolean
        get() {
            if (this === player && fakeOnPressing && mc.options.keyUse.isPressedOnAny) {
                return true
            }

            val usingItem = this.usingItemOrNull ?: return false
            return isInHand(usingItem, InteractionHand.OFF_HAND) && usingItem.item is ShieldItem ||
                isInHand(usingItem, InteractionHand.MAIN_HAND) && !isBlocksAttacksExisting
        }

    /**
     * Whether the sword in the main hand of [entity] is drawn blocking: while AutoBlock shows the block pose, or while
     * SwordBlock draws the shield it blocks with as the sword.
     */
    @JvmStatic
    @JvmOverloads
    fun shouldAnimateSwordBlock(entity: LivingEntity, mainHandItem: ItemStack = entity.mainHandItem): Boolean {
        if (!mainHandItem.isSword) {
            return false
        }

        return KillAuraAutoBlock.showsBlockPose && entity === player || running && entity.shouldApplySwordBlockAnimation
    }

    @JvmOverloads
    fun shouldHideOffhand(
        offHandStack: ItemStack = player.offhandItem,
        mainHandStack: ItemStack = player.mainHandItem
    ): Boolean {
        if (!running) {
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
        if (isBlocksAttacksExisting) {
            return@sequenceHandler
        }

        val packet = event.packet

        if (packet is ServerboundUseItemPacket) {
            val hand = packet.hand
            val itemInHand = player.getItemInHand(hand) // or activeItem

            if (hand == InteractionHand.MAIN_HAND && itemInHand.isSword
                && itemInHand.useAnimation != ItemUseAnimation.BLOCK) {
                val offHandItem = player.offhandItem
                if (offHandItem.item !is ShieldItem) {
                    // Until "now" we should get a shield from the server
                    requires { player.mainHandItem.isSword }
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
