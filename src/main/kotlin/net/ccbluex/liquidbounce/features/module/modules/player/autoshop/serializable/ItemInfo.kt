package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable

import kotlinx.serialization.Serializable

@Serializable
data class ItemInfo(val id: String, val minAmount: Int = 1)
