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
import net.ccbluex.liquidbounce.utils.timer.TimeUtils

class HeadsTab : WrappedCreativeTabs("Heads")
{

	// List of heads
	private val heads = ArrayList<IItemStack>()

	/**
	 * Constructor of heads tab
	 */
	init
	{
		representedType.backgroundImageName = "item_search.png"

		loadHeads() // TODO: Asynchronize
	}

	/**
	 * Load all heads from the database
	 */
	private fun loadHeads()
	{
		try
		{
			ClientUtils.logger.info("Loading heads...")

			val headsConfiguration = JsonParser().parse(HttpUtils["${LiquidBounce.CLIENT_CLOUD}/heads.json"])

			if (!headsConfiguration.isJsonObject) return

			val headsConf = headsConfiguration.asJsonObject

			if (headsConf.get("enabled").asBoolean)
			{
				val nanoTime = System.nanoTime()

				val url = headsConf.get("url").asString

				ClientUtils.logger.info("Loading heads from $url...")

				val headsElement = JsonParser().parse(HttpUtils[url])

				if (!headsElement.isJsonObject)
				{
					ClientUtils.logger.error("Something is wrong, the heads json is not a JsonObject!")
					return
				}

				headsElement.asJsonObject.entrySet().map { it.value.asJsonObject }.forEach { ItemUtils.createItem("skull 1 3 {display:{Name:\"${it.get("name").asString}\"},SkullOwner:{Id:\"${it.get("uuid").asString}\",Properties:{textures:[{Value:\"${it.get("value").asString}\"}]}}}")?.let(heads::add) }

				ClientUtils.logger.info("Loaded " + heads.size + " heads from HeadDB. Took ${TimeUtils.nanosecondsToString(System.nanoTime() - nanoTime)}.")
			}
			else ClientUtils.logger.info("Heads are disabled.")
		}
		catch (e: Exception)
		{
			ClientUtils.logger.error("Error while reading heads.", e)
		}
	}

	/**
	 * Add all items to tab
	 *
	 * @param itemList list of tab items
	 */
	override fun displayAllReleventItems(itemList: MutableList<IItemStack>)
	{
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
