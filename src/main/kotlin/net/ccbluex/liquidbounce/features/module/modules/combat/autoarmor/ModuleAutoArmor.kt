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
package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.AutoArmorSaveArmor.durabilityThreshold
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.ModuleAutoArmor.performMoveOrHotbarClick
import net.ccbluex.liquidbounce.utils.inventory.ArmorItemSlot
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.hasInventorySpace
import net.ccbluex.liquidbounce.utils.item.armor.ArmorComparatorMode
import net.ccbluex.liquidbounce.utils.item.armor.ArmorEvaluation
import net.ccbluex.liquidbounce.utils.item.armor.ArmorPiece
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.world.item.Items

/**
 * AutoArmor module
 *
 * Automatically puts on the best armor.
 */
object ModuleAutoArmor : ClientModule("AutoArmor", ModuleCategories.COMBAT) {

    val inventoryConstraints = tree(PlayerInventoryConstraints())

    /**
     * Selects how armor pieces are ranked against each other. See [ArmorComparatorMode].
     */
    val modes = choices("Mode", Smart, arrayOf(Smart, RawDefense))

    /**
     * The original damage-model ranking. Weighs every protection enchantment into the modern,
     * toughness-aware damage formula.
     */
    object Smart : Mode("Smart") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        val comparatorMode = ArmorComparatorMode.SMART
    }

    /**
     * SkyWars/BedWars oriented ranking. Picks the armor with the highest real damage reduction under the
     * legacy 1.8 model, ignoring niche enchantments (Fire/Blast Protection, Thorns, Unbreaking, ...) that
     * would otherwise trick the module into preferring objectively worse armor.
     */
    object RawDefense : Mode("RawDefense") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        /**
         * Whether Projectile Protection should influence the ranking as a deciding argument (useful on
         * archer-heavy servers). When disabled it only matters when everything else is equal.
         */
        val considerProjectileProtection by boolean("ConsiderProjectileProtection", true)

        val comparatorMode = ArmorComparatorMode.RAW_DEFENSE
    }

    /**
     * Should the module use the hotbar to equip armor pieces?
     * If disabled, it will only use inventory moves.
     */
    object UseHotbar : ToggleableValueGroup(this, "Hotbar", true) {
        /**
         * Defines whether the [UseHotbar] option supports the armor swap from MC 1.19.4+.
         */
        val canSwapArmor by boolean("CanSwapArmor", false)
    }

    init {
        tree(UseHotbar)
        tree(AutoArmorSaveArmor)
    }

    @Suppress("unused")
    private val scheduleHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (player.isSpectator) {
            return@handler
        }

        // Filter out already equipped armor pieces
        val durabilityThreshold = if (AutoArmorSaveArmor.enabled) durabilityThreshold else Int.MIN_VALUE

        val comparatorMode = (modes.activeMode as? RawDefense)?.comparatorMode ?: Smart.comparatorMode
        val considerProjectileProtection = RawDefense.considerProjectileProtection

        val armorToEquip = ArmorEvaluation
            .findBestArmorPieces(
                durabilityThreshold = durabilityThreshold,
                mode = comparatorMode,
                considerProjectileProtection = considerProjectileProtection
            )
            .values.filterNotNull().filter { !it.isAlreadyEquipped }

        for (armorPiece in armorToEquip) {
            event.schedule(
                inventoryConstraints,
                equipArmorPiece(armorPiece) ?: continue,
                Priority.IMPORTANT_FOR_PLAYER_LIFE
            )
        }
    }

    /**
     * Tries to move the given armor piece in the target slot in the inventory. There are two possible behaviors:
     * 1. If there is no free space in the target slot, it will make space in that slot (see [performMoveOrHotbarClick])
     * 2. If there is free space, it will move the armor piece there
     *
     * @return false if a move was not possible, true if a move occurred
     */
    private fun equipArmorPiece(armorPiece: ArmorPiece): InventoryAction? {
        val stackInArmor = player.inventory.getItem(armorPiece.inventorySlot)

        if (stackInArmor.item == Items.ELYTRA) {
            return null
        }

        return performMoveOrHotbarClick(armorPiece, isInArmorSlot = !stackInArmor.isEmpty)
    }

    /**
     * Central move-function of this module. There are following options:
     * 1. If the slot is in the hotbar, we do a right-click on it (if possible)
     * 2. If the slot is in inventory, we shift+left click it
     * 3. If the slot is an armor slot and there is free space in inventory, we shift+left click it otherwise
     * throw it out.
     *
     * @param isInArmorSlot True if the slot is an armor slot.
     * @return True if a move occurred.
     */
    private fun performMoveOrHotbarClick(
        armorPiece: ArmorPiece,
        isInArmorSlot: Boolean
    ): InventoryAction {
        val inventorySlot = armorPiece.itemSlot
        val armorPieceSlot = if (isInArmorSlot) ArmorItemSlot(armorPiece.slotType) else inventorySlot

        val canTryHotbarMove = UseHotbar.enabled &&
            !InventoryManager.isInventoryOpen && (!isInArmorSlot || UseHotbar.canSwapArmor)

        if (inventorySlot is HotbarItemSlot && canTryHotbarMove) {
            return InventoryAction.UseItem(inventorySlot, this)
        }

        // Should the item be just thrown out of the inventory
        val shouldThrow = isInArmorSlot && !hasInventorySpace()

        return if (shouldThrow) {
            InventoryAction.Click.performThrow(screen = null, armorPieceSlot)
        } else {
            InventoryAction.Click.performQuickMove(screen = null, armorPieceSlot)
        }
    }

}
