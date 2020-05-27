/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script.api

import jdk.nashorn.api.scripting.JSObject
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

@Suppress("UNCHECKED_CAST", "unused")
class ScriptTab(private val tabObject: JSObject) : CreativeTabs(tabObject.getMember("name") as String) {

    override fun getTabIconItem() = Items::class.java.getField(tabObject.getMember("icon") as String).get(null) as Item

    override fun getTranslatedTabLabel() = tabObject.getMember("name") as String

    // TODO: Use array instead of function
    override fun displayAllReleventItems(p_78018_1_: MutableList<ItemStack>?) {
        (tabObject.getMember("items") as JSObject).call(tabObject, p_78018_1_)
    }
}