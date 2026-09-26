/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.arguments

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import net.ccbluex.liquidbounce.lang.translation

/**
 * Typed parse-error factories for the client argument types, mirroring the vanilla
 * pattern of one `DynamicCommandExceptionType` per failure mode (see e.g.
 * `Vec3Argument.ERROR_NOT_COMPLETE`). Unlike vanilla's `Component.literal` messages,
 * the factories produce translatable components so the whole error sentence renders
 * in the user's language.
 */
object CommandErrors {

    /** First `%s` = rejected value, second `%s` = parameter name. */
    @JvmField
    val INVALID_CHOICE = DynamicCommandExceptionType { args ->
        val (value, parameter) = args as List<*>
        translation("liquidbounce.commandManager.invalidChoice", value, parameter)
    }

    /** First `%s` = rejected value, second `%s` = parameter name. */
    @JvmField
    val INVALID_BOOLEAN = DynamicCommandExceptionType { args ->
        val (value, parameter) = args as List<*>
        translation("liquidbounce.commandManager.invalidBoolean", value, parameter)
    }

    /** First `%s` = rejected value, second `%s` = type name. */
    @JvmField
    val INVALID_MULTI_SELECT = DynamicCommandExceptionType { args ->
        val (value, typeName) = args as List<*>
        translation("liquidbounce.commandManager.invalidMultiSelect", value, typeName)
    }

    /** `%s` = parameter name. */
    @JvmField
    val EMPTY_MULTI_SELECT = DynamicCommandExceptionType { parameter ->
        translation("liquidbounce.commandManager.emptyMultiSelect", parameter)
    }

    /** `%s` = rejected module name. */
    @JvmField
    val NO_SUCH_MODULE = DynamicCommandExceptionType { name ->
        translation("liquidbounce.commandManager.noSuchModule", name)
    }

    /** `%s` = rejected player name. */
    @JvmField
    val NO_SUCH_PLAYER = DynamicCommandExceptionType { name ->
        translation("liquidbounce.commandManager.noSuchPlayer", name)
    }

    /** `%s` = rejected friend name. */
    @JvmField
    val NOT_A_FRIEND = DynamicCommandExceptionType { name ->
        translation("liquidbounce.commandManager.notAFriend", name)
    }

    /** First `%s` = rejected value, second `%s` = the `max` keyword. */
    @JvmField
    val INVALID_ENCHANT_LEVEL = DynamicCommandExceptionType { args ->
        val (value, maxKeyword) = args as List<*>
        translation("liquidbounce.commandManager.invalidEnchantLevel", value, maxKeyword)
    }
}
