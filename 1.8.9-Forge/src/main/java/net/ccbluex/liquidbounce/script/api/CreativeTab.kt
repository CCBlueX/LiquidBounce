package net.ccbluex.liquidbounce.script.api

import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.JSType
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

/**
 * A script api class for creative tabs support
 *
 * @author CCBlueX
 */
object CreativeTab {

    /**
     * Register a java script creative tab
     *
     * @param scriptObjectMirror Java Script function for creative tab (like a CreativeTabs class)
     */
    @JvmStatic
    fun registerTab(scriptObjectMirror : ScriptObjectMirror) {
        object : CreativeTabs(JSType.toString(scriptObjectMirror.callMember("getLabel"))) {

            override fun displayAllReleventItems(p_78018_1_ : MutableList<ItemStack>?) {
                scriptObjectMirror.callMember("displayAllReleventItems", p_78018_1_)
            }

            override fun getTabIconItem() : Item {
                return JSType.toObject(scriptObjectMirror.callMember("getTabIconItem")) as Item
            }

            override fun getTranslatedTabLabel() : String {
                return JSType.toString(scriptObjectMirror.callMember("getLabel"))
            }

        }
    }
}