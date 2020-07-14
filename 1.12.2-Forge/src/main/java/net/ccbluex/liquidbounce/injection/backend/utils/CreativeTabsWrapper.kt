package net.ccbluex.liquidbounce.injection.backend.utils

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.util.WrappedCreativeTabs
import net.ccbluex.liquidbounce.api.util.WrappedMutableList
import net.ccbluex.liquidbounce.injection.backend.unwrap
import net.ccbluex.liquidbounce.injection.backend.wrap
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList

class CreativeTabsWrapper(val wrapped: WrappedCreativeTabs, name: String) : CreativeTabs(name) {
    override fun getTabIconItem(): ItemStack = ItemStack(wrapped.getTabIconItem().unwrap())
    override fun displayAllRelevantItems(items: NonNullList<ItemStack>) = wrapped.displayAllReleventItems(WrappedMutableList(items!!, IItemStack::unwrap, ItemStack::wrap))
    override fun getTranslatedTabLabel(): String = wrapped.getTranslatedTabLabel()
    override fun hasSearchBar(): Boolean = wrapped.hasSearchBar()
}