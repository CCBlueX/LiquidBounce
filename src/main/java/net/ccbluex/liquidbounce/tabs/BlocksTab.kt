/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.tabs

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.util.WrappedCreativeTabs


class BlocksTab : WrappedCreativeTabs("Special blocks")
{

    /**
     * Initialize of special blocks tab
     */
    init
    {
        representedType.backgroundImageName = "item_search.png"
    }

    /**
     * Return icon item of tab
     *
     * @return icon item
     */
    override val tabIconItem: IItem
        get() = ItemStack(classProvider.getBlockEnum(BlockType.COMMAND_BLOCK)).item!!

    /**
     * Return name of tab
     *
     * @return tab name
     */
    override val translatedTabLabel
        get() = "Special blocks"

    /**
     * @return searchbar status
     */
    override val hasSearchBar
        get() = true

    /**
     * Add all items to tab
     *
     * @param itemList list of tab items
     */
    override fun displayAllReleventItems(itemList: MutableList<IItemStack>)
    {
        val provider = classProvider

        itemList.add(ItemStack(provider.getBlockEnum(BlockType.COMMAND_BLOCK)))
        itemList.add(ItemStack(provider.getItemEnum(ItemType.COMMAND_BLOCK_MINECART)))
        itemList.add(ItemStack(provider.getBlockEnum(BlockType.BARRIER)))
        itemList.add(ItemStack(provider.getBlockEnum(BlockType.DRAGON_EGG)))
        itemList.add(ItemStack(provider.getBlockEnum(BlockType.BROWN_MUSHROOM_BLOCK)))
        itemList.add(ItemStack(provider.getBlockEnum(BlockType.RED_MUSHROOM_BLOCK)))
        itemList.add(ItemStack(provider.getBlockEnum(BlockType.FARMLAND)))
        itemList.add(ItemStack(provider.getBlockEnum(BlockType.MOB_SPAWNER)))
        itemList.add(ItemStack(provider.getBlockEnum(BlockType.LIT_FURNACE)))
    }
}
