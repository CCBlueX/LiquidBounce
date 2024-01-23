package net.ccbluex.liquidbounce.features.itemgroup.groups

import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroup
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.utils.client.asText
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack

class ContainerItemGroup : ClientItemGroup(
    "Containers",
    icon = { ItemStack(Blocks.CHEST) },
    items = {
        // Add empty chest to keep the chest tab
        it.add(ItemStack(Blocks.CHEST).setCustomName("Empty Chest".asText()))

        // Add all stored containers
        it.addAll(ClientItemGroups.containersAsItemStacks())
    }
)
