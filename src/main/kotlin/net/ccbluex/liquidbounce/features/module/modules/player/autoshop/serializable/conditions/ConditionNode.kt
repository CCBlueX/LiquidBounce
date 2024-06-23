/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
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
