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
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl.classProvider


class BlocksTab : WrappedCreativeTabs("Special blocks") {

    /**
     * Initialize of special blocks tab
     */
    init {
        representedType.backgroundImageName = "item_search.png"
    }

    /**
     * Add all items to tab
     *
     * @param itemList list of tab items
     */
    override fun displayAllReleventItems(itemList: MutableList<IItemStack>) {
        itemList.add(classProvider.createItemStack(classProvider.getBlockEnum(BlockType.COMMAND_BLOCK)))
        itemList.add(classProvider.createItemStack(classProvider.getItemEnum(ItemType.COMMAND_BLOCK_MINECART)))
        itemList.add(classProvider.createItemStack(classProvider.getBlockEnum(BlockType.BARRIER)))
        itemList.add(classProvider.createItemStack(classProvider.getBlockEnum(BlockType.DRAGON_EGG)))
        itemList.add(classProvider.createItemStack(classProvider.getBlockEnum(BlockType.BROWN_MUSHROOM_BLOCK)))
        itemList.add(classProvider.createItemStack(classProvider.getBlockEnum(BlockType.RED_MUSHROOM_BLOCK)))
        itemList.add(classProvider.createItemStack(classProvider.getBlockEnum(BlockType.FARMLAND)))
        itemList.add(classProvider.createItemStack(classProvider.getBlockEnum(BlockType.MOB_SPAWNER)))
        itemList.add(classProvider.createItemStack(classProvider.getBlockEnum(BlockType.LIT_FURNACE)))
    }

    /**
     * Return icon item of tab
     *
     * @return icon item
     */
    override fun getTabIconItem(): IItem = classProvider.createItemStack(classProvider.getBlockEnum(BlockType.COMMAND_BLOCK)).item!!

    /**
     * Return name of tab
     *
     * @return tab name
     */
    override fun getTranslatedTabLabel() = "Special blocks"

    /**
     * @return searchbar status
     */
    override fun hasSearchBar() = true
}