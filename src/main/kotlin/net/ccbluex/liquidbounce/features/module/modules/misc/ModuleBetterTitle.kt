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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.thirdparty.translator.TranslationResult
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.TitleEvent
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsAutoTranslate
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Gui
import net.minecraft.network.chat.Component

object ModuleBetterTitle : ClientModule(
    "BetterTitle", ModuleCategories.RENDER, aliases = listOf("BetterSubtitle")
) {
    init {
        tree(AutoTranslate)
    }
}

private object AutoTranslate : ToggleableValueGroup(ModuleBetterTitle, "AutoTranslate", false) {
    private val components by multiEnumChoice("Components", TitleType.entries)
    private val showIn by multiEnumChoice("ShowIn", ShowIn.CHAT)

    private inline fun <reified E : TitleEvent.TextContent> translatorHandler(
        type: TitleType
    ) = suspendHandler<E> { event ->
        if (type !in components) {
            return@suspendHandler
        }

        val string = event.text
            ?.string
            ?.stripMinecraftColorCodes()
            ?.takeUnless(String::isBlank)
            ?: return@suspendHandler

        val result = GlobalSettingsAutoTranslate.translate(text = string)
        if (result.isValid && result is TranslationResult.Success) {
            showIn.forEach { it.show(type, event, result) }
        }
    }

    @Suppress("unused")
    private val titleHandler = translatorHandler<TitleEvent.Title>(TitleType.TITLE)

    @Suppress("unused")
    private val subtitleHandler =
        translatorHandler<TitleEvent.Subtitle>(TitleType.SUBTITLE)
}

@Suppress("unused")
private enum class ShowIn(
    override val tag: String,
    val show: (TitleType, TitleEvent.TextContent, TranslationResult.Success) -> Unit
) : Tagged {
    CHAT("Chat", { type, _, result ->
        chat(
            highlight(type.tag),
            regular(": "),
            result.toResultText(),
        )
    }),
    MESSAGE("Message", { type, event, result ->
        result.translation.asPlainText(ChatFormatting.WHITE).let {
            event.text = it
            type.setText(it)
        }
    })
}


private enum class TitleType(
    override val tag: String,
    /**
     * Doesn't use [Gui.setTitle] and [Gui.setSubtitle] because
     * this will cause reset of the stayIn timer
     */
    val setText: (Component) -> Unit
) : Tagged {
    TITLE("Title", {
        mc.gui.title = it
    }),
    SUBTITLE("Subtitle", {
        mc.gui.subtitle = it
    })
}
