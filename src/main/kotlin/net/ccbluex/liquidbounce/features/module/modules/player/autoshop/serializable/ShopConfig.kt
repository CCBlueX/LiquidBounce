package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable

import kotlinx.serialization.Serializable

@Serializable
data class ShopConfig (
    val traderTitle: String,
    val initialCategorySlot: Int,
    val elements: List<ShopElement>
) {
    companion object {
        fun emptyConfig() = ShopConfig("", -1, emptyList())
    }
}
