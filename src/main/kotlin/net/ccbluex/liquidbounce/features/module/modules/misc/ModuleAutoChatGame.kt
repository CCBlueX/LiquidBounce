/*
 *
 *  * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *  *
 *  * Copyright (c) 2015 - 2023 CCBlueX
 *  *
 *  * LiquidBounce is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * LiquidBounce is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.openai.Gpt
import kotlin.concurrent.thread

/**
 * Automatically solves chat game riddles.
 */
object ModuleAutoChatGame : Module("AutoChatGame", Category.MISC) {

    private val openAiKey by text("OpenAiKey", "")
    private val delayResponse by intRange("ReactionTime", 1000..5000, 0..10000)
    private val cooldownMinutes by int("Cooldown", 2, 0..60)
    private val bufferTime by int("BufferTime", 200, 0..500)
    private val triggerSentence by text("TriggerSentence", "Chat Game")
    private val serverName by text("ServerName", "Minecraft")

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
        You participate in a chat game in which you have to answer questions.
        The goal is to answer them as short and precise as possible.
        It mostly only requires one word as an answer or if it's a math question it requires the result.
        The questions might be based on the game Minecraft and the server you are playing on.
        Always answer with the first thing that comes to your mind.
        The server name is {SERVER_NAME}.
        On true or false questions, respond without any dots, in lower-case with 'true' or 'false'.
        If you do not know it, just guess with something that fits.
        DO NOT PUT ANYTHING IN THE FRONT OF YOUR ANSWER!
        DO NOT PUT ANY DOTS AT THE END OF YOUR ANSWER!
        DO NOT SAY ANYTHING ELSE THAN THE ANSWER! If you do, you will be disqualified.
        """.trimIndent().replace("\n", " ")
    private val prompt by text("Prompt", defaultPrompt)

    override fun enable() {
        if (openAiKey.isBlank()) {
            chat("§cPlease enter your OpenAI key in the module settings.")
            enabled = false
            return
        }
    }

    private val chatBuffer = mutableListOf<String>()
    private val triggerWordChronometer = Chronometer()
    private val cooldownChronometer = Chronometer()

    val chatHandler = sequenceHandler<ChatReceiveEvent> { event ->
        val message = event.message

        // Only handle game messages. It is unlikely that any server will use a player for the chat game.
        if (event.type != ChatReceiveEvent.ChatType.GAME_MESSAGE) {
            return@sequenceHandler
        }

        // Auto GG
        if (message.contains("Show some love by typing")) {
            delay(delayResponse.random().toLong())

            network.sendChatMessage("gg")
            return@sequenceHandler
        }

        // Trigger word checking. Cooldown prevents the bot from answering the question twice
        // if the result has the same tag.
        if (cooldownChronometer.hasElapsed(cooldownMinutes * 60000L)) {
            // Does the message contain the magic trigger word?
            if (message.contains(triggerSentence)) {
                triggerWordChronometer.reset()

                // TODO: Add option for servers that have the trigger word and question in the same message.
                // Do not include the trigger word in the buffer.
                chatBuffer.clear()
                return@sequenceHandler
            }

            // If the trigger word has been said, add the message to the buffer.
            if (!triggerWordChronometer.hasElapsed(bufferTime.toLong())) {
                chatBuffer.add(message)
            }
        } else {
            chatBuffer.clear()
        }
    }

    val repeatable = repeatable {
        // Has the trigger word been said and has the buffer time elapsed?
        if (triggerWordChronometer.hasElapsed(bufferTime.toLong())) {
            // Is the buffer empty? - If it is we already answered the question.
            if (chatBuffer.isEmpty()) {
                return@repeatable
            }

            // Handle questions
            var question = chatBuffer.joinToString(" ")
            chatBuffer.clear()
            cooldownChronometer.reset()

            // Remove double spaces
            while (question.contains("  ")) {
                question = question.replace("  ", " ")
            }

            chat("§aUnderstood question: $question")

            // Create new AI instance with OpenAI key
            val ai = Gpt(openAiKey, prompt.replace("{SERVER_NAME}", serverName))

            thread {
                runCatching {
                    val startAsk = System.currentTimeMillis()
                    var answer = ai.requestNewAnswer(question)

                    // Remove dot on the end of answer
                    if (answer.last() == '.') {
                        answer = answer.substring(0, answer.length - 1)
                    }

                    chat("§aAnswer: $answer, took ${System.currentTimeMillis() - startAsk}ms.")

                    val delay = delayResponse.random()
                    chat("§aAnswering question: $answer, waiting for ${delay}ms.")

                    Thread.sleep(delay.toLong())

                    // Send answer
                    network.sendChatMessage(answer)
                }.onFailure {
                    chat("§cFailed to answer question: ${it.message}")
                }
            }
        }
    }

}
