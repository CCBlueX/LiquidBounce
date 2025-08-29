package net.ccbluex.liquidbounce.utils.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.Item
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.TagKey
import java.util.EnumSet

val Item.isSword: Boolean
    get() = isIn(ItemTags.SWORDS)

val Item.isTool: Boolean
    get() = components.get(DataComponentTypes.TOOL) != null

private val ARMOR_EQUIPMENT_SLOTS = EnumSet.of(
    EquipmentSlot.HEAD,
    EquipmentSlot.BODY,
    EquipmentSlot.LEGS,
    EquipmentSlot.FEET
)

val Item.isArmor: Boolean
    get() {
        val attrib = components.get(DataComponentTypes.EQUIPPABLE) ?: return false

        return attrib.slot in ARMOR_EQUIPMENT_SLOTS && attrib.allowedEntities.isEmpty
    }

fun Item.isIn(itemTag: TagKey<Item>) = registryEntry.isIn(itemTag)

operator fun TagKey<Item>.contains(item: Item) = item.isIn(this)
