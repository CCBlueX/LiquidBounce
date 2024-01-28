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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.DummyEvent
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.pressedOnKeyboard
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType

/**
 * AutoGapple module
 *
 * Automatically eats apples whenever your health is low.
 */

object ModuleAutoGapple : Module("AutoGapple", Category.COMBAT) {

    private val health by int("Health", 15, 1..20, "HP")
    private val inventoryConstraints = tree(InventoryConstraintsConfigurable())

    private val notDuringRegeneration by boolean("NotDuringRegeneration", true)
    private val notDuringCombat by boolean("NotDuringCombat", true)

    val repeatable = repeatable {
        val hotbarSlot = findHotbarSlot(Items.GOLDEN_APPLE)
        val invSlot = findInventorySlot(Items.GOLDEN_APPLE)

        if (interaction.hasRidingInventory() && invSlot != null && hotbarSlot == null) {
            return@repeatable
        }

        if (player.health + player.absorptionAmount < health) {
            if (hotbarSlot == null) {
                if (findEmptyHotbarSlot() && invSlot != null) {
                    performInventoryClick(invSlot)
                }

                return@repeatable
            }

            hotbarSlot.run {
                if (notDuringRegeneration && player.hasStatusEffect(StatusEffects.REGENERATION)) {
                    return@repeatable
                }

                if (notDuringCombat && CombatManager.isInCombat()) {
                    return@repeatable
                }

                val delay = inventoryConstraints.clickDelay.random()

                if (!waitConditional(delay) { !canUseItem() }) {
                    return@repeatable
                }

                SilentHotbar.selectSlotSilently(this@ModuleAutoGapple, this, 1)

                if (player.isUsingItem) {
                    interaction.stopUsingItem(player)

                    if (!waitConditional(1) { !canUseItem() }) {
                        SilentHotbar.resetSlot(this)

                        return@repeatable
                    }

                    if (SilentHotbar.serversideSlot != this) {
                        SilentHotbar.selectSlotSilently(this@ModuleAutoGapple, this, delay.coerceAtLeast(1))
                    }
                }

                var stopItemUse = false

                waitUntil {
                    // Keep the slot during eating
                    SilentHotbar.selectSlotSilently(this@ModuleAutoGapple, this, 1)

                    mc.options.useKey.isPressed = true

                    stopItemUse = player.health + player.absorptionAmount >= health
                        || player.inventory.getStack(this).item != Items.GOLDEN_APPLE
                        || !canUseItem()

                    return@waitUntil stopItemUse
                }

                if (stopItemUse) {
                    releaseUseKey()

                    SilentHotbar.resetSlot(this)
                }
            }
        }
    }

    private fun findEmptyHotbarSlot(): Boolean {
        return ALL_SLOTS_IN_INVENTORY.find {
            it.slotType == ItemSlotType.HOTBAR && it.itemStack.isNothing()
        } != null
    }

    private fun canUseItem() = !InventoryTracker.isInventoryOpenServerSide

    private fun shouldCancelInvMove(): Boolean {
        if (inventoryConstraints.violatesNoMove) {
            if (canCloseMainInventory) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }

            return true
        }

        if (inventoryConstraints.invOpen && !isInInventoryScreen) {
            return true
        }

        if (!player.currentScreenHandler.isPlayerInventory) {
            return true
        }

        return false
    }

    private suspend fun Sequence<DummyEvent>.performInventoryClick(item: ItemSlot): Boolean {
        if (shouldCancelInvMove()) {
            return false
        }

        val slot = item.getIdForServerWithCurrentScreen() ?: return false

        if (!isInInventoryScreen) {
            openInventorySilently()
        }

        val startDelay = inventoryConstraints.startDelay.random()

        if (startDelay > 0) {
            if (!waitConditional(startDelay) { shouldCancelInvMove() }) {
                return false
            }
        }

        interaction.clickSlot(0, slot, 0, SlotActionType.QUICK_MOVE, player)

        if (canCloseMainInventory) {
            waitConditional(inventoryConstraints.closeDelay.random()) { shouldCancelInvMove() }

            // Can it still be closed?
            if (canCloseMainInventory) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }
        }

        return true
    }

    private fun releaseUseKey() {
        if (!mc.options.useKey.pressedOnKeyboard) {
            mc.options.useKey.isPressed = false
        }
    }

    override fun disable() {
        releaseUseKey()
    }
}
