package net.ccbluex.liquidbounce.features.itemgroup

import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.logger
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

/**
 * An item group from the client
 */
open class ClientItemGroup(
    val plainName: String,
    val icon: () -> ItemStack,
    val items: (items: ItemGroup.Entries) -> Unit
) {

    // Create item group and assign to minecraft groups
    fun create(): ItemGroup {
        // Expand array
        val itemGroup = FabricItemGroup.builder()
            .displayName(plainName.asText())
            .icon(icon)
            .entries { displayContext, entries ->
                runCatching {
                    items(entries)
                }.onFailure {
                    logger.error("Unable to create item group $plainName", it)
                }
            }
            .build()

        // Add tab to creative inventory
        Registry.register(Registries.ITEM_GROUP, Identifier("liquidbounce", plainName.lowercase()), itemGroup)

        return itemGroup
    }

}
