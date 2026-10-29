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
package net.ccbluex.liquidbounce.api.thirdparty.ai

import net.ccbluex.liquidbounce.config.types.list.Tagged

/**
 * Wire protocols supported for AI chat completions.
 *
 * [defaultBaseUrl] is used when no base URL is configured, and it is the API root the
 * protocol's endpoint paths are resolved against (it must end with a slash).
 */
enum class AiProtocol(
    override val tag: String,
    val defaultBaseUrl: String,
) : Tagged {

    /**
     * OpenAI Chat Completions protocol.
     *
     * Also served by DeepSeek, Moonshot/Kimi, Qwen, Zhipu, OpenRouter, Groq and Ollama's
     * `/v1` compatibility endpoint - only the base URL, model and key have to be changed.
     */
    OPENAI_COMPATIBLE("OpenAiCompatible", "https://api.openai.com/v1/"),

    /**
     * Anthropic Messages protocol.
     */
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1/"),

}
