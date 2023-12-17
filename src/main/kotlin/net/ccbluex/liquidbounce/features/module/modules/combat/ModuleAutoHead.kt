/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.item.InventoryTracker
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.Hand
import kotlin.math.max

/**
 * AutoHead module
 *
 * Automatically uses heads whenever your health is low.
 * Perfectly suited for Hypixel ThePit
 */

object ModuleAutoHead : Module("AutoHead", Category.COMBAT) {

    private val health by int("Health", 15, 1..40)
    private val healthToIgnoreRegen by int("HealthToIgnoreRegen", 5, 1..10)
    private val combatPauseTime by int("CombatPauseTime", 0, 0..40)
    private val swapDelay by int("SwapDelay", 5, 1..100)

    val repeatable = repeatable {
        val headSlot = findHotbarSlot(Items.PLAYER_HEAD)

        if (interaction.hasRidingInventory()) {
            return@repeatable
        }

        val isInInventoryScreen =
            InventoryTracker.isInventoryOpenServerSide || mc.currentScreen is GenericContainerScreen

        val fullHealth = player.health + player.absorptionAmount
        if (fullHealth < max(health, healthToIgnoreRegen) && headSlot != null && !isInInventoryScreen) {
            if (player.hasStatusEffect(StatusEffects.REGENERATION) || fullHealth >= healthToIgnoreRegen) {
                return@repeatable
            }

            // We need to take some actions
            CombatManager.pauseCombatForAtLeast(combatPauseTime)

            if (player.isBlocking) {
                interaction.stopUsingItem(player)
                waitTicks(1)
            }

            if (headSlot != player.inventory.selectedSlot) {
                SilentHotbar.selectSlotSilently(
                    this, headSlot, swapDelay
                )
            }

            // Use head
            interaction.sendSequencedPacket(world) { sequence ->
                PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence)
            }
            return@repeatable
        }
    }

}
