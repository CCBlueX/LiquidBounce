/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
/*package net.ccbluex.liquidbounce.tabs

import com.google.gson.JsonParser
import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.inventory.ItemUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class HeadsTab : CreativeTabs("Heads") {

    // List of heads
    private val heads = arrayListOf<ItemStack>()

    /**
     * Constructor of heads tab
     */
    init {
        backgroundImageName = "item_search.png"

        // Launch the coroutine to load heads asynchronously
        GlobalScope.launch { loadHeads() }
    }

    private suspend fun loadHeads() {
        runBlocking {
            runCatching {
                LOGGER.info("Loading heads...")

                // Asynchronously fetch the heads configuration
                val responseDeferred = async { get("$CLIENT_CLOUD/heads.json") }
                val (response, _) = responseDeferred.await()
                val headsConfiguration = JsonParser().parse(response)

                // Process the heads configuration
                if (!headsConfiguration.isJsonObject) return@runBlocking

                val headsConf = headsConfiguration.asJsonObject

                if (headsConf["enabled"].asBoolean) {
                    val url = headsConf["url"].asString

                    LOGGER.info("Loading heads from $url...")

                    // Asynchronously fetch the heads data
                    val headsResponseDeferred = async { get(url) }
                    val (headsResponse, _) = headsResponseDeferred.await()
                    val headsElement = JsonParser().parse(headsResponse)

                    // Process the heads data
                    if (!headsElement.isJsonObject) {
                        LOGGER.error("Something is wrong, the heads json is not a JsonObject!")
                        return@runBlocking
                    }

                    val headsObject = headsElement.asJsonObject

                    for ((_, value) in headsObject.entrySet()) {
                        val headElement = value.asJsonObject

                        heads += ItemUtils.createItem("skull 1 3 {display:{Name:\"${headElement["name"].asString}\"},SkullOwner:{Id:\"${headElement["uuid"].asString}\",Properties:{textures:[{Value:\"${headElement["value"].asString}\"}]}}}")!!
                    }

                    LOGGER.info("Loaded ${heads.size} heads from HeadDB.")
                } else
                    LOGGER.info("Heads are disabled.")
            }.onFailure {
                LOGGER.error("Error while reading heads.", it)
            }
        }
    }

    /**
     * Add all items to tab
     *
     * @param itemList list of tab items
     */
    override fun displayAllReleventItems(itemList: MutableList<ItemStack>) {
        itemList += heads
    }

    /**
     * Return icon item of tab
     *
     * @return icon item
     */
    override fun getTabIconItem(): Item = Items.skull

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
}*/