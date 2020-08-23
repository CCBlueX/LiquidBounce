/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.tabs

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.util.WrappedCreativeTabs
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl.classProvider
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import java.util.*

class HeadsTab : WrappedCreativeTabs("Heads") {

    // List of heads
    private val heads = ArrayList<IItemStack>()

    /**
     * Constructor of heads tab
     */
    init {
        representedType.backgroundImageName = "item_search.png"

        loadHeads()
    }

    /**
     * Load all heads from the database
     */
    private fun loadHeads() {
        try {
            ClientUtils.getLogger().info("Loading heads...")

            val headsConfiguration = JsonParser().parse(HttpUtils.get("${LiquidBounce.CLIENT_CLOUD}/heads.json"))

            if (!headsConfiguration.isJsonObject) return

            val headsConf = headsConfiguration.asJsonObject

            if (headsConf.get("enabled").asBoolean) {
                val url = headsConf.get("url").asString

                ClientUtils.getLogger().info("Loading heads from $url...")

                val headsElement = JsonParser().parse(HttpUtils.get(url))

                if (!headsElement.isJsonObject) {
                    ClientUtils.getLogger().error("Something is wrong, the heads json is not a JsonObject!")
                    return
                }

                val headsObject = headsElement.asJsonObject

                for ((_, value) in headsObject.entrySet()) {
                    val headElement = value.asJsonObject

                    heads.add(ItemUtils.createItem("skull 1 3 {display:{Name:\"${headElement.get("name").asString}\"},SkullOwner:{Id:\"${headElement.get("uuid").asString}\",Properties:{textures:[{Value:\"${headElement.get("value").asString}\"}]}}}"))
                }

                ClientUtils.getLogger().info("Loaded " + heads.size + " heads from HeadDB.")
            } else
                ClientUtils.getLogger().info("Heads are disabled.")
        } catch (e: Exception) {
            ClientUtils.getLogger().error("Error while reading heads.", e)
        }
    }

    /**
     * Add all items to tab
     *
     * @param itemList list of tab items
     */
    override fun displayAllReleventItems(itemList: MutableList<IItemStack>) {
        itemList.addAll(heads)
    }

    /**
     * Return icon item of tab
     *
     * @return icon item
     */
    override fun getTabIconItem(): IItem = classProvider.getItemEnum(ItemType.SKULL)

    /**
     * Return name of tab
     *
     * @return tab name
     */
    override fun getTranslatedTabLabel() = "Heads"

    /**
     * @return searchbar status
     */
    override fun hasSearchBar() = true
}