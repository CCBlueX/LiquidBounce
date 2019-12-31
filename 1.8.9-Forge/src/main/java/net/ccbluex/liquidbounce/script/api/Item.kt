package net.ccbluex.liquidbounce.script.api

import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.minecraft.item.ItemStack

/**
 * A script api class for item parsing support
 *
 * @author CCBlueX
 */
object Item {

    /**
     * Register a java script creative tab
     */
    @JvmStatic
    fun createItem(itemArguments : String) : ItemStack {
        return ItemUtils.createItem(itemArguments)
    }
}