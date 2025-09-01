/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

/**
 * FastExp module
 *
 * Automatically repairs your armor.
 */
@Suppress("MagicNumber")
object ModuleFastExp : ClientModule(
    "FastExp",
    Category.PLAYER,
    bindAction = InputBind.BindAction.HOLD,
    disableOnQuit = true
) {

    private object Rotate : ToggleableConfigurable(this, "Rotate", true) {
        val rotations = tree(RotationsConfigurable(this))
    }

    init {
        tree(Rotate)
    }

    private val noWaste by boolean("NoWaste", true)
    private val itemsPerTick by int("ItemsPerTick", 5, 1..32)
    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..40, "ticks")

    @Suppress("unused", "ComplexCondition")
    private val repeatable = tickHandler {
        val slot = Slots.OffhandWithHotbar.findSlot(Items.EXPERIENCE_BOTTLE)
        if (slot == null || player.isDead || InventoryManager.isInventoryOpen || isRepaired(slot)) {
            return@tickHandler
        }

        CombatManager.pauseCombatForAtLeast(combatPauseTime)

        if (Rotate.enabled) {
            waitUntil {
                val rotation = Rotation(player.yaw, 90f)
                RotationManager.setRotationTarget(
                    Rotate.rotations.toRotationTarget(rotation),
                    Priority.IMPORTANT_FOR_USAGE_3,
                    this@ModuleFastExp
                )
                RotationManager.serverRotation.pitch > 85f
            }
        }

        val pitch = if (Rotate.enabled) RotationManager.serverRotation.pitch else 90f
        repeat(itemsPerTick) {
            useHotbarSlotOrOffhand(
                slot,
                slotResetDelay.random(),
                pitch = pitch
            )
        }
    }

    private fun isRepaired(slot: HotbarItemSlot): Boolean {
        if (!noWaste) {
            return false
        }

        // an item in the other hand, not holding the exp bottle could also get repaired
        val possibleSlot = if (slot == OffHandSlot) {
            player.mainHandStack
        } else {
            player.offHandStack
        }

        return player.inventory.armor.any { itemStack -> isRepaired(itemStack) } || isRepaired(possibleSlot) ||
            player.inventory.armor.all { itemStack -> noMending(itemStack) } && noMending(possibleSlot)
    }

    private fun isRepaired(itemStack: ItemStack) = itemStack.getEnchantment(Enchantments.MENDING) != 0 &&
        itemStack.damage <= 0

    private fun noMending(itemStack: ItemStack?) = itemStack.getEnchantment(Enchantments.MENDING) == 0

}
