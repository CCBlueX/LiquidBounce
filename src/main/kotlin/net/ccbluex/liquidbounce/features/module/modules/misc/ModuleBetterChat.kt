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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.interfaces.ChatHudLineAddition
import net.ccbluex.liquidbounce.interfaces.ChatMessageAddition
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.text.Text
import net.minecraft.text.TextVisitFactory
import net.minecraft.util.Formatting
import org.apache.commons.lang3.StringUtils

/**
 * BetterChat Module
 *
 * Quality of life improvements to the in-game chat.
 */
object ModuleBetterChat : Module("BetterChat", Category.MISC, aliases = arrayOf("AntiSpam")) {

    val infiniteLength = boolean("Infinite", true)
    val antiClear = boolean("AntiClear", true)

    var antiChatClearPaused = false

    private object AntiSpam : ToggleableConfigurable(this, "AntiSpam", true) {

        val regexFilters = mutableListOf<Regex>()

        val stack = boolean("StackMessages", true)
        val filters = textArray("Filters", mutableListOf()).onChanged {
            compileFilters()
        }

        fun compileFilters() {
            regexFilters.clear()
            filters.get().forEach {
                regexFilters.add(Regex(it))
            }
        }

    }

    init {
        tree(AntiSpam)
    }

    @Suppress("unused")
    val chatHandler = handler<ChatReceiveEvent> { event ->
        if (!AntiSpam.enabled) {
            return@handler
        }

        val string = TextVisitFactory.removeFormattingCodes(event.textData)
        var content = StringUtils.substringAfter(string, ">") ?: string
        content = content.trim()

        if (AntiSpam.regexFilters.isNotEmpty()) {
            val shouldBeRemoved = AntiSpam.regexFilters.any {
                it.matches(content)
            }

            if (shouldBeRemoved) {
                event.cancelEvent()
                return@handler
            }
        }

        // stacks messages so that e.g., when a message is sent twice
        // it gets replaces by a new messages that has `[2]` appended
        if (AntiSpam.stack.get()) {
            // always cancel so each message gets an ID
            event.cancelEvent()

            // appends "external" to every message id
            // so servers can't troll users with messages that
            // imitate client messages
            val id = "$string-external"

            val literalText = Text.literal("")
            val text = event.applyChatDecoration.invoke(event.textData)
            literalText.append(text)

            @Suppress("CAST_NEVER_SUCCEEDS") // succeeds with mixins
            val other = mc.inGameHud.chatHud.messages.find {
                (it as ChatMessageAddition).`liquid_bounce$getId`() == id
            }

            var count = 1
            other?.let {
                @Suppress("CAST_NEVER_SUCCEEDS", "KotlinRedundantDiagnosticSuppress") // succeeds with mixins
                count += (other as ChatHudLineAddition).`liquid_bounce$getCount`()
                literalText.append(" ${Formatting.GRAY}[$count]")
            }

            val data = MessageMetadata(prefix = false, id = id, remove = true, count = count)
            chat(texts = arrayOf(literalText), data)
        }
    }

}
