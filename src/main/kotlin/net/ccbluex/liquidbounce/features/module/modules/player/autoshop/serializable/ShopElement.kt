package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable

import kotlinx.serialization.Serializable
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions.ConditionNode

@Serializable
data class ShopElement (
    val item: ItemInfo,
    val amountPerClick: Int = 1,
    val categorySlot: Int,
    val itemSlot: Int,
    val price: ItemInfo,
    val purchaseConditions: ConditionNode? = null
)
