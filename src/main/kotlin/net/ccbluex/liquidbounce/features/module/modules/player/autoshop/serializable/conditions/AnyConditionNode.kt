package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions

import kotlinx.serialization.Serializable

@Serializable
data class AnyConditionNode(val any: List<ConditionNode>) : ConditionNode
