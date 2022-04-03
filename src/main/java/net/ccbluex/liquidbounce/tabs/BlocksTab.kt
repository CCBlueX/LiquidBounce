/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.tabs

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack


class BlocksTab : CreativeTabs("Special blocks") {

    /**
     * Initialize of special blocks tab
     */
    init {
        backgroundImageName = "item_search.png"
    }

    /**
     * Add all items to tab
     *
     * @param itemList list of tab items
     */
    override fun displayAllReleventItems(itemList: MutableList<ItemStack>) {
        itemList.add(ItemStack(Blocks.command_block))
        itemList.add(ItemStack(Items.command_block_minecart))
        itemList.add(ItemStack(Blocks.barrier))
        itemList.add(ItemStack(Blocks.dragon_egg))
        itemList.add(ItemStack(Blocks.brown_mushroom_block))
        itemList.add(ItemStack(Blocks.red_mushroom_block))
        itemList.add(ItemStack(Blocks.farmland))
        itemList.add(ItemStack(Blocks.mob_spawner))
        itemList.add(ItemStack(Blocks.lit_furnace))
    }

    /**
     * Return icon item of tab
     *
     * @return icon item
     */
    override fun getTabIconItem(): Item = ItemStack(Blocks.command_block).item!!

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