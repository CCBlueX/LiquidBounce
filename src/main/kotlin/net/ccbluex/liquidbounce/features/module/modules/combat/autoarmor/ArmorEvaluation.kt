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
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.ALL_SLOTS_IN_INVENTORY
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorParameter
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ArmorItem

object ArmorEvaluation {
    /**
     * We expect damage to be around diamond sword hits
     */
    private const val EXPECTED_DAMAGE: Float = 6.0F

    fun findBestArmorPieces(
        slots: List<ItemSlot> = ALL_SLOTS_IN_INVENTORY
    ): Map<EquipmentSlot, ArmorPiece?> {
        val armorPiecesGroupedByType = groupArmorByType(slots)

        // We start with assuming that the best pieces are those which have the most damage points.
        var currentBestPieces = armorPiecesGroupedByType.mapValues { (_, piecesForType) ->
            piecesForType.maxByOrNull { it.toughness.toDouble() }
        }

        // Run some passes in which we try to find best armor pieces based on the parameters of the last pass
        for (ignored in 0 until 2) {
            val comparator = getArmorComparatorFor(currentBestPieces)

            currentBestPieces = armorPiecesGroupedByType.mapValues { it.value.maxWithOrNull(comparator) }
        }

        return currentBestPieces
    }

    fun findBestArmorPiecesWithComparator(
        slots: List<ItemSlot> = ALL_SLOTS_IN_INVENTORY,
        comparator: ArmorComparator
    ): Map<EquipmentSlot, ArmorPiece?> {
        val armorPiecesGroupedByType = groupArmorByType(slots)

        return armorPiecesGroupedByType.mapValues { it.value.maxWithOrNull(comparator) }
    }

    private fun groupArmorByType(slots: List<ItemSlot>): Map<EquipmentSlot, List<ArmorPiece>> {
        val armorPiecesGroupedByType = slots.mapNotNull { slot ->
            return@mapNotNull when (slot.itemStack.item) {
                is ArmorItem -> ArmorPiece(slot)
                else -> null
            }
        }.groupBy(ArmorPiece::slotType)
        return armorPiecesGroupedByType
    }

    fun getArmorComparatorFor(currentKit: Map<EquipmentSlot, ArmorPiece?>): ArmorComparator {
        return getArmorComparatorForParameters(getParametersForSlots(currentKit))
    }

    fun getArmorComparatorForParameters(currentParameters: Map<EquipmentSlot, ArmorParameter>): ArmorComparator {
        return ArmorComparator(EXPECTED_DAMAGE, currentParameters)
    }

    /**
     * Returns for each slot the summed up armor parameters without that slot.
     */
    private fun getParametersForSlots(currentKit: Map<EquipmentSlot, ArmorPiece?>): Map<EquipmentSlot, ArmorParameter> {
        // Sum up all parameters
        val totalArmorParameter =
            currentKit.values.fold(ArmorParameter(0.0F, 0.0F)) { acc, armorPiece ->
                if (armorPiece != null) {
                    ArmorParameter(
                        acc.defensePoints + armorPiece.defensePoints,
                        acc.toughness + armorPiece.toughness
                    )
                } else {
                    acc
                }
            }

        // Return the parameter sum for each slot without the current slot
        return currentKit.mapValues { (_, armorPiece) ->
            if (armorPiece != null) {
                ArmorParameter(
                    totalArmorParameter.defensePoints - armorPiece.defensePoints,
                    totalArmorParameter.toughness - armorPiece.toughness
                )
            } else {
                totalArmorParameter
            }
        }
    }

}
