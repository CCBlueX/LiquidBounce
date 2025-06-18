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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.thirdparty.TranslatorApi
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.TitleEvent
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

object ModuleBetterTitle : ClientModule(
    "BetterTitle", Category.RENDER, aliases = arrayOf("BetterSubtitle")
) {

    private enum class Type(override val choiceName: String) : NamedChoice {
        TITLE("Title"), SUBTITLE("Subtitle")
    }

    private val autoTranslate by multiEnumChoice("AutoTranslate", Type.entries)

    private inline fun <reified E : TitleEvent.TextContent> translatorHandler(type: Type) = suspendHandler<E> {
        if (type !in autoTranslate) {
            return@suspendHandler
        }

        val string = it.text?.string?.takeUnless(String::isBlank) ?: return@suspendHandler

        val result = TranslatorApi.google(text = string)
        if (result.isValid) {
            chat(
                highlight(type.choiceName)
                    .append(" ")
                    .append(result.toResultText())
            )
        }
    }

    @Suppress("unused")
    private val titleHandler = translatorHandler<TitleEvent.Title>(Type.TITLE)

    @Suppress("unused")
    private val subtitleHandler = translatorHandler<TitleEvent.Subtitle>(Type.SUBTITLE)

}
