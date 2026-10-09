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

package net.ccbluex.liquidbounce.features.inventoryPreset

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions.RestrictionType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.GenericItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSubtype
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.MiningToolItemFacet
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

/**
 * Contains the frontend representation of the user defined preference of what should a slot contain.
 */
sealed class FrontendSlotPreference {
    /**
     * Converts the frontend representation of the user
     * configured preset into a version
     * which the [net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanGenerator] understands.
     */
    abstract fun toBackendRepresentation(): ConvertedSlotPreference
    abstract fun serialize(context: JsonSerializationContext): JsonObject

    class SingleSlotPreference(private val item: Item) : FrontendSlotPreference() {
        companion object {
            /**
             * Some items like bow or crossbow represent an item type with additional sorting logic.
             * Those items must be remapped.
             */
            val itemSpecialTypeMap = mapOf(
                Items.BOW to CleanupPlanTemplate.SlotContentPreference(GenericItemType.BOW),
                Items.CROSSBOW to CleanupPlanTemplate.SlotContentPreference(GenericItemType.CROSSBOW),
            )
        }

        override fun toBackendRepresentation(): ConvertedSlotPreference {
            val specialType = itemSpecialTypeMap[item]

            if (specialType != null) {
                return ConvertedSlotPreference(specialType)
            }

            val contentPreference = CleanupPlanTemplate.SlotContentPreference(
                itemType = GenericItemType.ANY_ITEM,
                subtype = ItemSubtype.SpecificItem(item)
            )

            return ConvertedSlotPreference(contentPreference)
        }

        override fun serialize(context: JsonSerializationContext) = JsonObject().apply {
            addProperty("type", "SINGLE")

            add("item", context.serialize(item))
        }
    }

    class GroupSlotPreference(private val itemGroupType: ItemGroupType) : FrontendSlotPreference() {
        override fun toBackendRepresentation(): ConvertedSlotPreference {
            return ConvertedSlotPreference(itemGroupType.preference)
        }

        /**
         * Enum representing item categories used for preset item classification.
         */
        @Suppress("UNUSED")
        enum class ItemGroupType(
            private val itemType: GenericItemType,
            private val subtype: ItemSubtype = ItemSubtype.None,
        ) {
            @SerializedName("ARROWS")
            ARROWS(GenericItemType.ARROW),

            @SerializedName("SWORD")
            SWORD(GenericItemType.SWORD),

            @SerializedName("WEAPON")
            WEAPON(GenericItemType.WEAPON),

            @SerializedName("AXE")
            AXE_TOOL(GenericItemType.TOOL, ItemSubtype.ToolTypes(MiningToolItemFacet.MASK_AXE)),

            @SerializedName("HOE")
            HOE_TOOL(GenericItemType.TOOL, ItemSubtype.ToolTypes(MiningToolItemFacet.MASK_HOE)),

            @SerializedName("SHOVEL")
            SHOVEL_TOOL(GenericItemType.TOOL, ItemSubtype.ToolTypes(MiningToolItemFacet.MASK_SHOVEL)),

            @SerializedName("PICKAXE")
            PICKAXE_TOOL(GenericItemType.TOOL, ItemSubtype.ToolTypes(MiningToolItemFacet.MASK_PICKAXE)),

            @SerializedName("FOOD")
            FOOD(GenericItemType.FOOD),

            @SerializedName("POTION")
            POTION(GenericItemType.POTION),

            @SerializedName("BLOCK")
            BLOCK(GenericItemType.BLOCK),

            @SerializedName("THROWABLE")
            THROWABLE(GenericItemType.THROWABLE),

            @SerializedName("SPEAR")
            SPEAR(GenericItemType.SPEAR),

            @SerializedName("MACE")
            MACE(GenericItemType.MACE),

            @SerializedName("SHIELD")
            SHIELD(GenericItemType.SHIELD),

            @SerializedName("ROD")
            ROD(GenericItemType.ROD);

            val preference: CleanupPlanTemplate.SlotContentPreference
                get() = CleanupPlanTemplate.SlotContentPreference(itemType, subtype)
        }

        override fun serialize(context: JsonSerializationContext) = JsonObject().apply {
            addProperty("type", "GROUP")

            add("group", context.serialize(itemGroupType))
        }
    }

    data object IgnoreSlotPreference : FrontendSlotPreference() {
        override fun toBackendRepresentation(): ConvertedSlotPreference {
            return ConvertedSlotPreference(null, RestrictionType.FORBID_TAMPERING)
        }

        override fun serialize(context: JsonSerializationContext) = JsonObject().apply {
            addProperty("type", "IGNORE")
        }
    }

    data object AnySlotPreference : FrontendSlotPreference() {
        override fun toBackendRepresentation(): ConvertedSlotPreference {
            return ConvertedSlotPreference(null, RestrictionType.NONE)
        }

        override fun serialize(context: JsonSerializationContext) = JsonObject().apply {
            addProperty("type", "ANY")
        }
    }

    data class ConvertedSlotPreference(
        val contentPreference: CleanupPlanTemplate.SlotContentPreference?,
        val slotRestriction: RestrictionType = RestrictionType.NONE
    )
}
