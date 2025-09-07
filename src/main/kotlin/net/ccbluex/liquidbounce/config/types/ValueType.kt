/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.util.AutoCompletionProvider
import net.ccbluex.liquidbounce.config.util.JsonValueFactory
import net.ccbluex.liquidbounce.utils.input.HumanInputDeserializer

enum class ValueType(
    val deserializer: HumanInputDeserializer.StringDeserializer<*>? = null,
    val completer: AutoCompletionProvider.CompletionHandler = AutoCompletionProvider.defaultCompleter,
    val factory: JsonValueFactory? = null,
) {

    // Primitive Types
    BOOLEAN(
        HumanInputDeserializer.booleanDeserializer,
        AutoCompletionProvider.booleanCompleter,
        factory = JsonValueFactory.Boolean,
    ),
    FLOAT(
        HumanInputDeserializer.floatDeserializer,
        factory = JsonValueFactory.Float,
    ),
    FLOAT_RANGE(
        HumanInputDeserializer.floatRangeDeserializer,
        factory = JsonValueFactory.FloatRange,
    ),
    INT(
        HumanInputDeserializer.intDeserializer,
        factory = JsonValueFactory.Int,
    ),
    INT_RANGE(
        HumanInputDeserializer.intRangeDeserializer,
        factory = JsonValueFactory.IntRange,
    ),
    TEXT(
        HumanInputDeserializer.textDeserializer,
        factory = JsonValueFactory.Text,
    ),
    COLOR(
        HumanInputDeserializer.colorDeserializer,
        factory = JsonValueFactory.Color,
    ),
    BLOCK(
        HumanInputDeserializer.blockDeserializer,
    ),
    ITEM(
        HumanInputDeserializer.itemDeserializer,
    ),
    SOUND(
        HumanInputDeserializer.soundDeserializer,
    ),
    STATUS_EFFECT(
        HumanInputDeserializer.statusEffectDeserializer,
    ),
    SCREEN_HANDLER,
    ENTITY_TYPE,
    CLIENT_PACKET,
    SERVER_PACKET,
    KEY(
        HumanInputDeserializer.keyDeserializer,
    ),
    FILE(
        HumanInputDeserializer.fileDeserializer,
    ),
    BIND,
    VECTOR3_I,
    VECTOR3_D,
    VECTOR2_F,

    // Configuration Types
    CHOICE(
        completer = AutoCompletionProvider.choiceCompleter,
    ),
    CHOOSE(
        completer = AutoCompletionProvider.chooseCompleter,
    ),
    MULTI_CHOOSE(
        HumanInputDeserializer.textArrayDeserializer,
    ),
    LIST,
    MUTABLE_LIST,
    ITEM_LIST,
    REGISTRY_LIST,
    CURVE,

    CONFIGURABLE(
        factory = JsonValueFactory.Configurable
    ),
    TOGGLEABLE(
        factory = JsonValueFactory.Toggleable
    ),

    // Client Types
    FRIEND,
    PROXY,
    ACCOUNT,
    SUBSCRIBED_ITEM,

    // Invalid type
    INVALID;
}
