package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
@Serializable(with = ConditionNodeSerializer::class)
sealed interface ConditionNode

object ConditionNodeSerializer : JsonContentPolymorphicSerializer<ConditionNode>(ConditionNode::class) {
    override fun selectDeserializer(element: JsonElement): KSerializer<out ConditionNode> {
        return when {
            "id" in element.jsonObject -> ItemConditionNode.serializer()
            "any" in element.jsonObject -> AnyConditionNode.serializer()
            "all" in element.jsonObject -> AllConditionNode.serializer()
            else -> throw IllegalArgumentException("Unknown type: ${element.jsonObject}")
        }
    }
}
