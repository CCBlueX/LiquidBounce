package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.ALL_SLOTS_IN_INVENTORY
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorKitParameters
import net.ccbluex.liquidbounce.utils.item.ArmorParameter
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.AnimalArmorItem
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
            return@mapNotNull when (val item = slot.itemStack.item) {
                is ArmorItem -> {
                    // Filter out animal armor which is an armor item but not for the player
                    if (item is AnimalArmorItem) {
                        return@mapNotNull null
                    }

                    ArmorPiece(slot)
                }
                else -> null
            }
        }.groupBy(ArmorPiece::slotType)

        return armorPiecesGroupedByType
    }

    fun getArmorComparatorFor(currentKit: Map<EquipmentSlot, ArmorPiece?>): ArmorComparator {
        return getArmorComparatorForParameters(ArmorKitParameters.getParametersForSlots(currentKit))
    }

    fun getArmorComparatorForParameters(currentParameters: ArmorKitParameters): ArmorComparator {
        return ArmorComparator(EXPECTED_DAMAGE, currentParameters)
    }


}
