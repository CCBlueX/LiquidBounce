package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions

import kotlinx.serialization.Serializable

@Serializable
data class ItemConditionNode(
    val id: String,
    val min: Int = 1,
    val max: Int = Int.MAX_VALUE
) : ConditionNode
