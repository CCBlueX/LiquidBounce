/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.item.convertClientSlotToServerSlot
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.SplashPotionItem
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.potion.PotionUtil
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand
import org.apache.commons.lang3.RandomUtils

/**
 * AutoPot module
 *
 * Automatically throws healing potions whenever your health is low.
 */

object ModuleAutoPot : Module("AutoPot", Category.COMBAT) {

    private val delay by int("Delay", 10, 10..20)
    private val health by int("Health", 18, 1..20)
    private val tillGroundDistance by float("TillGroundDistance", 2f, 1f..5f)

    val rotations = tree(RotationsConfigurable())

    val repeatable = repeatable {
        if (player.isDead) {
            return@repeatable
        }

        val potHotBar = findPotion(0, 8)
        val potInvSlot = findPotion(9, 40)

        if (potHotBar == null && potInvSlot == null) {
            return@repeatable
        }

        if (player.health < health) {
            if (potHotBar != null) {
                val collisionBlock = FallingPlayer.fromPlayer(player).findCollision(20)?.pos

                if (player.y - (collisionBlock?.y ?: 0) > tillGroundDistance) {
                    return@repeatable
                }

                if (potHotBar != player.inventory.selectedSlot) {
                    network.sendPacket(UpdateSelectedSlotC2SPacket(potHotBar))
                }

                if (player.pitch <= 80) {
                    RotationManager.aimAt(
                        Rotation(player.yaw, RandomUtils.nextFloat(80f, 90f)),
                        configurable = rotations
                    )
                }

                // Using timer so as to avoid sword shield
                wait(2)
                network.sendPacket(PlayerInteractItemC2SPacket(Hand.MAIN_HAND))

                if (potHotBar != player.inventory.selectedSlot) {
                    network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                }

                wait(delay)

                return@repeatable
            } else {
                val serverSlot = convertClientSlotToServerSlot(potInvSlot!!)

                val openInventory = mc.currentScreen !is InventoryScreen

                if (openInventory) {
                    network.sendPacket(
                        ClientCommandC2SPacket(
                            player,
                            ClientCommandC2SPacket.Mode.OPEN_INVENTORY
                        )
                    )
                }

                interaction.clickSlot(0, serverSlot, 0, SlotActionType.QUICK_MOVE, player)

                if (openInventory) {
                    network.sendPacket(CloseHandledScreenC2SPacket(0))
                }

                return@repeatable
            }
        }
    }

    private fun findPotion(startSlot: Int, endSlot: Int): Int? {
        for (slot in startSlot..endSlot) {
            val stack = player.inventory.getStack(slot)
            if (stack.item is SplashPotionItem) {
                for (effect in PotionUtil.getPotionEffects(stack)) {
                    if (effect.effectType == StatusEffects.INSTANT_HEALTH) {
                        return slot
                    }
                }
            }
        }
        return null
    }
}
