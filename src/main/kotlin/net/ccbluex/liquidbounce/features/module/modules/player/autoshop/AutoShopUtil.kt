package net.ccbluex.liquidbounce.features.module.modules.player.autoshop

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
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
data class ShopConfig (
    val traderTitle: String,
    val initialCategorySlot: Int,
    val elements: List<ShopElement>
) {
    companion object {
        fun emptyConfig() : ShopConfig {
            return ShopConfig("", -1, emptyList())
        }
    }
}

@Serializable
data class ShopElement (
    val id: String,
    val minAmount: Int = 1,
    val amountPerClick: Int = 1,
    val categorySlot: Int,
    val itemSlot: Int,
    val price: PriceInfo,
    val purchaseConditions: ConditionNode? = null
)

@Serializable
data class PriceInfo(val id: String, val minAmount: Int)

@Serializable
data class ItemInfo(
    val id: String,
    val min: Int = 1,
    val max: Int = Int.MAX_VALUE
) : ConditionNode

@Serializable
data class AnyConditionNode(val any: List<ConditionNode>) : ConditionNode

@Serializable
data class AllConditionNode(val all: List<ConditionNode>) : ConditionNode

@Serializable(with = ConditionNodeSerializer::class)
sealed interface ConditionNode

object ConditionNodeSerializer : JsonContentPolymorphicSerializer<ConditionNode>(ConditionNode::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out ConditionNode> {
        return when {
            "id" in element.jsonObject -> ItemInfo.serializer()
            "any" in element.jsonObject -> AnyConditionNode.serializer()
            "all" in element.jsonObject -> AllConditionNode.serializer()
            else -> throw IllegalArgumentException("Unknown type: ${element.jsonObject}")
        }
    }
}
