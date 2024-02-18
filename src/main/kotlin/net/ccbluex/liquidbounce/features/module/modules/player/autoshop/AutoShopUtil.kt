package net.ccbluex.liquidbounce.features.module.modules.player.autoshop

import kotlinx.serialization.Serializable
import net.ccbluex.liquidbounce.config.AutoShopConfig
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.item.Item
import net.minecraft.text.Text

/**
 * Loads [configFileName] and displays a notification depending on the result
 */
fun loadAutoShopConfig(configFileName: String, moduleName: String = ModuleAutoShop.name) {
    val result = AutoShopConfig.load(configFileName)
    val message = if (result)
        Text.translatable("liquidbounce.module.autoShop.reload.success")
    else Text.translatable("liquidbounce.module.autoShop.reload.error")

    notification(message, moduleName,
        if (result) NotificationEvent.Severity.INFO else NotificationEvent.Severity.ERROR
    )
}

data class AutoShopConfig (
    val traderTitle: String,
    val initialCategorySlot: Int,
    val elements: MutableList<AutoShopElement>
)

data class AutoShopElement (
    var item: Item,
    val minAmount: Int,
    val amountPerClick: Int = 1,
    val categorySlot: Int,
    val itemSlot: Int,
    val price: List<Map<Item, Int>>,
    val itemsToCheckBeforeBuying: List<Pair<Item, Int>>?
)

@Serializable
data class RawShopConfig (
    val traderTitle: String,
    val initialCategorySlot: Int,
    val items: List<RawShopElement>
)

@Serializable
data class RawShopElement (
    var itemID: String,
    var minAmount: Int,
    val amountPerClick: Int = 1,
    val categorySlot: Int,
    val itemSlot: Int,
    val price: List<List<RawItemInfo>>,
    val checkItems: List<RawItemInfo>? = null
)

@Serializable
data class RawItemInfo (
    var itemID: String,
    var minAmount: Int,
)
