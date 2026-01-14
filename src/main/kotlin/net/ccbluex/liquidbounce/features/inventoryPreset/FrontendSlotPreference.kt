package net.ccbluex.liquidbounce.features.inventoryPreset

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions.RestrictionType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.GenericItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.MiningToolItemFacet
import net.minecraft.item.Item
import net.minecraft.item.Items

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
                subtypes = setOf(item)
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
        enum class ItemGroupType(val preference: CleanupPlanTemplate.SlotContentPreference) {
            @SerializedName("ARROWS")
            ARROWS(CleanupPlanTemplate.SlotContentPreference(GenericItemType.ARROW)),
            @SerializedName("SWORD")
            SWORD(CleanupPlanTemplate.SlotContentPreference(GenericItemType.SWORD)),
            @SerializedName("WEAPON")
            WEAPON(CleanupPlanTemplate.SlotContentPreference(GenericItemType.WEAPON)),
            @SerializedName("AXE")
            AXE_TOOL(
                CleanupPlanTemplate.SlotContentPreference(
                    GenericItemType.TOOL,
                    setOf(MiningToolItemFacet.ItemToolType.AXE)
                )
            ),
            @SerializedName("HOE")
            HOE_TOOL(
                CleanupPlanTemplate.SlotContentPreference(
                    GenericItemType.TOOL,
                    setOf(MiningToolItemFacet.ItemToolType.HOE)
                )
            ),
            @SerializedName("SHOVEL")
            SHOVEL_TOOL(
                CleanupPlanTemplate.SlotContentPreference(
                    GenericItemType.TOOL,
                    setOf(MiningToolItemFacet.ItemToolType.SHOVEL)
                )
            ),
            @SerializedName("PICKAXE")
            PICKAXE_TOOL(
                CleanupPlanTemplate.SlotContentPreference(
                    GenericItemType.TOOL,
                    setOf(MiningToolItemFacet.ItemToolType.PICKAXE)
                )
            ),
            @SerializedName("FOOD")
            FOOD(CleanupPlanTemplate.SlotContentPreference(GenericItemType.FOOD)),
            @SerializedName("POTION")
            POTION(CleanupPlanTemplate.SlotContentPreference(GenericItemType.POTION)),
            @SerializedName("BLOCK")
            BLOCK(CleanupPlanTemplate.SlotContentPreference(GenericItemType.BLOCK)),
            @SerializedName("THROWABLE")
            THROWABLE(CleanupPlanTemplate.SlotContentPreference(GenericItemType.THROWABLE))
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
