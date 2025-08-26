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

import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.api.thirdparty.OPENAI_BASE_URL
import net.ccbluex.liquidbounce.api.thirdparty.OpenAiApi
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * Automatically solves chat game riddles.
 */
object ModuleAutoChatGame : ClientModule("AutoChatGame", Category.MISC) {

    init {
        doNotIncludeAlways()
    }

    private val baseUrl by text("BaseUrl", OPENAI_BASE_URL)
    private val openAiKey by text("OpenAiKey", "")
    private val model by text("Model", "gpt-4o-mini") // GPT 4O Mini should be enough for this
    private val delayResponse by intRange("ReactionTime", 1000..5000, 0..10000,
        "ms")
    private val cooldownMinutes by int("Cooldown", 2, 0..60, "minutes")
    private val bufferTime by int("BufferTime", 200, 0..500, "ms")
    private val triggerSentence by text("TriggerSentence", "Chat Game")
    private val includeTrigger by boolean("IncludeTrigger", true)
    private val serverName by text("ServerName", "Minecraft")
    private val answerTemplate by text("AnswerTemplate", "%s")

    /**
     * Default prompt for the AI.
     * This is the text that the AI will use to generate answers.
     *
     * It is recommended to not change this, as it is already optimized for the chat game.
     * If you want to change it, make sure to keep the same structure.
     *
     * Do not create any line breaks, as it might break the JSON format.
     */
    private val defaultPrompt = """
        You participate in a chat game in which you have to answer questions or do tasks.
        Your goal is to answer them as short and precise as possible and win the game.
        The questions might be based on the game Minecraft or the minecraft server you are playing on.
        The server name is {SERVER_NAME}.
        On true or false questions, respond without any dots, in lower-case with 'true' or 'false'.
        On math questions, respond with the result.
        On first to type tasks, respond with the word.
        On unscramble tasks, the word is scrambled and might be from the game Minecraft (ex. Spawners, Iron Golem),
        respond with the unscrambled word.
        On other questions, respond with the answer.
        DO NOT SAY ANYTHING ELSE THAN THE ANSWER! If you do, you will be disqualified.
        A few hints: [
        Amethyst geodes spawn at Y level and below in 1.18 -> 30,
        Minecraft's moon has the same amount of lunar phases as the moon in real life -> true
        ]
        """.trimIndent().replace('\n', ' ')
    private val prompt by text("Prompt", defaultPrompt)

    override fun onEnabled() {
        if (openAiKey.isBlank()) {
            chat("§cPlease enter your OpenAI key in the module settings.")
            enabled = false
            return
        }
    }

    private val chatBuffer = mutableListOf<String>()
    private val triggerWordChronometer = Chronometer()
    private val cooldownChronometer = Chronometer()

    @Suppress("unused")
    val chatHandler = sequenceHandler<ChatReceiveEvent> { event ->
        val message = event.message

        // Only handle game messages. It is unlikely that any server will use a player for the chat game.
        if (event.type != ChatReceiveEvent.ChatType.GAME_MESSAGE) {
            return@sequenceHandler
        }

        // Auto GG
        if (message.contains("Show some love by typing")) {
            waitTicks(delayResponse.random() / 50)
            network.sendChatMessage("gg")
            return@sequenceHandler
        }

        // Trigger word checking. Cooldown prevents the bot from answering the question twice
        // if the result has the same tag.
        if (cooldownChronometer.hasElapsed(cooldownMinutes * 60000L)) {
            // Does the message contain the magic trigger word?
            if (message.contains(triggerSentence)) {
                triggerWordChronometer.reset()

                chatBuffer.clear()
                if (!includeTrigger) {
                    // Do not include the trigger word in the buffer.
                    return@sequenceHandler
                }
            }

            // If the trigger word has been said, add the message to the buffer.
            if (!triggerWordChronometer.hasElapsed(bufferTime.toLong())) {
                chatBuffer.add(message)
            }
        } else {
            chatBuffer.clear()
        }
    }

    @Suppress("unused")
    val tickHandler = tickHandler {
        waitUntil {
            // Has the trigger word been said and has the buffer time elapsed?
            triggerWordChronometer.hasElapsed(bufferTime.toLong())
            // Is the buffer empty? - If it is we already answered the question.
                && chatBuffer.isNotEmpty()
        }

        // Handle questions
        val question = chatBuffer
            .joinToString(" ")
            // Remove duplicated spaces
            .replace(Regex("\\s+"), " ")
            // Remove leading and trailing whitespace
            .trim()

        chatBuffer.clear()
        cooldownChronometer.reset()

        chat("§aUnderstood question: $question")

        val startAsk = System.currentTimeMillis()

        val answer = waitFor(Dispatchers.IO) {
            runCatching {
                // Create new AI instance with OpenAI key
                val ai = OpenAiApi(baseUrl, openAiKey, model, prompt.replace("{SERVER_NAME}", serverName))

                ai.requestNewAnswer(question).trimEnd {
                    // Remove dot on the end of answer
                    it == '.'
                }
            }.onFailure {
                logger.error("GPT AutoChatGame failed", it)
                chat("§cFailed to answer question: ${it.message}")
            }.getOrNull()
        } ?: return@tickHandler

        chat("§aAnswer: $answer, took ${System.currentTimeMillis() - startAsk}ms.")

        val delay = delayResponse.random()
        chat("§aAnswering question: $answer, waiting for ${delay}ms.")
        waitTicks(delay / 50)

        // Send answer
        val formattedAnswer = answerTemplate.format(answer)
        if (formattedAnswer.startsWith("/")) {
            network.sendCommand(formattedAnswer.substring(1))
        } else {
            network.sendChatMessage(formattedAnswer)
        }
    }
}
