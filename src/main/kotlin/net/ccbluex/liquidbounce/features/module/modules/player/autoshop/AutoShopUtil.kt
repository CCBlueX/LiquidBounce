package net.ccbluex.liquidbounce.features.module.modules.player.autoshop

import kotlinx.serialization.Serializable
import net.ccbluex.liquidbounce.config.AutoShopConfig
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.text.Text

/**
 * Loads [configFileName] and displays a notification depending on the result
 */
fun loadAutoShopConfig(configFileName: String, moduleName: String = ModuleAutoShop.name) : Boolean {
    val result = AutoShopConfig.load(configFileName)
    val message = if (result) { Text.translatable("liquidbounce.module.autoShop.reload.success") }
                else { Text.translatable("liquidbounce.module.autoShop.reload.error") }

    notification(message, moduleName,
        if (result) NotificationEvent.Severity.INFO else NotificationEvent.Severity.ERROR
    )
    return result
}

@Serializable
data class AutoShopConfig (
    val traderTitle: String,
    val initialCategorySlot: Int,
    val elements: List<ShopElement>
)

@Serializable
data class ShopElement (
    val id: String,
    val minAmount: Int = 1,
    val amountPerClick: Int = 1,
    val categorySlot: Int,
    val itemSlot: Int,
    val price: ItemInfo,
    val purchaseConditions: ConditionNode? = null
)

@Serializable
data class ItemInfo(
    val id: String,
    val min: Int = 1,
    val max: Int = Int.MAX_VALUE
) : ConditionNode

@Serializable
data class AnyConditionNode(
    val any: List<ConditionNode>
) : ConditionNode

@Serializable
data class AllConditionNode(
    val all: List<ConditionNode>
) : ConditionNode

@Suppress("EmptyClassBlock")
interface ConditionNode
