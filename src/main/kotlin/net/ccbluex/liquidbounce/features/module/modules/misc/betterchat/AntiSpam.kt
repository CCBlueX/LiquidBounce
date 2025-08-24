package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.interfaces.ChatHudLineAddition
import net.ccbluex.liquidbounce.interfaces.ChatMessageAddition
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.text.Text
import net.minecraft.text.TextVisitFactory
import net.minecraft.util.Formatting

object AntiSpam : ToggleableConfigurable(ModuleBetterChat, "AntiSpam", true) {

    private var regexFilters = emptySet<Regex>()

    private val stack by boolean("StackMessages", false)
    private val filters by textList("Filters", mutableListOf()).onChanged {
        compileFilters()
    }

    private fun compileFilters() {
        regexFilters = filters.mapTo(HashSet(filters.size, 1.0F), ::Regex)
    }

    @Suppress("unused", "CAST_NEVER_SUCCEEDS" /* succeed with mixins */)
    val chatHandler = handler<ChatReceiveEvent> { event ->
        val string = TextVisitFactory.removeFormattingCodes(event.textData)

        if (regexFilters.isNotEmpty()) {
            val content = string.subSequence(string.indexOf('>') + 1, string.length).trim()

            val shouldBeRemoved = regexFilters.any {
                it.matches(content)
            }

            if (shouldBeRemoved) {
                event.cancelEvent()
                return@handler
            }
        }

        // stacks messages so that e.g., when a message is sent twice
        // it gets replaces by a new messages that has `[2]` appended
        if (stack && event.type != ChatReceiveEvent.ChatType.DISGUISED_CHAT_MESSAGE) {
            // always cancel so each message gets an ID
            event.cancelEvent()

            // appends "external" to every message id
            // so servers can't troll users with messages that
            // imitate client messages
            val id = "$string-external"

            val literalText = Text.literal("")
            val text = event.applyChatDecoration.invoke(event.textData)
            literalText.append(text)

            val other = mc.inGameHud.chatHud.messages.find {
                (it as ChatMessageAddition).`liquid_bounce$getId`() == id
            }

            var count = 1
            other?.let {
                count += (other as ChatHudLineAddition).`liquid_bounce$getCount`()
                literalText.append(" ${Formatting.GRAY}[$count]")
            }

            val data = MessageMetadata(prefix = false, id = id, remove = true, count = count)
            chat(texts = arrayOf(literalText), data)
        }
    }

}
